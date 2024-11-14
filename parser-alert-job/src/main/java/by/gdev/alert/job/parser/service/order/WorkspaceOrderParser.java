package by.gdev.alert.job.parser.service.order;

import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.Order;
import by.gdev.alert.job.parser.domain.db.ParserSource;
import by.gdev.alert.job.parser.domain.db.Price;
import by.gdev.alert.job.parser.domain.db.Subcategory;
import by.gdev.alert.job.parser.repository.OrderRepository;
import by.gdev.alert.job.parser.repository.ParserSourceRepository;
import by.gdev.alert.job.parser.service.ParserService;
import by.gdev.alert.job.parser.util.SiteName;
import by.gdev.common.model.OrderDTO;
import by.gdev.common.model.SourceSiteDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
@Slf4j
@RequiredArgsConstructor
public class WorkspaceOrderParser extends AbsctractSiteParser {

    private final String baseURI = "https://workspace.ru";
    private final String statusParam = "?STATUS=published";
    private final ParserService parserService;
    private final ParserSourceRepository parserSourceRepository;
    private final OrderRepository orderRepository;
    private final ModelMapper mapper;

    @Override
    protected List<OrderDTO> mapItems(String link, Long siteSourceJobId, Category category, Subcategory subCategory) {

        Document document = null;
        try {
            document = Jsoup.connect(link+statusParam).get();
        } catch (IOException e) {
            log.error("cannot parse orders by link {}", link);
            throw new RuntimeException(e);
        }

        Elements cards = document.getElementsByClass("vacancies__card _tender");
        return cards.stream()
                .map(card -> {
                    Element element = card.children().get(1);
                    String title = element.child(0).child(0).text();
                    String postfixLink = element.child(0).child(0).attr("href");
                    String price = element.child(1).text();

                    String date = card.children().get(2).child(0).child(1).text();

                    Order order = new Order();
                    order.setTitle(title);
                    order.setLink(baseURI + postfixLink);

                    SimpleDateFormat formatter = new SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("ru"));

                    try {
                        order.setDateTime(formatter.parse(date));
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }

                    String regex = "(\\d+(?:\\s\\d+)*)";
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(price);
                    if (matcher.find()) {
                        order.setPrice(new Price(price, Integer.parseInt(matcher.group(1).replaceAll("\\s", ""))));
                    }


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
                    log.debug("found new order {} {}", order.getTitle(), order.getLink());
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
        return SiteName.WORKSPACE;
    }
}