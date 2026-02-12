package by.gdev.alert.job.parser.service.order.search.converter.impl;

import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.Subcategory;
import by.gdev.alert.job.parser.service.order.search.converter.Converter;
import by.gdev.common.model.CategoryDTO;
import by.gdev.common.model.SubCategoryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CategoryTreeConverter implements Converter<Category, CategoryDTO> {
    private final Converter<Subcategory, SubCategoryDTO> subcategoryConverter;

    @Override
    public CategoryDTO convert(Category source) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(source.getId());
        dto.setName(source.getName());
        dto.setNativeLocName(source.getNativeLocName());
        dto.setLink(source.getLink());
        dto.setParse(source.isParse());

        dto.setSubCategories(
                subcategoryConverter.convertAll(source.getSubCategories()));
        return dto;
    }
}
