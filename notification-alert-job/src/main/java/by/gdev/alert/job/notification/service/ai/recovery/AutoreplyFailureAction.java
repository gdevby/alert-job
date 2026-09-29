package by.gdev.alert.job.notification.service.ai.recovery;

/**
 * Что делать после ошибки автоответа / валидации учётки.
 */
public enum AutoreplyFailureAction {
    /** Повторить с тем же прокси (краткий сбой UI/тайминг). */
    RETRY_SAME,
    /** Сменить прокси и повторить (IP-блок, advanced captcha, DDoS). */
    ROTATE_PROXY,
    /** Не повторять (неверный пароль, OTP, разметка и т.п.). */
    ABORT
}
