package by.gdev.alert.job.core.service.cleanup;

import java.util.List;

public class UserModuleCleanupData {
    private String moduleName;
    private List<String> positiveWords;
    private List<String> negativeWords;

    public UserModuleCleanupData(String moduleName,
                                 List<String> positiveWords,
                                 List<String> negativeWords) {
        this.moduleName = moduleName;
        this.positiveWords = positiveWords;
        this.negativeWords = negativeWords;
    }

    public String getModuleName() { return moduleName; }
    public List<String> getPositiveWords() { return positiveWords; }
    public List<String> getNegativeWords() { return negativeWords; }
}

