package by.gdev.alert.job.llm.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)

public record AiDecision(

        // Нужно ли вообще отвечать на сообщение (true — отвечаем, false — игнорируем)
        boolean shouldReply,

        // Уверенность модели в своём решении (числовой коэффициент)
        double confidence,

        // Причина, почему модель решила отвечать или не отвечать
        String reason,

        // Сформированный текст ответа (если shouldReply = true)
        String reply,

        // Ключевые слова, которые модель нашла в сообщении
        List<String> matchedKeywords,

        // Ключевые слова, которые ожидались, но не были найдены
        List<String> missedKeywords,

        // Объяснение, почему выбрана конкретная категория
        String categoryMatchReason,

        // Объяснение, почему выбрана конкретная подкатегория
        String subcategoryMatchReason
) {}




