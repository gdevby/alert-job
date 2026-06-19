package by.gdev.alert.job.llm.config.startup;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация AI‑компонентов приложения.
 * <p>
 * Отвечает за создание и инициализацию {@link ChatClient},
 * используемого для выполнения LLM‑запросов.
 * <p>
 * Основные особенности:
 * <ul>
 *     <li>Создаёт {@link ChatClient} на основе переданного {@link ChatModel}.</li>
 *     <li>Используется при старте приложения для проверки доступности AI‑провайдера.</li>
 *     <li>Является частью Spring‑контекста благодаря {@code @Configuration}.</li>
 * </ul>
 */
@Configuration
public class AiConfig {

    /**
     * Создаёт {@link ChatClient}, используемый для выполнения запросов к LLM.
     *
     * @param chatModel модель, предоставляемая Spring AI (например, Groq, OpenRouter и т.д.)
     * @return сконфигурированный клиент для общения с AI
     */
    @Bean
    public ChatClient chatClientStartup(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
