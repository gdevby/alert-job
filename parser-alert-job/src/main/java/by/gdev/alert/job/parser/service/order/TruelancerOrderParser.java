package by.gdev.alert.job.parser.service.order;

import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.CurrencyEntity;
import by.gdev.alert.job.parser.domain.db.Order;
import by.gdev.alert.job.parser.domain.db.ParserSource;
import by.gdev.alert.job.parser.domain.db.Price;
import by.gdev.alert.job.parser.domain.db.Subcategory;
import by.gdev.alert.job.parser.domain.truelancer.TreuelancerProjects;
import by.gdev.alert.job.parser.domain.truelancer.TrueLancerRoot;
import by.gdev.alert.job.parser.domain.truelancer.TruelancerOrder;
import by.gdev.alert.job.parser.repository.CurrencyRepository;
import by.gdev.alert.job.parser.repository.OrderRepository;
import by.gdev.alert.job.parser.repository.ParserSourceRepository;
import by.gdev.alert.job.parser.service.ParserService;
import by.gdev.alert.job.parser.util.SiteName;
import by.gdev.common.model.OrderDTO;
import by.gdev.common.model.SourceSiteDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TruelancerOrderParser extends AbsctractSiteParser {

    @Value("${freelancer.proxy.active}")
    private boolean isNeedProxy;
    private String uri = "https://api.truelancer.com/api/v1/projects";
    private final CurrencyRepository currencyRepository;
    private final ParserService parserService;
    private final ParserSourceRepository parserSourceRepository;
    private final OrderRepository orderRepository;
    private final ModelMapper mapper;
    private RestTemplate restTemplate;
    @Value("${parser.work.truelancer.com}")
    private void setActive(boolean active) {
        this.active = active;
    }

    @Override
    protected List<OrderDTO> mapItems(String link, Long siteSourceJobId, Category category, Subcategory subCategory) {
		if (!active)
			return new ArrayList<>();
        restTemplate = getRestTemplate(isNeedProxy);

        TrueLancerRoot root = restTemplate.postForObject(uri, Map.of("category", link), TrueLancerRoot.class);
        TreuelancerProjects projects = root.getProjects();
        List<TruelancerOrder> orders = projects.getOrders();

        return orders.stream()
                .map(truelancerOrder ->{
                    Order order = new Order();
                    order.setTitle(truelancerOrder.getTitle());
                    order.setLink(truelancerOrder.getLink());
                    order.setMessage(truelancerOrder.getDescription());
                    order.setDateTime(new Date());

                    String currencyCode = truelancerOrder.getCurrency();
                    Optional<CurrencyEntity> currency = currencyRepository.findByCurrencyCode(currencyCode);

                    currency.ifPresent(c ->{
                        double convertedPrice = (truelancerOrder.getBudget() / c.getNominal()) * c.getCurrencyValue();
                        order.setPrice(new Price(truelancerOrder.getBudget() + " " + currencyCode, (int) convertedPrice));
                    });


                    ParserSource parserSource = new ParserSource();
                    parserSource.setSource(siteSourceJobId);
                    parserSource.setCategory(category.getId());
                    parserSource.setSubCategory(Objects.nonNull(subCategory) ? subCategory.getId() : null);

                    order.setSourceSite(parserSource);
                    return order;
                })
                .filter(Order::isValidOrder)
                .filter(order -> parserService.isExistsOrder(category, subCategory, order.getLink()))
                .map(order -> {
                    //log.debug("found new order {} {}", order.getTitle(), order.getLink());
                    parserService.saveOrderLinks(category, subCategory, order.getLink());
                    ParserSource parserSource = order.getSourceSite();
                    Optional<ParserSource> source = parserSourceRepository.findBySourceAndCategoryAndSubCategory(
                            parserSource.getSource(),
                            parserSource.getCategory(),
                            parserSource.getSubCategory()
                    );

                    if (source.isPresent()) {
                        parserSource = source.get();
                    } else {
                        parserSource = parserSourceRepository.save(parserSource);
                    }

                    order.setSourceSite(parserSource);
                    order = orderRepository.save(order);
                    OrderDTO orderDto = mapper.map(order, OrderDTO.class);
                    SourceSiteDTO sourceSiteDto = orderDto.getSourceSite();
                    sourceSiteDto.setCategoryName(category.getNativeLocName());
                    if (Objects.nonNull(subCategory))
                        sourceSiteDto.setSubCategoryName(subCategory.getNativeLocName());
                    orderDto.setSourceSite(sourceSiteDto);
                    return orderDto;
                })
                .toList();
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.TRUELANCER;
    }
}
