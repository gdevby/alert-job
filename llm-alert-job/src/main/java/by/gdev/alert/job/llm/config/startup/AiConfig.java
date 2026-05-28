package by.gdev.alert.job.llm.config.startup;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    public ChatClient chatClientStartup(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
