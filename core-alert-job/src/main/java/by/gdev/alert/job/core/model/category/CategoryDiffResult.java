package by.gdev.alert.job.core.model.category;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Data
@Slf4j
public class CategoryDiffResult {

    private List<String> newCategories;
    private List<String> removedCategories;

    private Map<String, List<String>> newSubcategories;
    private Map<String, List<String>> removedSubcategories;
}

