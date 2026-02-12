package by.gdev.alert.job.parser.service.order.search.converter.impl;

import by.gdev.alert.job.parser.domain.db.ParserSource;
import by.gdev.alert.job.parser.service.order.search.converter.Converter;
import by.gdev.common.model.SourceSiteDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ParserSourceToSourceSiteDTOConverter implements Converter<ParserSource, SourceSiteDTO> {

    @Override
    public SourceSiteDTO convert(ParserSource source) {
        SourceSiteDTO target =  new SourceSiteDTO();
        target.setSource(source.getSource());
        target.setId(source.getId());
        target.setCategory(source.getCategory());
        return target;
    }
}
