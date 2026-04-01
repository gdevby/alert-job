package by.gdev.alert.job.llm.service.aiautoreply;

import by.gdev.alert.job.llm.domain.AiReplyTemplate;
import by.gdev.alert.job.llm.domain.dto.AiDecision;
import by.gdev.alert.job.llm.domain.dto.order.AiAppUserDTO;
import by.gdev.alert.job.llm.domain.dto.order.AiOrderModulesDTO;
import by.gdev.alert.job.llm.domain.dto.order.OrderDTO;
import by.gdev.alert.job.llm.service.aiautoreply.sender.limiter.TimeRateLimiter;
import by.gdev.alert.job.llm.service.aiautoreply.sender.limiter.TokenBucket;
import by.gdev.alert.job.llm.service.template.AiReplyTemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
    private final AiReplyTemplateService templateService;

    @Autowired
    public AiOrderAnalysisService(ChatClient.Builder builder, ObjectMapper mapper,
                                  TokenBucket tokenBucket, TimeRateLimiter timeRateLimiter,
                                  ExecutorService llmExecutor, AiReplyTemplateService templateService) {
        this.chatClient = builder.build();
        this.mapper = mapper;
        this.tokenBucket = tokenBucket;
        this.timeRateLimiter = timeRateLimiter;
        this.llmExecutor = llmExecutor;
        this.templateService = templateService;
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

    public String loadTemplate(String userUuid, Long moduleId) {

        // 1. Проверяем пользовательский шаблон
        if (userUuid != null && moduleId != null) {
            AiReplyTemplate userTemplate =
                    templateService.getTemplateForUserAndModule(userUuid, moduleId);

            if (userTemplate != null) {
                log.info("Using USER template for user={}, module={}", userUuid, moduleId);
                return userTemplate.getHtmlTemplate();
            }
        }

        // 2. Если пользовательского нет — используем системный default
        log.info("Using DEFAULT template");

        String defaultPath = "prompts/templates/default.txt";
        String defaultContent = loadFromClasspath(defaultPath);

        if (defaultContent != null) {
            return defaultContent;
        }

        throw new RuntimeException("Default template not found: " + defaultPath);
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


    public AiDecision analyze(OrderDTO order, AiAppUserDTO user, AiOrderModulesDTO orderModule){

        String orderTitle = order.getTitle();
        String orderContent = order.getMessage();
        String orderLink = order.getLink();
        String priceText = order.getPrice() != null ? order.getPrice().getPrice() : "не указана";
        int priceValue = order.getPrice() != null ? order.getPrice().getValue() : 0;
        String siteName = order.getSourceSite() != null ? order.getSourceSite().getSourceName() : null;
        String categoryName = order.getSourceSite() != null ? order.getSourceSite().getCategoryName() : null;
        String subcategoryName = order.getSourceSite() != null ? order.getSourceSite().getSubCategoryName() : null;
        String orderDate = order.getDateTime() != null ? order.getDateTime().toString() : "не указана";

        List<String> keywords = List.of();

        String promptFile = selectPromptFile(categoryName, subcategoryName);
        log.info("AI ANALYSIS PROMPT FILE: {}", promptFile);

        String promptTemplate = loadPrompt(promptFile);

        // 3. Определяем тип проекта (front/back/dev/mobile)
        String type = detectType(categoryName, subcategoryName);

        // 4. Загружаем шаблон письма
        String replyTemplate;
        if (user == null || orderModule == null){
            log.debug("Template source: DEFAULT (user or module is null)");
            replyTemplate = loadTemplate(type, siteName);
        }
        else {
            log.info("Template source: USER (user={}, module={})", user.getUuid(), orderModule.getName());
            replyTemplate = loadTemplate(user.getUuid(), orderModule.getId());
        }

        // 5. Формируем prompt
        String prompt = promptTemplate.formatted(
                orderTitle,
                orderContent,
                priceText,
                priceValue,
                orderLink,
                orderDate,
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

                //НЕ РАБОТАЕТ - StringTemplate v4- подходит для мелких шаблонов типа
                /*
                Hello <name>
                <if(condition)> ... <endif>
                 <list:{x|...}>*/
                /*2026-03-30T12:31:45.903+03:00 ERROR 25588 --- [llm] [pool-3-thread-1] b.g.a.j.l.s.a.AiOrderAnalysisService     : Unexpected LLM error
                java.lang.IllegalArgumentException: The template string is not valid.
                at org.springframework.ai.chat.prompt.PromptTemplate.<init>(PromptTemplate.java:86)
                at org.springframework.ai.chat.client.advisor.api.AdvisedRequest.toPrompt(AdvisedRequest.java:171)
                at org.springframework.ai.chat.client.DefaultChatClient$DefaultChatClientRequestSpec$1.aroundCall(DefaultChatClient.java:680)
                at org.springframework.ai.chat.client.advisor.DefaultAroundAdvisorChain.lambda$nextAroundCall$1(DefaultAroundAdvisorChain.java:98)
                at io.micrometer.observation.Observation.observe(Observation.java:565)
                at org.springframework.ai.chat.client.advisor.DefaultAroundAdvisorChain.nextAroundCall(DefaultAroundAdvisorChain.java:98)
                at org.springframework.ai.chat.client.DefaultChatClient$DefaultCallResponseSpec.doGetChatResponse(DefaultChatClient.java:493)
                at org.springframework.ai.chat.client.DefaultChatClient$DefaultCallResponseSpec.lambda$doGetObservableChatResponse$1(DefaultChatClient.java:482)
                at io.micrometer.observation.Observation.observe(Observation.java:565)
                at org.springframework.ai.chat.client.DefaultChatClient$DefaultCallResponseSpec.doGetObservableChatResponse(DefaultChatClient.java:482)
                at org.springframework.ai.chat.client.DefaultChatClient$DefaultCallResponseSpec.doSingleWithBeanOutputConverter(DefaultChatClient.java:456)
                at org.springframework.ai.chat.client.DefaultChatClient$DefaultCallResponseSpec.entity(DefaultChatClient.java:451)
                at by.gdev.alert.job.llm.service.aiautoreply.AiOrderAnalysisService.lambda$analyze$0(AiOrderAnalysisService.java:186)
                at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:317)
                at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1144)
                at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:642)
                at java.base/java.lang.Thread.run(Thread.java:1583)
                Caused by: org.stringtemplate.v4.compiler.STException: null
                at org.stringtemplate.v4.compiler.Compiler.reportMessageAndThrowSTException(Compiler.java:224)
                at org.stringtemplate.v4.compiler.Compiler.compile(Compiler.java:154)
                at org.stringtemplate.v4.STGroup.compile(STGroup.java:514)
                at org.stringtemplate.v4.ST.<init>(ST.java:162)
                at org.stringtemplate.v4.ST.<init>(ST.java:156)
                at org.springframework.ai.chat.prompt.PromptTemplate.<init>(PromptTemplate.java:80)
	... 16 common frames omitted

                2026-03-30T12:31:45.905+03:00  INFO 25588 --- [llm] [nio-8033-exec-8] b.g.a.j.l.s.a.AiOrderAnalysisService     : AI ANALYSIS PROMPT FILE: prompts/analysis_default.txt
                2026-03-30T12:31:45.906+03:00  INFO 25588 --- [llm] [nio-8033-exec-8] b.g.a.j.l.s.a.AiOrderAnalysisService     : AI ANALYSIS PROMPT FILE: prompts/analysis_default.txt, estimatedTokens: 1253
                106:17: 'true' came as a complete surprise to me*/
                /*AiDecision decision = chatClient.prompt()
                        .user(prompt)
                        .call()
                        .entity(AiDecision.class);
                 */

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

