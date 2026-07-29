package by.gdev.alert.job.llm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class LlmExecutorConfig {

    @Bean
    public ExecutorService llmExecutor() {
        return Executors.newSingleThreadExecutor();
    }
}
