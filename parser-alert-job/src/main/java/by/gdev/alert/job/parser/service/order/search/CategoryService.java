package by.gdev.alert.job.parser.service.order.search;

import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.repository.SiteSourceJobRepository;
import by.gdev.alert.job.parser.service.order.search.converter.Converter;
import by.gdev.common.model.CategoryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final SiteSourceJobRepository siteSourceJobRepository;
    private final Converter<Category, CategoryDTO> categoryTreeConverter;

    public List<CategoryDTO> getCategoryTree(String site) {
        SiteSourceJob job = siteSourceJobRepository.findByName(site);

        return job.getCategories().stream()
                .map(categoryTreeConverter::convert)
                .toList();
    }
}


