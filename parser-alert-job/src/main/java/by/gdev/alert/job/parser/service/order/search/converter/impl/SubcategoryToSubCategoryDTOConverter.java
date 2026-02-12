package by.gdev.alert.job.parser.service.order.search.converter.impl;

import by.gdev.alert.job.parser.domain.db.Subcategory;
import by.gdev.alert.job.parser.service.order.search.converter.Converter;
import by.gdev.common.model.SubCategoryDTO;
import org.springframework.stereotype.Component;

@Component
public class SubcategoryToSubCategoryDTOConverter implements Converter<Subcategory, SubCategoryDTO> {

    @Override
    public SubCategoryDTO convert(Subcategory source) {
        SubCategoryDTO dto = new SubCategoryDTO();
        dto.setId(source.getId());
        dto.setName(source.getName());
        dto.setNativeLocName(source.getNativeLocName());
        dto.setLink(source.getLink());
        dto.setParse(source.isParse());
        return dto;
    }
}

