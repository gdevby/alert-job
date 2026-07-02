package by.gdev.alert.job.notification.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)

public record AiDecision(
        boolean shouldReply,
        double confidence,
        String reason,
        String reply,
        List<String> matchedKeywords,
        List<String> missedKeywords,
        String categoryMatchReason,
        String subcategoryMatchReason
) {}
