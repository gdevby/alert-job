package by.gdev.alert.job.llm.config.startup;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("groq")
public class GroqStartupCheck implements ApplicationRunner {

    private final ChatClient chatClientStartup;

    public GroqStartupCheck(ChatClient chatClientStartup) {
        this.chatClientStartup = chatClientStartup;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            chatClientStartup
                    .prompt("ping")
                    .call()
                    .content();

            System.out.println("✅ Groq доступен");
        } catch (Exception e) {
            System.err.println("❌ Groq недоступен: " + e.getMessage());
        }
    }
}
