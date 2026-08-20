package by.gdev.alert.job.llm.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Решение AI по поводу автоответа:
 *  - уверенность модели;
 *  - причину выбора;
 *  - текст ответа;
 * Используется для логирования и анализа поведения LLM.
 */
@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Решение AI о необходимости автоответа и объяснение выбора")
public class AiDecision {

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

        public AiDecision() {
        }

        public AiDecision(double confidence, String reason, String reply) {
                this.confidence = confidence;
                this.reason = reason;
                this.reply = reply;
        }
}
