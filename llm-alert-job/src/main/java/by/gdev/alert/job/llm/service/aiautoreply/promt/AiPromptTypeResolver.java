package by.gdev.alert.job.llm.service.aiautoreply.promt;

import by.gdev.alert.job.llm.domain.promt.AiPromptType;
import org.springframework.stereotype.Service;

@Service
public class AiPromptTypeResolver {

    public static AiPromptType resolve(String category, String subcategory) {

        String cat = category == null ? "" : category.toLowerCase();
        String sub = subcategory == null ? "" : subcategory.toLowerCase();

        // DESIGN
        if (cat.contains("design") ||
                sub.contains("design") ||
                sub.contains("brand") ||
                sub.contains("identity") ||
                sub.contains("logo") ||
                sub.contains("corporate") ||
                sub.contains("letterhead") ||
                sub.contains("business card")) {
            return AiPromptType.DESIGN;
        }

        // CHATBOTS
        if (sub.contains("chatbot") || sub.contains("chatbots")) {
            return AiPromptType.CHATBOTS;
        }

        // DEVELOPMENT
        if (cat.contains("development") || cat.contains("it")) {
            return AiPromptType.DEVELOPMENT;
        }

        // DEFAULT
        return AiPromptType.DEFAULT;
    }
}

