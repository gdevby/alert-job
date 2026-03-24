package by.gdev.alert.job.llm.config.startup;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("openrouter")
public class OpenRouterStartupCheck implements ApplicationRunner {

        private final ChatClient chatClientStartup;
        @Value("${spring.ai.openai.chat.options.model}")
        private String model;

    public OpenRouterStartupCheck(ChatClient chatClientStartup) {
        this.chatClientStartup = chatClientStartup;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            chatClientStartup
                    .prompt("Respond with the word OK only.")
                    .call()
                    .content();

            System.out.println("OpenRouter доступен");
            System.out.println("Используемая модель: " + model);
        } catch (Exception e) {
            System.err.println("OpenRouter недоступен: " + e.getMessage());
        }
    }
}
