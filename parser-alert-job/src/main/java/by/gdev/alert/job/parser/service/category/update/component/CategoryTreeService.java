package by.gdev.alert.job.parser.service.category.update.component;

import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.domain.db.Subcategory;
import by.gdev.alert.job.parser.repository.CategoryRepository;
import by.gdev.alert.job.parser.service.category.update.dto.tree.CategoryDTO;
import by.gdev.alert.job.parser.service.category.update.dto.tree.SiteDTO;
import by.gdev.alert.job.parser.service.category.update.dto.tree.SubcategoryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryTreeService {

    private final CategoryRepository categoryRepository;

    public SiteDTO buildTree(SiteSourceJob job) {

        // Загружаем категории + подкатегории одним запросом
        List<Category> categories =
                categoryRepository.findAllWithSubcategoriesBySourceId(job.getId());

        // DTO сайта
        SiteDTO site = new SiteDTO();
        site.setId(job.getId());
        site.setName(job.getName());

        List<CategoryDTO> categoryDTOs = new ArrayList<>();

        for (Category c : categories) {

            CategoryDTO catDto = new CategoryDTO();
            catDto.setId(c.getId());
            catDto.setName(c.getNativeLocName());

            List<SubcategoryDTO> subDTOs = new ArrayList<>();

            if (c.getSubCategories() != null) {
                for (Subcategory s : c.getSubCategories()) {
                    SubcategoryDTO sd = new SubcategoryDTO();
                    sd.setId(s.getId());
                    sd.setName(s.getNativeLocName());
                    subDTOs.add(sd);
                }
            }

            catDto.setSubcategories(subDTOs);
            categoryDTOs.add(catDto);
        }

        site.setCategories(categoryDTOs);
        return site;
    }
}

