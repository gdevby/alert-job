package by.gdev.alert.job.parser.service.order.search.converter.impl;

import by.gdev.alert.job.parser.domain.db.Order;
import by.gdev.alert.job.parser.domain.db.ParserSource;
import by.gdev.alert.job.parser.domain.db.Price;
import by.gdev.alert.job.parser.service.order.search.converter.Converter;
import by.gdev.common.model.OrderDTO;
import by.gdev.common.model.PriceDTO;
import by.gdev.common.model.SourceSiteDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderToOrderDTOConverter implements Converter<Order, OrderDTO> {

    private final Converter<ParserSource, SourceSiteDTO> sourceConverter;
    private final Converter<Price, PriceDTO> priceConverter;

    @Override
    public OrderDTO convert(Order source) {
        OrderDTO target = new OrderDTO();
        target.setLink(source.getLink());
        target.setMessage(source.getMessage());
        target.setTitle(source.getTitle());
        target.setSourceSite(sourceConverter.convert(source.getSourceSite()));
        target.setPrice(priceConverter.convert(source.getPrice()));
        target.setOpenForAll(source.isOpenForAll());
        target.setValidOrder(source.isValidOrder());
        return target;
    }
}
