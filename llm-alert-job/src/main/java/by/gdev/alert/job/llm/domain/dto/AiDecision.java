package by.gdev.alert.job.llm.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * Решение AI по поводу автоответа:
 *  - текст ответа;
 * Используется для логирования и анализа поведения LLM.
 */
@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Решение AI о необходимости автоответа и объяснение выбора")
public class AiDecision {

        /** Сформированный текст ответа */
        @JsonPropertyDescription("Сформированный текст ответа")
        @Schema(description = "Ответ, сформированный AI", example = "Здравствуйте! Готов помочь…")
        private String reply;

        private String prefix;

        public AiDecision() {
        }

        @Schema(description = "Флаг валидности решения AI (true - решение принято корректно, false - ошибка или невалидный ответ в случае пустого ответа или ошибки)")
        private boolean valid;

        public AiDecision(String reply, String prefix, boolean valid) {
                this.reply = reply;
                this.prefix = prefix;
                this.valid = valid;
        }
}
