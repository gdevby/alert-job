package by.gdev.alert.job.core.model.credential.dto;

/** Состояние фоновой проверки учётных данных в core. */
public enum CredentialValidationJobStatus {
    /** Playwright-проверка ещё выполняется. */
    PENDING,
    /** Вход на биржу успешен. */
    COMPLETED,
    /** Ошибка входа или сбой вызова notification. */
    FAILED
}
