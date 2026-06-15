package by.gdev.alert.job.llm.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiDecision {

        @JsonPropertyDescription("Нужно ли вообще отвечать на сообщение (true — отвечаем, false — игнорируем)")
        private boolean shouldReply;

        @JsonPropertyDescription("Уверенность модели в своём решении (числовой коэффициент)")
        private double confidence;

        @JsonPropertyDescription("Причина, почему модель решила отвечать или не отвечать")
        private String reason;

        @JsonPropertyDescription("Сформированный текст ответа (если shouldReply = true)")
        private String reply;

        @JsonPropertyDescription("Ключевые слова, которые модель нашла в сообщении")
        private List<String> matchedKeywords;

        @JsonPropertyDescription("Ключевые слова, которые ожидались, но не были найдены")
        private List<String> missedKeywords;

        @JsonPropertyDescription("Объяснение, почему выбрана конкретная категория")
        private String categoryMatchReason;

        @JsonPropertyDescription("Объяснение, почему выбрана конкретная подкатегория")
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
