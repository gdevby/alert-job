package by.gdev.alert.job.notification.service.ai.queue.step.dto;

import by.gdev.alert.job.notification.model.dto.AiNotificationPayload;
import by.gdev.alert.job.notification.model.dto.DecryptedCredential;
import by.gdev.alert.job.notification.service.ai.parser.AutoreplyPlaywrightParser;

public record SendAutoreplyInput(
        AutoreplyPlaywrightParser parser,
        DecryptedCredential creds,
        AiNotificationPayload payload
) {}

