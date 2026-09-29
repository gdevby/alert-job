package by.gdev.common.service.playwright.captcha.failure;

import by.gdev.common.service.playwright.captcha.CaptchaService;

/**
 * Последняя зафиксированная ошибка капчи в текущем потоке (см. {@link CaptchaService}).
 */
public record CaptchaFailureInfo(
        CaptchaFailureCode code,
        String userMessage,
        String phase,
        String pageUrl
) {
    public String codeName() {
        return code != null ? code.name() : null;
    }
}
