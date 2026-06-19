package by.gdev.alert.job.llm.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Решение AI по поводу автоответа:
 *  - содержит флаг необходимости ответа;
 *  - уверенность модели;
 *  - причину выбора;
 *  - текст ответа;
 *  - найденные/ненайденные ключевые слова;
 *  - объяснение выбора категории и подкатегории.
 *
 * Используется для логирования и анализа поведения LLM.
 */
@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Решение AI о необходимости автоответа и объяснение выбора")
public class AiDecision {

        /** Нужно ли отвечать на сообщение */
        @JsonPropertyDescription("Нужно ли вообще отвечать на сообщение (true — отвечаем, false — игнорируем)")
        @Schema(description = "Флаг необходимости ответа", example = "true")
        private boolean shouldReply;

        /** Уверенность модели в своём решении */
        @JsonPropertyDescription("Уверенность модели в своём решении (числовой коэффициент)")
        @Schema(description = "Уверенность модели (0.0–1.0)", example = "0.87")
        private double confidence;

        /** Причина, почему модель решила отвечать или нет */
        @JsonPropertyDescription("Причина, почему модель решила отвечать или не отвечать")
        @Schema(description = "Причина принятого решения", example = "Найдено ключевое слово: urgent")
        private String reason;

        /** Сформированный текст ответа */
        @JsonPropertyDescription("Сформированный текст ответа (если shouldReply = true)")
        @Schema(description = "Ответ, сформированный AI", example = "Здравствуйте! Готов помочь…")
        private String reply;

        /** Ключевые слова, которые модель нашла */
        @JsonPropertyDescription("Ключевые слова, которые модель нашла в сообщении")
        @Schema(description = "Найденные ключевые слова", example = "[\"urgent\", \"help\"]")
        private List<String> matchedKeywords;

        /** Ключевые слова, которые ожидались, но не найдены */
        @JsonPropertyDescription("Ключевые слова, которые ожидались, но не были найдены")
        @Schema(description = "Ожидаемые, но ненайденные ключевые слова", example = "[\"price\", \"budget\"]")
        private List<String> missedKeywords;

        /** Объяснение выбора категории */
        @JsonPropertyDescription("Объяснение, почему выбрана конкретная категория")
        @Schema(description = "Причина выбора категории", example = "Сообщение относится к заказам")
        private String categoryMatchReason;

        /** Объяснение выбора подкатегории */
        @JsonPropertyDescription("Объяснение, почему выбрана конкретная подкатегория")
        @Schema(description = "Причина выбора подкатегории", example = "Упоминание срочности и помощи")
        private String subcategoryMatchReason;

        public AiDecision(boolean shouldReply, double confidence, String reason, String reply,
                          List<String> matchedKeywords, List<String> missedKeywords,
                          String categoryMatchReason, String subcategoryMatchReason) {
                this.shouldReply = shouldReply;
                this.confidence = confidence;
                this.reason = reason;
                this.reply = reply;
                this.matchedKeywords = matchedKeywords;
                this.missedKeywords = missedKeywords;
                this.categoryMatchReason = categoryMatchReason;
                this.subcategoryMatchReason = subcategoryMatchReason;
        }
}
