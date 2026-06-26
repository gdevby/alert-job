package by.gdev.alert.job.llm.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Сервис для выполнения простых запросов к LLM.
 * <p>
 * Предоставляет минимальный интерфейс для отправки текстового промта
 * и получения ответа модели в виде строки.
 * Используется в местах, где не требуется сложная логика анализа
 * или шаблонизации.
 */
@Service
public class LlmService {

    /**
     * Клиент для взаимодействия с LLM.
     */
    private final ChatClient chatClient;

    /**
     * Создаёт сервис на основе переданного билдера ChatClient.
     *
     * @param builder фабрика для создания ChatClient
     */
    public LlmService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    /**
     * Отправляет произвольный текстовый промт в LLM
     * и возвращает ответ модели как строку.
     *
     * @param prompt текст запроса
     * @return ответ модели
     */
    public String ask(String prompt) {
        return chatClient
                .prompt()
                .user(prompt)
                .call()
                .content();
    }
}
