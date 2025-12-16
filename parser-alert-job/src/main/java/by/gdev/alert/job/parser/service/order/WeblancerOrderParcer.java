package by.gdev.alert.job.parser.service.order;

import by.gdev.alert.job.parser.domain.db.*;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeblancerOrderParcer extends AbsctractSiteParser {

    private final String sourceLink = "https://www.weblancer.net";

    private final ParserService service;
    private final OrderRepository orderRepository;
    private final ParserSourceRepository parserSourceRepository;
    private final CurrencyRepository currencyRepository;
    private final ModelMapper mapper;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy");

    @Override
    @SneakyThrows
    public List<OrderDTO> mapItems(String link, Long siteSourceJobId, Category category, Subcategory subCategory) {
        if (Objects.isNull(link))
            return Lists.newArrayList();

        Document doc = Jsoup.connect(link)
                .userAgent("Mozilla/5.0")
                .referrer("https://google.com")
                .timeout(10000)
                .get();

        // Контейнер заказа — article.bg-white
        Elements orders = doc.select("article.bg-white");

        if (orders.isEmpty()) {
            log.warn("Weblancer: orders not found on {}", link);
            return Lists.newArrayList();
        }

        return orders.stream()
                .map(e -> parseOrder(e, siteSourceJobId, category, subCategory))
                .filter(Objects::nonNull)
                .filter(Order::isValidOrder)
                .filter(f -> service.isExistsOrder(category, subCategory, f.getLink()))
                .map(e -> saveOrder(e, category, subCategory))
                .toList();
    }

    private Order parseOrder(Element e, Long siteSourceJobId, Category category, Subcategory subCategory) {
        Order order = new Order();

        // Заголовок и ссылка
        Element titleEl = e.selectFirst("h2 a[href]");
        if (titleEl == null)
            return null;

        order.setTitle(titleEl.text());
        order.setLink(sourceLink + titleEl.attr("href"));

        // Описание
        Element descEl = e.selectFirst("p.text-gray-600");
        order.setMessage(descEl != null ? descEl.text() : "");

        // Дата (формат: 15.12.2025)
        try {
            Element dateSpan = e.selectFirst("div.flex.flex-wrap.items-center span");
            if (dateSpan != null) {
                Date parsed = DATE_FORMAT.parse(dateSpan.text().trim());
                order.setDateTime(parsed);
            }
        } catch (Exception ex) {
            log.error("Date parse error", ex);
            order.setValidOrder(false);
        }

        // Цена — в HTML её нет
        order.setPrice(new Price("По договоренности", 0));

        // Источник
        ParserSource parserSource = new ParserSource();
        parserSource.setSource(siteSourceJobId);
        parserSource.setCategory(category.getId());
        parserSource.setSubCategory(subCategory.getId());
        order.setSourceSite(parserSource);

        return order;
    }

    private OrderDTO saveOrder(Order e, Category category, Subcategory subCategory) {
        service.saveOrderLinks(category, subCategory, e.getLink());

        ParserSource ps = e.getSourceSite();
        ParserSource existing = parserSourceRepository
                .findBySourceAndCategoryAndSubCategory(
                        ps.getSource(),
                        ps.getCategory(),
                        ps.getSubCategory()
                )
                .orElseGet(() -> parserSourceRepository.save(ps));

        e.setSourceSite(existing);
        e = orderRepository.save(e);

        OrderDTO dto = mapper.map(e, OrderDTO.class);
        SourceSiteDTO source = dto.getSourceSite();
        source.setCategoryName(category.getNativeLocName());
        if (subCategory != null)
            source.setSubCategoryName(subCategory.getNativeLocName());
        dto.setSourceSite(source);

        return dto;
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.WEBLANCER;
    }
}
