package by.gdev.alert.job.llm.config.startup;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AiStartupCheck implements ApplicationRunner {

    private final ChatClient chatClientStartup;

    @Value("${spring.ai.openai.chat.options.model}")
    private String model;

    @Value("${AI_BASE_URL}")
    private String baseUrl;

    public AiStartupCheck(ChatClient chatClientStartup) {
        this.chatClientStartup = chatClientStartup;
    }

    @Override
    public void run(ApplicationArguments args) {
        String provider = detectProvider(baseUrl);

        try {
            chatClientStartup
                    .prompt("Respond with OK only.")
                    .call()
                    .content();

            log.debug("{} доступен", provider);
            log.debug("Используемая модель: {}", model);
        } catch (Exception e) {
            log.error("{} недоступен: {}", provider, e.getMessage());
        }
    }

    private String detectProvider(String url) {
        if (url.contains("groq")) return "Groq";
        if (url.contains("openrouter")) return "OpenRouter";
        return "AI провайдер";
    }
}
