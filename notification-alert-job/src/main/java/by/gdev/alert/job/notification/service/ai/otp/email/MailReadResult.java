package by.gdev.alert.job.notification.service.ai.otp.email;

import java.util.Collections;
import java.util.List;

/**
 * @param connected     удалось установить IMAP-сессию и открыть папку
 * @param errorMessage  при {@code connected == false} — причина сбоя connect; при {@code true} — предупреждение о частичном сбое чтения
 */
public record MailReadResult(List<MailDto> messages, boolean connected, String errorMessage) {

    public static MailReadResult connectFailed(String message) {
        return new MailReadResult(Collections.emptyList(), false, message);
    }

    public static MailReadResult success(List<MailDto> messages) {
        return new MailReadResult(messages, true, null);
    }

    public static MailReadResult partialAfterConnect(List<MailDto> messages, String message) {
        return new MailReadResult(messages, true, message);
    }
}
