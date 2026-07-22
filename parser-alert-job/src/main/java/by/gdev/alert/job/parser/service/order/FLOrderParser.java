package by.gdev.alert.job.parser.service.order;

import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.Order;
import by.gdev.alert.job.parser.domain.db.ParserSource;
import by.gdev.alert.job.parser.domain.db.Price;
import by.gdev.alert.job.parser.domain.db.Subcategory;
import by.gdev.alert.job.parser.domain.rss.Item;
import by.gdev.alert.job.parser.domain.rss.Rss;
import by.gdev.common.model.OrderDTO;
import by.gdev.common.model.SiteName;
import by.gdev.alert.job.parser.service.order.jsoup.JsoupClient;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class FLOrderParser extends AbsctractSiteParser {

    private final JsoupClient jsoupClient;

    private Pattern paymentPatter = Pattern.compile(".*[Бб]юджет: (\\d+).*");
    private Pattern currencyPatter = Pattern.compile("\\d.*&#8381;");

    private final ModelMapper mapper;
    @Value("${parser.work.fl.ru}")
    private void setActive(boolean active) {
        this.active = active;
    }

    @Override
    @SneakyThrows
    protected List<OrderDTO> mapItems(String rssURI, Long siteSourceJobId, Category category, Subcategory subCategory) {
		if (!active)
			return new ArrayList<>();

        if (rssURI == null || rssURI.isBlank()) {
            log.warn("{}: пустой RSS URI для категории {}", getSiteName(), category.getNativeLocName());
            return List.of();
        }

        JAXBContext jaxbContext = JAXBContext.newInstance(Rss.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        String xml = jsoupClient.getRaw(rssURI);
        if (xml == null) {
            log.warn("{} вернул NULL вместо RSS", getSiteName());
            return List.of();
        }
        String trimmed = xml.trim();
        StringReader reader = new StringReader(trimmed);
        Rss rss = (Rss) jaxbUnmarshaller.unmarshal(reader);

        if (rss.getChannel() == null || rss.getChannel().getItem() == null)
            return List.of();

        List<Order> rawOrders = rss.getChannel().getItem().stream()
                .map(item -> buildOrder(item, siteSourceJobId, category, subCategory))
                .filter(Objects::nonNull)
                .toList();

        return getOrdersData(rawOrders, category, subCategory);
    }

    private Order buildOrder(Item item, Long siteSourceJobId, Category category, Subcategory subCategory) {
        if (!getParserService().isExistsOrder(item.getLink()))
            return null;

        Order order = getOrderRepository().findByLink(item.getLink()).orElse(new Order());

        order.setTitle(item.getTitle());
        order.setDateTime(item.getPubDate());
        order.setMessage(item.getDescription());
        order.setLink(item.getLink());

        order = parsePrice(order);

        String cleaned = order.getTitle().replaceAll("(\\(Бюджет: .*[0-9\\;\\)])", "");
        order.setTitle(cleaned);

        ParserSource ps = new ParserSource();
        ps.setSource(siteSourceJobId);
        ps.setCategory(category.getId());
        ps.setSubCategory(subCategory != null ? subCategory.getId() : null);
        order.setSourceSite(ps);
        return order;
    }

    @SneakyThrows
    private Order parsePrice(Order order) {
        Matcher m = paymentPatter.matcher(order.getTitle());
        Price price = new Price();
        if (m.find()) {
            price.setValue(Integer.valueOf(m.group(1)));
            order.setPrice(price);
        }
        Matcher m1 = currencyPatter.matcher(order.getTitle());
        if (m1.find()) {
            price.setPrice(m1.group(0).replaceAll("&#8381;", "руб."));
            order.setPrice(price);
        }

        Document doc;
        try {
            doc = jsoupClient.get(order.getLink());
            if (doc == null) {
                log.warn("{} вернул null (502/503/504) для {}", getSiteName(),  order.getLink());
                return order;
            }
        } catch (Exception ex) {
            order.setValidOrder(false);
            log.debug("invalid flru link {}", order.getLink());
            return order;
        }

        Element el = doc.selectFirst(".b-layout__txt_lineheight_1");
        if (Objects.nonNull(el) && (el.text().contains("Срочный заказ") || el.text().contains("Для всех"))) {
            order.setOpenForAll(true);
        }
        return order;
    }


    public SiteName getSiteName() {
        return SiteName.FLRU;
    }
}