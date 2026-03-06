package by.gdev.alert.job.llm.service.aiautoreply;

import by.gdev.alert.job.llm.domain.dto.AiDecision;
import by.gdev.alert.job.llm.service.aiautoreply.sender.limiter.TimeRateLimiter;
import by.gdev.alert.job.llm.service.aiautoreply.sender.limiter.TokenBucket;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Service
@Slf4j
public class AiOrderAnalysisService {

    private final ChatClient chatClient;
    private final ObjectMapper mapper;
    private final TokenBucket tokenBucket;
    private final TimeRateLimiter timeRateLimiter;
    private final ExecutorService llmExecutor;

    @Autowired
    public AiOrderAnalysisService(ChatClient.Builder builder, ObjectMapper mapper,
                                  TokenBucket tokenBucket, TimeRateLimiter timeRateLimiter,
                                  ExecutorService llmExecutor) {
        this.chatClient = builder.build();
        this.mapper = mapper;
        this.tokenBucket = tokenBucket;
        this.timeRateLimiter = timeRateLimiter;
        this.llmExecutor = llmExecutor;
    }

    private int estimateTokens(String prompt) { // грубая оценка: 1 токен это 4 символа
        int length = prompt.length();
        return Math.max(1, length / 4);
    }

    private String loadPrompt(String path) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalStateException("Prompt file not found: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load prompt: " + path, e);
        }
    }

    public String loadTemplate(String type, String site) {
        String base = "prompts/templates/";

        // 1. кастомный шаблон, если сайт указан
        if (site != null && !site.isBlank()) {
            String custom = base + site.toLowerCase() + "_" + type + ".txt";
            String customContent = loadFromClasspath(custom);
            if (customContent != null) return customContent;
        }

        // 2. общий шаблон по типу
        String general = base + type + ".txt";
        String generalContent = loadFromClasspath(general);
        if (generalContent != null) return generalContent;

        // 3. fallback отсутствует — шаблон обязателен
        throw new RuntimeException("Template not found for type=" + type + ", site=" + site);
    }

    private String loadFromClasspath(String path) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) return null;
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    private String detectType(String category, String subcategory) {
        String c = category == null ? "" : category.toLowerCase();
        String s = subcategory == null ? "" : subcategory.toLowerCase();

        if (c.contains("mobile") || s.contains("mobile")) return "mobile";
        if (c.contains("front") || s.contains("front")) return "front";
        if (c.contains("back") || s.contains("backend") || s.contains("server")) return "back";
        if (c.contains("dev") || c.contains("development")) return "dev";

        return "default";
    }



    private String selectPromptFile(String categoryName, String subcategoryName) {

        String cat = categoryName == null ? "" : categoryName.toLowerCase();
        String sub = subcategoryName == null ? "" : subcategoryName.toLowerCase();

        // DESIGN
        if (cat.contains("design") ||
                sub.contains("design") ||
                sub.contains("brand") ||
                sub.contains("identity") ||
                sub.contains("logo") ||
                sub.contains("corporate") ||
                sub.contains("letterhead") ||
                sub.contains("business card")) {
            return "prompts/analysis_design.txt";
        }

        // CHATBOTS
        if (sub.contains("chatbot") || sub.contains("chatbots")) {
            return "prompts/analysis_chatbots.txt";
        }

        // DEVELOPMENT & IT
        if (cat.contains("development") || cat.contains("it")) {
            return "prompts/analysis_dev.txt";
        }

        // DEFAULT
        return "prompts/analysis_default.txt";
    }


    public AiDecision analyze(String orderTitle,
                              String orderContent,
                              String siteName,
                              String categoryName,
                              String subcategoryName) {

        List<String> keywords = List.of();

        String promptFile = selectPromptFile(categoryName, subcategoryName);
        log.info("AI ANALYSIS PROMPT FILE: {}", promptFile);

        String promptTemplate = loadPrompt(promptFile);

        // 3. Определяем тип проекта (front/back/dev/mobile)
        String type = detectType(categoryName, subcategoryName);

        // 4. Загружаем шаблон письма
        String replyTemplate;
        replyTemplate = loadTemplate(type, siteName);

        String prompt = promptTemplate.formatted(
                orderTitle,
                orderContent,
                siteName,
                categoryName,
                subcategoryName,
                keywords,
                replyTemplate
        );

        int estimatedTokens = estimateTokens(prompt);
        log.info("AI ANALYSIS PROMPT FILE: {}, estimatedTokens: {}", promptFile, estimatedTokens);

        Future<AiDecision> future = llmExecutor.submit(() -> {
            try {
                // Лимит по времени между запросами
                timeRateLimiter.awaitSlot();

                // Лимит по токенам в минуту
                tokenBucket.consume(estimatedTokens);

                // Вызов LLM
                String raw = chatClient.prompt()
                        .user(prompt)
                        .call()
                        .content();

                raw = raw.replace("```json", "")
                        .replace("```", "")
                        .trim();

                return mapper.readValue(raw, AiDecision.class);

            } catch (org.springframework.ai.retry.NonTransientAiException e) {
                // Это 429, 400, 500 от Groq
                log.error("Groq API error: {}", e.getMessage());

                return new AiDecision(
                        false,
                        0.0,
                        "Groq API error: " + e.getMessage(),
                        null, null, null, null, null
                );

            } catch (Exception e) {
                // Любая другая ошибка внутри executor-потока
                log.error("Unexpected LLM error", e);

                return new AiDecision(
                        false,
                        0.0,
                        "Unexpected LLM error: " + e.getMessage(),
                        null, null, null, null, null
                );
            }
        });

        try {
            return future.get();

        } catch (Exception e) {
            // Ошибка executor'а, InterruptedException, ExecutionException
            log.error("Executor error", e);

            return new AiDecision(
                    false,
                    0.0,
                    "Executor error: " + e.getMessage(),
                    null, null, null, null, null
            );
        }
    }



}

