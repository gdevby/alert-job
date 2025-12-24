package by.gdev.alert.job.parser.service.order;

import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.CurrencyEntity;
import by.gdev.alert.job.parser.domain.db.Order;
import by.gdev.alert.job.parser.domain.db.ParserSource;
import by.gdev.alert.job.parser.domain.db.Price;
import by.gdev.alert.job.parser.domain.db.Subcategory;
import by.gdev.alert.job.parser.domain.freelancer.BudgetFreelancerOrder;
import by.gdev.alert.job.parser.domain.freelancer.FreelancerRoot;
import by.gdev.alert.job.parser.repository.CurrencyRepository;
import by.gdev.alert.job.parser.repository.OrderRepository;
import by.gdev.alert.job.parser.repository.ParserSourceRepository;
import by.gdev.alert.job.parser.service.ParserService;
import by.gdev.alert.job.parser.util.SiteName;
import by.gdev.common.model.OrderDTO;
import by.gdev.common.model.SourceSiteDTO;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


@Service
@RequiredArgsConstructor
@Slf4j
public class FreelancerOrderParser extends AbsctractSiteParser {

    @Value("${freelancer.proxy.active}")
    private boolean isNeedProxy;
    private String sourceLink = "https://www.freelancer.com/projects/%s";

    private final ParserService service;
    private final OrderRepository orderRepository;
    private final ParserSourceRepository parserSourceRepository;
    private final CurrencyRepository currencyRepository;
    private final ModelMapper mapper;

    private RestTemplate restTemplate;
    @Value("${parser.work.freelancer.com}")
	private boolean active;


    @Override
    @SneakyThrows
    public List<OrderDTO> mapItems(String link, Long siteSourceJobId, Category category, Subcategory subCategory) {
		if (!active)
			return new ArrayList<>();
        if (Objects.isNull(link))
            return Lists.newArrayList();
        restTemplate = getRestTemplate(isNeedProxy);
        FreelancerRoot response = restTemplate.getForObject(link, FreelancerRoot.class);
        return response.getResult().getProjects().stream()
                .map(e -> {
                    Order order = new Order();
                    order.setTitle(e.getTitle());
                    String orderLink = String.format(sourceLink, e.getSeoUrl());
                    order.setLink(orderLink);
                    order.setMessage(e.getDescription());
                    if (e.getType().equals("fixed")) {
                        String currencyCode = e.getCurrency().getCode();
                        Optional<CurrencyEntity> currency = currencyRepository.findByCurrencyCode(currencyCode);
                        currency.ifPresent(c -> {
                            BudgetFreelancerOrder b = e.getBudget();
                            long budget = Objects.nonNull(b.getMaximum()) ? b.getMaximum() : b.getMinimum();
                            Double priceValue = (budget / c.getNominal()) * c.getCurrencyValue();
                            String priceSource = String.format("%s %s", budget, currencyCode);
                            Price p = new Price(priceSource, priceValue.intValue());
                            order.setPrice(p);
                        });
                    }
                    order.setDateTime(new Date());
                    ParserSource parserSource = new ParserSource();
                    parserSource.setSource(siteSourceJobId);
                    parserSource.setCategory(category.getId());
                    parserSource.setSubCategory(subCategory.getId());
                    order.setSourceSite(parserSource);
                    return order;
                })
                .filter(Order::isValidOrder)
                .filter(f -> service.isExistsOrder(category, subCategory, f.getLink()))
                .map(e -> {
                    log.debug("found new order {} {}", e.getTitle(), e.getLink());
                    service.saveOrderLinks(category, subCategory, e.getLink());
                    ParserSource parserSource = e.getSourceSite();
                    Optional<ParserSource> optionalSource = parserSourceRepository.findBySourceAndCategoryAndSubCategory(
                            parserSource.getSource(), parserSource.getCategory(), parserSource.getSubCategory());
                    if (optionalSource.isPresent()) {
                        parserSource = optionalSource.get();
                    } else {
                        parserSource = parserSourceRepository.save(parserSource);
                    }
                    e.setSourceSite(parserSource);
                    e = orderRepository.save(e);
                    OrderDTO dto = mapper.map(e, OrderDTO.class);
                    SourceSiteDTO source = dto.getSourceSite();
                    source.setCategoryName(category.getNativeLocName());
                    if (Objects.nonNull(subCategory))
                        source.setSubCategoryName(subCategory.getNativeLocName());
                    dto.setSourceSite(source);
                    return dto;
                })
                .toList();
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.FREELANCER;
    }
}