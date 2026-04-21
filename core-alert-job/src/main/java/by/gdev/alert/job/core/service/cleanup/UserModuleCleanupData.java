package by.gdev.alert.job.core.service.cleanup;

import by.gdev.alert.job.core.model.cleanup.ParserCategoryDTO;
import lombok.Getter;

import java.util.List;

@Getter
public class UserModuleCleanupData {
    private String moduleName;
    private List<String> positiveWords;
    private List<String> negativeWords;
    private List<ParserCategoryDTO> categories;

    public UserModuleCleanupData(
            String moduleName,
            List<String> positiveWords,
            List<String> negativeWords,
            List<ParserCategoryDTO> categories
    ) {
        this.moduleName = moduleName;
        this.positiveWords = positiveWords;
        this.negativeWords = negativeWords;
        this.categories = categories;
    }
}

