package by.gdev.alert.job.llm.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class LlmService {

    private final ChatClient chatClient;

    public LlmService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String ask(String prompt) {
        return chatClient
                .prompt()
                .user(prompt)
                .call()
                .content();
    }
}



