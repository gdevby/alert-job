package by.gdev.alert.job.notification.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)

public record AiDecision(
        String reply
) {}
