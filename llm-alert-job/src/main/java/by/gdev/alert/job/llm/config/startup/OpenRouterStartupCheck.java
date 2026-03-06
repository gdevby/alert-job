package by.gdev.alert.job.llm.config.startup;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("openrouter")
public class OpenRouterStartupCheck implements ApplicationRunner {

    private final ChatClient chatClientStartup;

    public OpenRouterStartupCheck(ChatClient chatClientStartup) {
        this.chatClientStartup = chatClientStartup;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            chatClientStartup.prompt("ping").call().content();
            System.out.println("✅ OpenRouter доступен");
        } catch (Exception e) {
            System.err.println("❌ OpenRouter недоступен: " + e.getMessage());
        }
    }
}
