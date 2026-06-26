package by.gdev.alert.job.llm.config.startup;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Компонент, выполняющий проверку доступности AI‑провайдера при старте приложения.
 * <p>
 * Использует {@link ChatClient} для отправки тестового запроса
 * и логирует результат: доступность сервиса и используемую модель.
 * <p>
 * Основные особенности:
 * <ul>
 *     <li>Выполняется автоматически при запуске благодаря {@link ApplicationRunner}.</li>
 *     <li>Отправляет минимальный запрос к LLM для проверки соединения.</li>
 *     <li>Определяет провайдера по base URL (Groq, OpenRouter и т.д.).</li>
 *     <li>Логирует успешный результат или ошибку подключения.</li>
 * </ul>
 */
@Component
@Slf4j
public class AiStartupCheck implements ApplicationRunner {

    /**
     * Клиент для выполнения тестового запроса к LLM.
     */
    private final ChatClient chatClientStartup;

    /**
     * Имя модели, указанной в конфигурации Spring AI.
     */
    @Value("${spring.ai.openai.chat.options.model}")
    private String model;

    /**
     * Базовый URL AI‑провайдера (Groq, OpenRouter и т.д.).
     */
    @Value("${AI_BASE_URL}")
    private String baseUrl;

    /**
     * Конструктор, принимающий ChatClient, созданный в конфигурации.
     *
     * @param chatClientStartup клиент для выполнения LLM‑запросов
     */
    public AiStartupCheck(ChatClient chatClientStartup) {
        this.chatClientStartup = chatClientStartup;
    }

    /**
     * Выполняет тестовый запрос к AI‑провайдеру при старте приложения.
     * Логирует результат доступности и используемую модель.
     *
     * @param args аргументы запуска приложения
     */
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

    /**
     * Определяет AI‑провайдера по URL.
     *
     * @param url базовый URL
     * @return человекочитаемое имя провайдера
     */
    private String detectProvider(String url) {
        if (url.contains("groq")) return "Groq";
        if (url.contains("openrouter")) return "OpenRouter";
        return "AI провайдер";
    }
}
