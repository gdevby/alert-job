package by.gdev.alert.job.notification.service.ai.parser.impl;

import by.gdev.alert.job.notification.model.AutoreplyMode;
import by.gdev.alert.job.notification.model.dto.AiNotificationPayload;
import by.gdev.alert.job.notification.model.dto.DecryptedCredential;
import by.gdev.alert.job.notification.service.ai.merics.AutoreplyErrorTypes;
import by.gdev.alert.job.notification.service.ai.parser.AutoreplyPlaywrightParser;
import by.gdev.alert.job.notification.service.ai.proxy.AssignedProxyService;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepResult;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepType;
import by.gdev.common.model.SiteName;
import by.gdev.common.service.playwright.captcha.CaptchaService;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FlRuAutoreplyParser extends AutoreplyParser implements AutoreplyPlaywrightParser {

    private static final String[] LOGIN_ERROR_SELECTORS = {
            "div.invalid-feedback.mt-8.d-block:has-text('Неверный логин/пароль')",
            "text=Неверный логин/пароль"
    };

    /** Признаки страницы FL.ru "Введите код из письма для входа" (вход по коду на e-mail). */
    private static final String[] EMAIL_CODE_SELECTORS = {
            "form.js-send-confirmation-code",
            "h2:has-text('Введите код из письма для входа')",
            "a[href*='/account/repeat-send-code']"
    };

    private static final String EMAIL_CODE_MESSAGE =
            "На FL.ru включён вход по коду из письма. Автоотклики работать не будут, пока вы не отключите "
                    + "подтверждение входа по e-mail в настройках профиля FL.ru (Настройки -> Безопасность).";

    private final CaptchaService captchaService;

    @Value("${parser.autoreply.headless.fl.ru:true}")
    private void setHeadless(boolean headless) {
        this.headless = headless;
    }

    @Value("${parser.autoreply.proxy.fl.ru:false}")
    private void setProxy(boolean proxy) {
        this.proxy = proxy;
    }

    @Value("${parser.autoreply.send.request.fl.ru:true}")
    private void setOnSendRequest(boolean sendRequest) {
        this.sendRequest = sendRequest;
    }

    public FlRuAutoreplyParser(AssignedProxyService assignedProxyService, CaptchaService captchaService) {
        super(assignedProxyService);
        this.captchaService = captchaService;
    }

    @Override
    protected String sessionCookieCheckUrl() {
        return "https://www.fl.ru";
    }

    @Override
    protected StepResult<Void> login(Page page, AiNotificationPayload payload, DecryptedCredential creds, AutoreplyMode mode) {
        log.info("АВТООТВЕТ: {} -> НАЧАЛО ЛОГИНА, пользователь: {}", getSiteName(), creds.login());

        try {
            safeNavigate(page, "https://www.fl.ru/account/login/");
            log.info("АВТООТВЕТ: {} -> страница логина загружена, пользователь: {}", getSiteName(), creds.login());

            humanWarmup(page);

            if (!waitOrFail(page, "input[name='username']", 8000, "Поле логина")) {
                report(Level.WARN, log,
                        "АВТООТВЕТ: " + getSiteName() + " -> НЕ НАЙДЕНО ПОЛЕ ЛОГИНА, пользователь: " + creds.login(),
                        AutoreplyErrorTypes.FIELD_NOT_FOUND);
                return StepResult.fail(StepType.SEND_AUTOREPLY, "Поле логина не найдено", captureScreenshot(page));
            }

            try {
                getCurrentManager().humanMouse(page);
                getCurrentManager().humanDelay(page);
                getCurrentManager().humanType(page, "input[name='username']", creds.login());
                log.info("АВТООТВЕТ: {} -> логин заполнен: {}", getSiteName(), creds.login());
            } catch (Exception e) {
                report(Level.WARN, log,
                        "АВТООТВЕТ: " + getSiteName() + " -> НЕ УДАЛОСЬ ЗАПОЛНИТЬ ЛОГИН, пользователь: "
                                + creds.login() + ", ошибка: " + e.getMessage(),
                        AutoreplyErrorTypes.FIELD_NOT_FOUND);
                return StepResult.fail(StepType.SEND_AUTOREPLY, "Не удалось заполнить логин: " + e.getMessage(), captureScreenshot(page));
            }

            try {
                getCurrentManager().humanMouse(page);
                getCurrentManager().humanDelay(page);
                getCurrentManager().humanType(page, "input[name='password']", creds.password());
                log.info("АВТООТВЕТ: {} -> пароль заполнен для пользователя: {}", getSiteName(), creds.login());
            } catch (Exception e) {
                report(Level.WARN, log,
                        "АВТООТВЕТ: " + getSiteName() + " -> НЕ УДАЛОСЬ ЗАПОЛНИТЬ ПАРОЛЬ, пользователь: "
                                + creds.login() + ", ошибка: " + e.getMessage(),
                        AutoreplyErrorTypes.FIELD_NOT_FOUND);
                return StepResult.fail(StepType.SEND_AUTOREPLY, "Не удалось заполнить пароль: " + e.getMessage(), captureScreenshot(page));
            }

            getCurrentManager().humanScroll(page);
            getCurrentManager().humanDelay(page);

            log.info("АВТООТВЕТ: {} -> попытка прохождения SmartCaptcha для пользователя: {}", getSiteName(), creds.login());
            if (!captchaService.solveYandexSmartCaptcha(page)) {
                report(Level.WARN, log,
                        "АВТООТВЕТ: " + getSiteName() + " -> SmartCaptcha НЕ ПРОЙДЕНА, пользователь: " + creds.login(),
                        AutoreplyErrorTypes.CAPTCHA_FAILED);
                return StepResult.fail(StepType.SEND_AUTOREPLY, "SmartCaptcha не пройдена", captureScreenshot(page));
            }
            log.info("АВТООТВЕТ: {} -> SmartCaptcha пройдена, пользователь: {}", getSiteName(), creds.login());

            if (!clickOrFail(page, "#submit-button", 8000, "Кнопка 'Войти'")) {
                report(Level.WARN, log,
                        "АВТООТВЕТ: " + getSiteName() + " -> НЕ НАЙДЕНА КНОПКА 'Войти', пользователь: " + creds.login(),
                        AutoreplyErrorTypes.BUTTON_NOT_FOUND);
                return StepResult.fail(StepType.SEND_AUTOREPLY, "Кнопка 'Войти' не найдена", captureScreenshot(page));
            }
            log.info("АВТООТВЕТ: {} -> кнопка 'Войти' нажата, пользователь: {}", getSiteName(), creds.login());

            if (waitForLoginError(page, 3000)) {
                report(Level.WARN, log,
                        "АВТООТВЕТ: " + getSiteName() + " -> НЕВЕРНЫЙ ЛОГИН/ПАРОЛЬ, пользователь: " + creds.login(),
                        AutoreplyErrorTypes.LOGIN_FAILED);
                return StepResult.fail(StepType.SEND_AUTOREPLY, "Неверный логин/пароль", captureScreenshot(page));
            }

            try {
                page.waitForLoadState(LoadState.NETWORKIDLE);
                log.info("АВТООТВЕТ: {} -> страница загружена после входа, пользователь: {}", getSiteName(), creds.login());
            } catch (Exception e) {
                report(Level.WARN, log,
                        "АВТООТВЕТ: " + getSiteName() + " -> НЕ УДАЛОСЬ ДОЖДАТЬСЯ ЗАГРУЗКИ ПОСЛЕ ВХОДА, пользователь: "
                                + creds.login() + ", ошибка: " + e.getMessage(),
                        AutoreplyErrorTypes.TIMEOUT);
                return StepResult.fail(StepType.SEND_AUTOREPLY, "Не удалось дождаться загрузки после входа: " + e.getMessage(), captureScreenshot(page));
            }

            if (page.url().contains("/account/login")) {
                if (isLoginErrorPresent(page)) {
                    report(Level.WARN, log,
                            "АВТООТВЕТ: " + getSiteName() + " -> НЕВЕРНЫЙ ЛОГИН/ПАРОЛЬ, пользователь: " + creds.login(),
                            AutoreplyErrorTypes.LOGIN_FAILED);
                    return StepResult.fail(StepType.SEND_AUTOREPLY, "Неверный логин/пароль", captureScreenshot(page));
                }
                report(Level.WARN, log,
                        "АВТООТВЕТ: " + getSiteName() + " -> ВХОД НЕ ВЫПОЛНЕН, остались на странице логина, пользователь: " + creds.login(),
                        AutoreplyErrorTypes.LOGIN_FAILED);
                return StepResult.fail(StepType.SEND_AUTOREPLY, "Остались на странице логина", captureScreenshot(page));
            }

            log.info("АВТООТВЕТ: {} -> ЛОГИН УСПЕШЕН, пользователь: {}", getSiteName(), creds.login());
            setOtp(payload, null, false);
            return StepResult.ok(StepType.SEND_AUTOREPLY, null);

        } catch (Exception e) {
            log.error("АВТООТВЕТ: {} -> ОШИБКА ПРИ ЛОГИНЕ, пользователь: {}, ошибка: {}", getSiteName(), creds.login(), e.getMessage(), e);
            report(Level.ERROR, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> ОШИБКА ПРИ ЛОГИНЕ, пользователь: "
                            + creds.login() + ", ошибка: " + e.getMessage(),
                    AutoreplyErrorTypes.LOGIN_FAILED);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Ошибка при логине: " + e.getMessage(), captureScreenshot(page));
        }
    }

    private boolean waitForLoginError(Page page, int timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (isLoginErrorPresent(page)) {
                return true;
            }
            page.waitForTimeout(300);
        }
        return isLoginErrorPresent(page);
    }

    /**
     * FL.ru показывает форму кода из письма и после логина, и при заходе с восстановленной сессией,
     * причём прямо на главной — URL при этом не меняется, поэтому проверяем по разметке.
     */
    @Override
    protected StepResult<Void> checkAfterLogin(Page page, DecryptedCredential creds) {
        if (!isEmailCodePage(page)) {
            return null;
        }
        report(Level.WARN, log,
                "АВТООТВЕТ: " + getSiteName() + " -> ВКЛЮЧЁН ВХОД ПО КОДУ ИЗ ПИСЬМА, пользователь: " + creds.login(),
                AutoreplyErrorTypes.EMAIL_CODE_LOGIN_ENABLED);
        return StepResult.fail(StepType.SEND_AUTOREPLY, EMAIL_CODE_MESSAGE, captureScreenshot(page));
    }

    private boolean isEmailCodePage(Page page) {
        for (String selector : EMAIL_CODE_SELECTORS) {
            try {
                if (page.locator(selector).count() > 0) {
                    log.debug("АВТООТВЕТ: {} -> страница ввода кода из письма обнаружена (селектор: {})", getSiteName(), selector);
                    return true;
                }
            } catch (Exception e) {
                log.debug("АВТООТВЕТ: {} -> проверка страницы кода через '{}' не удалась: {}", getSiteName(), selector, e.getMessage());
            }
        }
        return false;
    }

    private boolean isLoginErrorPresent(Page page) {
        for (String selector : LOGIN_ERROR_SELECTORS) {
            try {
                Locator locator = page.locator(selector).first();
                if (locator.isVisible()) {
                    log.debug("АВТООТВЕТ: {} -> ошибка логина обнаружена (селектор: {})", getSiteName(), selector);
                    return true;
                }
            } catch (Exception e) {
                log.debug("АВТООТВЕТ: {} -> проверка ошибки логина через '{}' не удалась: {}", getSiteName(), selector, e.getMessage());
            }
        }
        return false;
    }

    @Override
    protected StepResult<Void> processAutoReply(Page page, AiNotificationPayload payload, DecryptedCredential creds) {
        String link = payload.getOrder().getLink();
        String login = creds.login();
        log.info("АВТООТВЕТ: {} -> НАЧАЛО ОБРАБОТКИ ЗАКАЗА: {}, пользователь: {}", getSiteName(), link, login);

        try {
            page.navigate(link);
            page.waitForLoadState(LoadState.NETWORKIDLE);
            log.info("АВТООТВЕТ: {} -> страница заказа открыта, пользователь: {}", getSiteName(), login);
            takeScreenshot(page, getSiteName(), payload.getUser().getUuid(), "order_page");
        } catch (Exception e) {
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> НЕ УДАЛОСЬ ОТКРЫТЬ ЗАКАЗ, пользователь: "
                            + login + ", ошибка: " + e.getMessage(),
                    AutoreplyErrorTypes.ERROR_OPEN_PAGE);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Не удалось открыть заказ: " + e.getMessage(), captureScreenshot(page));
        }

        if (!waitOrFail(page, "#el-descr", 8000, "Поле текста отклика")) {
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> НЕ НАЙДЕНО ПОЛЕ ТЕКСТА ОТКЛИКА, пользователь: " + login,
                    AutoreplyErrorTypes.FIELD_NOT_FOUND);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Поле текста отклика не найдено", captureScreenshot(page));
        }

        try {
            page.fill("#el-descr", payload.getDecision().reply());
            log.info("АВТООТВЕТ: {} -> текст ответа вставлен, длина: {}, пользователь: {}", getSiteName(),
                    payload.getDecision().reply() != null ? payload.getDecision().reply().length() : 0, login);
        } catch (Exception e) {
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> НЕ УДАЛОСЬ ЗАПОЛНИТЬ ТЕКСТ ОТКЛИКА, пользователь: "
                            + login + ", ошибка: " + e.getMessage(),
                    AutoreplyErrorTypes.FIELD_NOT_FOUND);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Не удалось заполнить текст отклика: " + e.getMessage(), captureScreenshot(page));
        }

        try {
            Locator payRadio = page.locator("label[for='el-pay-0']");
            if (payRadio.count() > 0) {
                payRadio.click();
                log.info("АВТООТВЕТ: {} -> выбран способ оплаты 'На банковскую карту физ. лица', пользователь: {}", getSiteName(), login);
            } else {
                log.warn("АВТООТВЕТ: {} -> радиокнопка оплаты не найдена, пользователь: {}", getSiteName(), login);
            }
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> ошибка при выборе способа оплаты, пользователь: {}, ошибка: {}", getSiteName(), login, e.getMessage());
            // Не критично, продолжаем
        }

        if (!waitOrFail(page, "#el-time_from", 8000, "Поле срока выполнения")) {
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> НЕ НАЙДЕНО ПОЛЕ СРОКА ВЫПОЛНЕНИЯ, пользователь: " + login,
                    AutoreplyErrorTypes.FIELD_NOT_FOUND);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Поле срока выполнения не найдено", captureScreenshot(page));
        }

        try {
            String duration = String.valueOf(defaultDays);
            page.fill("#el-time_from", duration);
            log.info("АВТООТВЕТ: {} -> срок выполнения установлен: {} дней, пользователь: {}", getSiteName(), defaultDays, login);
        } catch (Exception e) {
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> НЕ УДАЛОСЬ ЗАПОЛНИТЬ СРОК ВЫПОЛНЕНИЯ, пользователь: "
                            + login + ", ошибка: " + e.getMessage(),
                    AutoreplyErrorTypes.FIELD_NOT_FOUND);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Не удалось заполнить срок выполнения: " + e.getMessage(), captureScreenshot(page));
        }

        if (!waitOrFail(page, "#el-cost_from", 8000, "Поле цены")) {
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> НЕ НАЙДЕНО ПОЛЕ ЦЕНЫ, пользователь: " + login,
                    AutoreplyErrorTypes.FIELD_NOT_FOUND);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Поле цены не найдено", captureScreenshot(page));
        }

        try {
            String price = String.valueOf(defaultPrice);
            page.fill("#el-cost_from", price);
            log.info("АВТООТВЕТ: {} -> цена установлена: {}, пользователь: {}", getSiteName(), defaultPrice, login);
        } catch (Exception e) {
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> НЕ УДАЛОСЬ ЗАПОЛНИТЬ ЦЕНУ, пользователь: "
                            + login + ", ошибка: " + e.getMessage(),
                    AutoreplyErrorTypes.FIELD_NOT_FOUND);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Не удалось заполнить цену: " + e.getMessage(), captureScreenshot(page));
        }

        if (!waitOrFail(page, "#el-submit", 8000, "Кнопка отправки отклика")) {
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> НЕ НАЙДЕНА КНОПКА ОТПРАВКИ ОТКЛИКА, пользователь: " + login,
                    AutoreplyErrorTypes.BUTTON_NOT_FOUND);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Кнопка отправки отклика не найдена", captureScreenshot(page));
        }
        log.info("АВТООТВЕТ: {} -> кнопка отправки найдена, пользователь: {}", getSiteName(), login);

        Locator sendBtn = page.locator("#el-submit");
        try {
            page.waitForCondition(sendBtn::isEnabled,
                    new Page.WaitForConditionOptions().setTimeout(5000));
            log.info("АВТООТВЕТ: {} -> кнопка отправки активна, пользователь: {}", getSiteName(), login);
        } catch (Exception e) {
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> КНОПКА ОТПРАВКИ НЕАКТИВНА, пользователь: "
                            + login + ", ошибка: " + e.getMessage(),
                    AutoreplyErrorTypes.BUTTON_NOT_FOUND);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Кнопка отправки неактивна", captureScreenshot(page));
        }

        if (sendRequest) {
            try {
                sendBtn.click();
                log.info("АВТООТВЕТ: {} -> ЗАЯВКА УСПЕШНО ОТПРАВЛЕНА, пользователь: {}", getSiteName(), login);
            } catch (Exception e) {
                report(Level.WARN, log,
                        "АВТООТВЕТ: " + getSiteName() + " -> НЕ УДАЛОСЬ НАЖАТЬ КНОПКУ ОТПРАВКИ, пользователь: "
                                + login + ", ошибка: " + e.getMessage(),
                        AutoreplyErrorTypes.BUTTON_NOT_FOUND);
                return StepResult.fail(StepType.SEND_AUTOREPLY, "Не удалось нажать кнопку отправки: " + e.getMessage(), captureScreenshot(page));
            }
        } else {
            log.info("АВТООТВЕТ: {} -> ЗАЯВКА НЕ ОТПРАВЛЕНА (sendRequest=false), пользователь: {}", getSiteName(), login);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Заявка не отправлена (sendRequest=false)", captureScreenshot(page));
        }

        page.waitForTimeout(2000);
        log.info("АВТООТВЕТ: {} -> ОТКЛИК УСПЕШНО ЗАВЕРШЁН, пользователь: {}", getSiteName(), login);
        return StepResult.ok(StepType.SEND_AUTOREPLY, null);
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.FLRU;
    }
}