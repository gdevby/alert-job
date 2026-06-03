package by.gdev.alert.job.notification.service.ai.otp.email;

import java.util.Date;

public record MailDto(
        long uid,
        String subject,
        String from,
        String to,
        Date sentDate,
        String body
) {}
