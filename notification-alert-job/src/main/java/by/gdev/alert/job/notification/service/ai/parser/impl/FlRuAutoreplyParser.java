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

    private static final String VALIDATE_CAPTCHA_PATH = "/validate-captcha";

    /** После IP-капчи FL.ru часто редиректит на главную — нужен явный переход на вход. */
    private static final String FLRU_LOGIN_URL = "https://www.fl.ru/account/login/?return=%2F";

    private static final String[] VALIDATE_CAPTCHA_MARKERS = {
            "text=подозрительная активность",
            "text=подтвердите, что вы не робот",
            "#label:has-text('Я человек')"
    };

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
    void safeNavigate(Page page, String url) {
        super.safeNavigate(page, url);
        StepResult<Void> gate = ensurePastValidateCaptcha(page, "navigate");
        if (gate != null) {
            throw new RuntimeException(gate.getErrorMessage());
        }
        resumeNavigationAfterValidateCaptcha(page, url, "navigate");
    }

    /**
     * После {@code /validate-captcha} сайт часто оставляет на {@code https://www.fl.ru/} вместо исходного URL.
     */
    private void resumeNavigationAfterValidateCaptcha(Page page, String targetUrl, String login) {
        if (targetUrl == null || targetUrl.isBlank() || isValidateCaptchaPage(page)) {
            return;
        }
        if (!needsResumeToTarget(page.url(), targetUrl)) {
            return;
        }
        log.info("АВТООТВЕТ: {} -> после validate-captcha с {} на {}, пользователь: {}",
                getSiteName(), page.url(), targetUrl, login);
        super.safeNavigate(page, targetUrl);
        StepResult<Void> gate = ensurePastValidateCaptcha(page, login);
        if (gate != null) {
            throw new RuntimeException(gate.getErrorMessage());
        }
    }

    private static boolean needsResumeToTarget(String currentUrl, String targetUrl) {
        if (currentUrl == null) {
            return true;
        }
        if (currentUrl.contains(VALIDATE_CAPTCHA_PATH)) {
            return false;
        }
        if (targetUrl.contains("/account/login") && !currentUrl.contains("/account/login")) {
            return true;
        }
        try {
            java.net.URI target = java.net.URI.create(targetUrl);
            java.net.URI current = java.net.URI.create(currentUrl);
            String targetPath = normalizePath(target.getPath());
            String currentPath = normalizePath(current.getPath());
            if (targetPath.isEmpty()) {
                return false;
            }
            return !currentPath.equals(targetPath) && !currentPath.startsWith(targetPath);
        } catch (Exception e) {
            return !currentUrl.startsWith(targetUrl);
        }
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank() || "/".equals(path)) {
            return "";
        }
        return path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }

    private void ensureOnLoginPage(Page page, String login) {
        if (page.locator("input[name='username']").count() > 0) {
            return;
        }
        StepResult<Void> validateGate = ensurePastValidateCaptcha(page, login);
        if (validateGate != null) {
            throw new RuntimeException(validateGate.getErrorMessage());
        }
        if (page.locator("input[name='username']").count() > 0) {
            return;
        }
        log.info("АВТООТВЕТ: {} -> форма входа не на экране (url={}), переход на login, пользователь: {}",
                getSiteName(), page.url(), login);
        super.safeNavigate(page, FLRU_LOGIN_URL);
        resumeNavigationAfterValidateCaptcha(page, FLRU_LOGIN_URL, login);
        StepResult<Void> gate = ensurePastValidateCaptcha(page, login);
        if (gate != null) {
            throw new RuntimeException(gate.getErrorMessage());
        }
        if (page.locator("input[name='username']").count() > 0) {
            return;
        }
        clickFlRuLoginEntryLink(page, login);
        page.waitForTimeout(800);
        if (page.locator("input[name='username']").count() == 0) {
            super.safeNavigate(page, FLRU_LOGIN_URL);
            resumeNavigationAfterValidateCaptcha(page, FLRU_LOGIN_URL, login);
            StepResult<Void> gate2 = ensurePastValidateCaptcha(page, login);
            if (gate2 != null) {
                throw new RuntimeException(gate2.getErrorMessage());
            }
        }
    }

    private void clickFlRuLoginEntryLink(Page page, String login) {
        String[] selectors = {
                "a[href*='/account/login']",
                "a:has-text('Вход')",
                "a:has-text('Войти')",
                ".header-login a",
                "header a[href*='login']"
        };
        for (String selector : selectors) {
            Locator loc = page.locator(selector);
            if (loc.count() == 0) {
                continue;
            }
            try {
                Locator first = loc.first();
                if (first.isVisible()) {
                    first.click(new Locator.ClickOptions().setTimeout(5000));
                    log.info("АВТООТВЕТ: {} -> нажата ссылка входа ({}), пользователь: {}",
                            getSiteName(), selector, login);
                    page.waitForTimeout(1200);
                    return;
                }
            } catch (Exception e) {
                log.debug("АВТООТВЕТ: {} -> клик входа '{}' не удался: {}", getSiteName(), selector, e.getMessage());
            }
        }
    }

    @Override
    protected StepResult<Void> login(Page page, AiNotificationPayload payload, DecryptedCredential creds, AutoreplyMode mode) {
        log.info("АВТООТВЕТ: {} -> НАЧАЛО ЛОГИНА, пользователь: {}", getSiteName(), creds.login());

        try {
            safeNavigate(page, FLRU_LOGIN_URL);
            ensureOnLoginPage(page, creds.login());
            log.info("АВТООТВЕТ: {} -> страница логина, url={}, пользователь: {}",
                    getSiteName(), page.url(), creds.login());

            humanWarmup(page);

            if (!waitOrFail(page, "input[name='username']", 8000, "Поле логина")) {
                ensureOnLoginPage(page, creds.login());
                if (!waitOrFail(page, "input[name='username']", 8000, "Поле логина")) {
                    report(Level.WARN, log,
                            "АВТООТВЕТ: " + getSiteName() + " -> НЕ НАЙДЕНО ПОЛЕ ЛОГИНА, пользователь: " + creds.login(),
                            AutoreplyErrorTypes.FIELD_NOT_FOUND);
                    return StepResult.fail(StepType.SEND_AUTOREPLY, "Поле логина не найдено", captureScreenshot(page));
                }
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

            if (!clickFlRuLoginButton(page, creds.login())) {
                report(Level.WARN, log,
                        "АВТООТВЕТ: " + getSiteName() + " -> НЕ НАЙДЕНА КНОПКА 'Войти', пользователь: " + creds.login(),
                        AutoreplyErrorTypes.BUTTON_NOT_FOUND);
                return StepResult.fail(StepType.SEND_AUTOREPLY, "Кнопка 'Войти' не найдена", captureScreenshot(page));
            }

            if (waitForLoginError(page, 3000)) {
                report(Level.WARN, log,
                        "АВТООТВЕТ: " + getSiteName() + " -> НЕВЕРНЫЙ ЛОГИН/ПАРОЛЬ, пользователь: " + creds.login(),
                        AutoreplyErrorTypes.LOGIN_FAILED);
                return StepResult.fail(StepType.SEND_AUTOREPLY, "Неверный логин/пароль", captureScreenshot(page));
            }

            StepResult<Void> afterSubmitGate = ensurePastValidateCaptcha(page, creds.login());
            if (afterSubmitGate != null) {
                return afterSubmitGate;
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

    /**
     * FL.ru /validate-captcha — блокировка по IP («подозрительная активность», «Я человек»).
     *
     * @return {@code null} если страницы нет или успешно прошли; иначе ошибка шага
     */
    private StepResult<Void> ensurePastValidateCaptcha(Page page, String login) {
        if (!isValidateCaptchaPage(page)) {
            return null;
        }
        for (int attempt = 1; attempt <= 3; attempt++) {
            if (!isValidateCaptchaPage(page)) {
                return null;
            }
            log.info("АВТООТВЕТ: {} -> validate-captcha / IP-блок (попытка {}), пользователь: {}",
                    getSiteName(), attempt, login);
            captchaService.logValidateCaptchaState(page, "ensure-attempt-" + attempt + "-before");
            if (!captchaService.solveFlRuValidateCaptcha(page)) {
                captchaService.logValidateCaptchaState(page, "ensure-attempt-" + attempt + "-failed");
                report(Level.WARN, log,
                        "АВТООТВЕТ: " + getSiteName() + " -> validate-captcha: IP-капча не пройдена, пользователь: " + login,
                        AutoreplyErrorTypes.CAPTCHA_FAILED);
                return StepResult.fail(StepType.SEND_AUTOREPLY,
                        "FL.ru validate-captcha: не пройдена hCaptcha «подозрительная активность»", captureScreenshot(page));
            }
            captchaService.dismissSmartCaptchaOverlay(page);
            if (isValidateCaptchaPage(page)) {
                submitFlRuLoginForm(page, login);
                clickValidateCaptchaContinue(page, login);
            }
            waitUntilLeftValidateCaptcha(page, 15_000);
            page.waitForTimeout(800);
            captchaService.logValidateCaptchaState(page, "ensure-attempt-" + attempt + "-after-wait");
            if (!isValidateCaptchaPage(page)) {
                log.info("АВТООТВЕТ: {} -> validate-captcha снята после попытки {}, url={}, пользователь: {}",
                        getSiteName(), attempt, page.url(), login);
                return null;
            }
        }
        if (isValidateCaptchaPage(page)) {
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> остались на validate-captcha, пользователь: " + login,
                    AutoreplyErrorTypes.CAPTCHA_FAILED);
            return StepResult.fail(StepType.SEND_AUTOREPLY,
                    "FL.ru validate-captcha: не удалось продолжить после капчи", captureScreenshot(page));
        }
        log.info("АВТООТВЕТ: {} -> validate-captcha пройдена, url={}, пользователь: {}",
                getSiteName(), page.url(), login);
        return null;
    }

    private boolean isValidateCaptchaPage(Page page) {
        try {
            String url = page.url();
            if (url != null && url.contains(VALIDATE_CAPTCHA_PATH)) {
                return true;
            }
            for (String marker : VALIDATE_CAPTCHA_MARKERS) {
                Locator loc = page.locator(marker);
                if (loc.count() > 0 && loc.first().isVisible()) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.debug("АВТООТВЕТ: {} -> проверка validate-captcha: {}", getSiteName(), e.getMessage());
        }
        return false;
    }

    private void clickValidateCaptchaContinue(Page page, String login) {
        String[] selectors = {
                "button[type='submit']",
                "input[type='submit']",
                "button:has-text('Продолжить')",
                "button:has-text('Отправить')",
                "a.btn:has-text('Продолжить')"
        };
        for (String selector : selectors) {
            Locator loc = page.locator(selector);
            if (loc.count() == 0) {
                continue;
            }
            try {
                if (loc.first().isVisible() && !loc.first().isDisabled()) {
                    loc.first().click(new Locator.ClickOptions().setTimeout(5000));
                    log.info("АВТООТВЕТ: {} -> validate-captcha: нажато «{}», пользователь: {}",
                            getSiteName(), selector, login);
                    return;
                }
            } catch (Exception e) {
                log.debug("АВТООТВЕТ: {} -> validate-captcha клик '{}': {}", getSiteName(), selector, e.getMessage());
            }
        }
    }

    private void waitUntilLeftValidateCaptcha(Page page, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (!isValidateCaptchaPage(page)) {
                return;
            }
            page.waitForTimeout(400);
        }
        try {
            page.waitForURL(url -> url != null && !url.contains(VALIDATE_CAPTCHA_PATH),
                    new Page.WaitForURLOptions().setTimeout(3000));
        } catch (Exception ignored) {
            // проверка isValidateCaptchaPage ниже по циклу ensurePastValidateCaptcha
        }
    }

    private boolean clickFlRuLoginButton(Page page, String login) {
        captchaService.dismissSmartCaptchaOverlay(page);
        if (captchaService.isSmartCaptchaOverlayVisible(page)) {
            log.warn("АВТООТВЕТ: {} -> SmartCaptcha advanced-оверлей перекрывает форму, отправляем без клика, пользователь: {}",
                    getSiteName(), login);
        }
        if (submitFlRuLoginForm(page, login)) {
            return true;
        }

        String[] selectors = {
                "#submit-button",
                "button[type='submit']",
                "form button:has-text('Войти')",
                "input[type='submit']"
        };
        long deadline = System.currentTimeMillis() + 20_000;
        while (System.currentTimeMillis() < deadline) {
            for (String selector : selectors) {
                Locator loc = page.locator(selector);
                if (loc.count() == 0) {
                    continue;
                }
                Locator first = loc.first();
                try {
                    if (!first.isVisible()) {
                        continue;
                    }
                    if (first.isDisabled()) {
                        continue;
                    }
                    getCurrentManager().humanDelay(page);
                    first.click(new Locator.ClickOptions().setTimeout(5000));
                    log.info("АВТООТВЕТ: {} -> кнопка 'Войти' нажата ({}), пользователь: {}", getSiteName(), selector, login);
                    return true;
                } catch (Exception e) {
                    log.debug("АВТООТВЕТ: {} -> клик '{}' не удался: {}", getSiteName(), selector, e.getMessage());
                }
            }
            page.waitForTimeout(400);
        }
        captchaService.dismissSmartCaptchaOverlay(page);
        for (String selector : selectors) {
            Locator loc = page.locator(selector);
            if (loc.count() == 0) {
                continue;
            }
            try {
                loc.first().click(new Locator.ClickOptions().setTimeout(5000).setForce(true));
                log.info("АВТООТВЕТ: {} -> кнопка 'Войти' нажата force ({}), пользователь: {}", getSiteName(), selector, login);
                return true;
            } catch (Exception e) {
                log.debug("АВТООТВЕТ: {} -> force-клик '{}' не удался: {}", getSiteName(), selector, e.getMessage());
            }
        }
        if (submitFlRuLoginForm(page, login)) {
            return true;
        }
        log.warn("CLICK FAILED at step 'Кнопка Войти': ни один селектор не сработал, пользователь: {}", login);
        return false;
    }

    private boolean submitFlRuLoginForm(Page page, String login) {
        try {
            Object submitted = page.evaluate(
                    """
                            () => {
                              const form = document.querySelector('form');
                              if (!form) return false;
                              const btn = document.querySelector('#submit-button, button[type="submit"]');
                              if (typeof form.requestSubmit === 'function') {
                                if (btn) form.requestSubmit(btn);
                                else form.requestSubmit();
                                return true;
                              }
                              form.submit();
                              return true;
                            }
                            """);
            if (Boolean.TRUE.equals(submitted)) {
                log.info("АВТООТВЕТ: {} -> форма логина отправлена (requestSubmit/submit), пользователь: {}", getSiteName(), login);
                return true;
            }
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> submit формы не удался: {}", getSiteName(), e.getMessage());
        }
        return false;
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
        StepResult<Void> validateGate = ensurePastValidateCaptcha(page, creds.login());
        if (validateGate != null) {
            return validateGate;
        }
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
            safeNavigate(page, link);
            StepResult<Void> validateGate = ensurePastValidateCaptcha(page, login);
            if (validateGate != null) {
                return validateGate;
            }
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