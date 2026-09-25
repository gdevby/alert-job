package by.gdev.alert.job.notification.service.ai.parser.impl;

import by.gdev.alert.job.notification.model.AutoreplyMode;
import by.gdev.alert.job.notification.model.dto.AiNotificationPayload;
import by.gdev.alert.job.notification.model.dto.DecryptedCredential;
import by.gdev.alert.job.notification.service.ai.merics.AutoreplyErrorTypes;
import by.gdev.alert.job.notification.service.ai.otp.OtpService;
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

import java.util.Locale;

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

    private static final String CODE_FORM = "form.js-send-confirmation-code";

    private static final String RESEND_CODE_LINK = "a[href*='/account/repeat-send-code/']";

    /** Перебираются по порядку: от самого точного описания поля к самому общему. */
    private static final String[] CODE_INPUT_SELECTORS = {
            CODE_FORM + " input[name*='code']",
            CODE_FORM + " input[type='text']",
            CODE_FORM + " input:not([type='hidden']):not([type='submit'])"
    };

    private static final String[] CODE_SUBMIT_SELECTORS = {
            CODE_FORM + " button[type='submit']",
            CODE_FORM + " input[type='submit']",
            CODE_FORM + " button"
    };

    private static final String CODE_FORM_NOT_PARSED_MESSAGE =
            "FL.ru запросил код из письма, но поле для его ввода не удалось найти на странице. "
                    + "Разметка формы записана в лог — сообщите об этом в поддержку.";

    private static final String CODE_NOT_RECEIVED_MESSAGE =
            "FL.ru запросил код подтверждения, но письмо с кодом не пришло на почтовый ящик сервиса "
                    + "за отведённое время. Проверьте, что письма от no_reply@free-lance.ru доходят и не попадают в спам.";

    private static final String CODE_REJECTED_MESSAGE =
            "FL.ru не принял код из письма — форма подтверждения осталась на экране. "
                    + "Код мог устареть, попробуйте запустить вход ещё раз.";

    private final CaptchaService captchaService;
    private final OtpService otpService;

    @Value("${credential.validation.otp.timeout.ms:120000}")
    private long otpTimeoutMs;

    /** Сколько раз ждать письмо (первая отправка + повторные по ссылке на сайте). */
    @Value("${parser.autoreply.fl.ru.otp.resend.max-rounds:3}")
    private int otpResendMaxRounds;

    /** Сколько ждать письмо в каждом раунде, прежде чем нажать «Отправить повторно». */
    @Value("${parser.autoreply.fl.ru.otp.wait-per-round.ms:90000}")
    private long otpWaitPerRoundMs;

    /** Максимум ждать, пока на странице снова станет доступна ссылка повторной отправки. */
    @Value("${parser.autoreply.fl.ru.otp.resend.cooldown.max-wait.ms:120000}")
    private long otpResendCooldownMaxWaitMs;

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

    public FlRuAutoreplyParser(AssignedProxyService assignedProxyService, CaptchaService captchaService,
                               OtpService otpService) {
        super(assignedProxyService);
        this.captchaService = captchaService;
        this.otpService = otpService;
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

            // Старые непрочитанные письма с кодом из прошлых попыток иначе подставляются вместо нового.
            otpService.beginOtpWait(SiteName.FLRU.name(), creds.login());

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

            waitForLoginCompletionOrEmailCode(page, 15000);

            try {
                page.waitForLoadState(LoadState.NETWORKIDLE,
                        new Page.WaitForLoadStateOptions().setTimeout(12000));
                log.info("АВТООТВЕТ: {} -> страница загружена после входа, пользователь: {}", getSiteName(), creds.login());
            } catch (Exception e) {
                // На форме кода таймер «отправить повторно» держит сеть активной — это не ошибка логина.
                if (!isEmailCodePage(page) && page.url().contains("/account/login")) {
                    report(Level.WARN, log,
                            "АВТООТВЕТ: " + getSiteName() + " -> НЕ УДАЛОСЬ ДОЖДАТЬСЯ ЗАГРУЗКИ ПОСЛЕ ВХОДА, пользователь: "
                                    + creds.login() + ", ошибка: " + e.getMessage(),
                            AutoreplyErrorTypes.TIMEOUT);
                    return StepResult.fail(StepType.SEND_AUTOREPLY,
                            "Не удалось дождаться загрузки после входа: " + e.getMessage(), captureScreenshot(page));
                }
                log.debug("АВТООТВЕТ: {} -> NETWORKIDLE не достигнут после входа, продолжаем (форма кода или редирект), пользователь: {}",
                        getSiteName(), creds.login());
            }

            if (isEmailCodePage(page)) {
                log.info("АВТООТВЕТ: {} -> FL.ru запросил код из письма (url={}), пользователь: {}",
                        getSiteName(), page.url(), creds.login());
                return StepResult.ok(StepType.SEND_AUTOREPLY, null);
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
     * Ждёт OTP из почты; если за раунд письмо не пришло — нажимает «Отправить повторно» на FL.ru
     * (с паузой по таймеру на странице) и повторяет ожидание.
     */
    private String waitForOtpWithResend(Page page, DecryptedCredential creds) {
        int rounds = Math.max(1, otpResendMaxRounds);
        long perRoundMs = otpWaitPerRoundMs > 0 ? otpWaitPerRoundMs : Math.max(30_000, otpTimeoutMs / rounds);

        for (int round = 1; round <= rounds; round++) {
            log.info("АВТООТВЕТ: {} -> ожидание кода из письма для {}, раунд {}/{}, таймаут {} ms",
                    getSiteName(), creds.login(), round, rounds, perRoundMs);
            String otp = otpService.waitForOtp(SiteName.FLRU.name(), creds.login(), perRoundMs);
            if (otp != null) {
                return otp;
            }
            if (round >= rounds) {
                break;
            }
            log.info("АВТООТВЕТ: {} -> код не пришёл за раунд {}, запрашиваем повторную отправку, пользователь: {}",
                    getSiteName(), round, creds.login());
            if (!requestFlRuCodeResend(page, creds)) {
                log.warn("АВТООТВЕТ: {} -> не удалось запросить повторную отправку кода, пользователь: {}",
                        getSiteName(), creds.login());
                break;
            }
        }
        return null;
    }

    private boolean requestFlRuCodeResend(Page page, DecryptedCredential creds) {
        try {
            waitUntilResendLinkAvailable(page, otpResendCooldownMaxWaitMs);
            Locator link = page.locator(RESEND_CODE_LINK).first();
            if (link.count() == 0 || !link.isVisible()) {
                log.warn("АВТООТВЕТ: {} -> ссылка «Отправить повторно» недоступна, пользователь: {}",
                        getSiteName(), creds.login());
                return false;
            }
            otpService.beginOtpWait(SiteName.FLRU.name(), creds.login());
            link.click();
            try {
                page.waitForLoadState(LoadState.LOAD,
                        new Page.WaitForLoadStateOptions().setTimeout(15_000));
            } catch (Exception e) {
                log.debug("АВТООТВЕТ: {} -> LOAD после повторной отправки: {}", getSiteName(), e.getMessage());
            }
            log.info("АВТООТВЕТ: {} -> нажата «Отправить повторно», пользователь: {}", getSiteName(), creds.login());
            return isEmailCodePage(page);
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> ошибка повторной отправки кода: {}, пользователь: {}",
                    getSiteName(), e.getMessage(), creds.login());
            return false;
        }
    }

    /**
     * FL.ru скрывает ссылку на время обратного отсчёта ({@code #counter} / {@code #seconds}),
     * затем показывает {@code #repeat-send}.
     */
    private void waitUntilResendLinkAvailable(Page page, long maxWaitMs) {
        long deadline = System.currentTimeMillis() + maxWaitMs;
        while (System.currentTimeMillis() < deadline) {
            Locator link = page.locator("#repeat-send " + RESEND_CODE_LINK).first();
            if (link.count() > 0 && link.isVisible()) {
                return;
            }
            link = page.locator(RESEND_CODE_LINK).first();
            if (link.count() > 0 && link.isVisible()) {
                return;
            }
            long waitMs = pollResendCooldownMs(page);
            if (waitMs > 0) {
                long remaining = deadline - System.currentTimeMillis();
                page.waitForTimeout(Math.min(waitMs, Math.max(remaining, 0)));
            } else {
                page.waitForTimeout(500);
            }
        }
    }

    private long pollResendCooldownMs(Page page) {
        try {
            Locator counter = page.locator("#counter");
            if (counter.count() == 0 || !counter.isVisible()) {
                return 0;
            }
            String secText = page.locator("#seconds").innerText().trim();
            int seconds = Integer.parseInt(secText.replaceAll("[^\\-0-9]", ""));
            if (seconds <= 0) {
                return 500;
            }
            log.debug("АВТООТВЕТ: {} -> повторная отправка кода через {} с", getSiteName(), seconds);
            return seconds * 1000L + 800L;
        } catch (Exception e) {
            return 0;
        }
    }

    /** Ждём исчезновения формы кода после отправки (URL при этом может не меняться). */
    private void waitForEmailCodeAccepted(Page page, int timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (!isEmailCodePage(page)) {
                return;
            }
            page.waitForTimeout(300);
        }
        try {
            page.waitForLoadState(LoadState.LOAD,
                    new Page.WaitForLoadStateOptions().setTimeout(5000));
        } catch (Exception ignored) {
            // таймер «отправить повторно» может мешать NETWORKIDLE
        }
    }

    /** Ждём редирект с логина, форму кода из письма или сообщение об ошибке пароля. */
    private void waitForLoginCompletionOrEmailCode(Page page, int timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (isLoginErrorPresent(page) || isEmailCodePage(page)) {
                return;
            }
            if (!page.url().contains("/account/login")) {
                return;
            }
            page.waitForTimeout(300);
        }
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
        log.info("АВТООТВЕТ: {} -> запрошен код из письма, пользователь: {}", getSiteName(), creds.login());

        if (findFirst(page, CODE_INPUT_SELECTORS) == null) {
            // Разметку формы вживую не видели — выводим её, чтобы уточнить селекторы по факту.
            log.warn("АВТООТВЕТ: {} -> поле кода не найдено, разметка формы: {}", getSiteName(), dumpCodeForm(page));
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> НЕ НАЙДЕНО ПОЛЕ КОДА ИЗ ПИСЬМА, пользователь: " + creds.login(),
                    AutoreplyErrorTypes.FIELD_NOT_FOUND);
            return StepResult.fail(StepType.SEND_AUTOREPLY, CODE_FORM_NOT_PARSED_MESSAGE, captureScreenshot(page));
        }

        String otp = waitForOtpWithResend(page, creds);
        if (otp == null) {
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> КОД ИЗ ПИСЬМА НЕ ПОЛУЧЕН за отведённое время, пользователь: "
                            + creds.login(),
                    AutoreplyErrorTypes.OTP_NOT_RECEIVED);
            return StepResult.fail(StepType.SEND_AUTOREPLY, CODE_NOT_RECEIVED_MESSAGE, captureScreenshot(page));
        }
        String code = otp.toUpperCase(Locale.ROOT);
        log.info("АВТООТВЕТ: {} -> код из письма получен ({} символов), пользователь: {}",
                getSiteName(), code.length(), creds.login());

        Locator input = findFirst(page, CODE_INPUT_SELECTORS);
        if (input == null) {
            log.warn("АВТООТВЕТ: {} -> поле кода пропало после ожидания письма, разметка: {}",
                    getSiteName(), dumpCodeForm(page));
            return StepResult.fail(StepType.SEND_AUTOREPLY, CODE_FORM_NOT_PARSED_MESSAGE, captureScreenshot(page));
        }

        try {
            input.click();
            input.fill(code);
            String filled = input.inputValue();
            log.info("АВТООТВЕТ: {} -> в поле кода введено: {} (ожидали {}), пользователь: {}",
                    getSiteName(), filled, code, creds.login());

            Locator submit = findFirst(page, CODE_SUBMIT_SELECTORS);
            if (submit != null) {
                submit.click(new Locator.ClickOptions().setTimeout(10000));
            } else {
                page.keyboard().press("Enter");
            }
            waitForEmailCodeAccepted(page, 25000);
        } catch (Exception e) {
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> НЕ УДАЛОСЬ ОТПРАВИТЬ КОД ИЗ ПИСЬМА, пользователь: "
                            + creds.login() + ", ошибка: " + e.getMessage(),
                    AutoreplyErrorTypes.FIELD_NOT_FOUND);
            return StepResult.fail(StepType.SEND_AUTOREPLY,
                    "Не удалось отправить код из письма: " + e.getMessage(), captureScreenshot(page));
        }

        if (isEmailCodePage(page)) {
            log.warn("АВТООТВЕТ: {} -> форма кода всё ещё на экране, url={}, разметка: {}, пользователь: {}",
                    getSiteName(), page.url(), dumpCodeForm(page), creds.login());
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> КОД ИЗ ПИСЬМА ОТКЛОНЁН, пользователь: " + creds.login(),
                    AutoreplyErrorTypes.EMAIL_CODE_LOGIN_ENABLED);
            return StepResult.fail(StepType.SEND_AUTOREPLY, CODE_REJECTED_MESSAGE, captureScreenshot(page));
        }

        otpService.invalidateOtp(SiteName.FLRU.name(), creds.login());
        log.info("АВТООТВЕТ: {} -> код из письма принят, пользователь: {}", getSiteName(), creds.login());
        return null;
    }

    /** Первый селектор из списка, которому на странице что-то соответствует. */
    private Locator findFirst(Page page, String[] selectors) {
        for (String selector : selectors) {
            try {
                Locator locator = page.locator(selector).first();
                if (locator.count() > 0) {
                    return locator;
                }
            } catch (Exception e) {
                log.debug("АВТООТВЕТ: {} -> селектор '{}' не сработал: {}", getSiteName(), selector, e.getMessage());
            }
        }
        return null;
    }

    private String dumpCodeForm(Page page) {
        try {
            Locator form = page.locator(CODE_FORM).first();
            return form.count() > 0 ? form.innerHTML() : "форма " + CODE_FORM + " на странице отсутствует";
        } catch (Exception e) {
            return "не удалось прочитать разметку: " + e.getMessage();
        }
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