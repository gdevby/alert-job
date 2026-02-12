package by.gdev.alert.job.parser.service.order.search.converter.impl;

import by.gdev.alert.job.parser.domain.db.Price;
import by.gdev.alert.job.parser.service.order.search.converter.Converter;
import by.gdev.common.model.PriceDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PriceToPriceDtoConverter implements Converter<Price, PriceDTO> {

    @Override
    public PriceDTO convert(Price source) {
        if (source == null){
            return null;
        }
        PriceDTO target = new PriceDTO();
        target.setPrice(source.getPrice());
        target.setValue(source.getValue());
        return target;
    }
}
