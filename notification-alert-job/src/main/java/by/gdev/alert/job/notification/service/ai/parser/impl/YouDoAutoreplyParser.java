package by.gdev.alert.job.notification.service.ai.parser.impl;

import by.gdev.alert.job.notification.model.AutoreplyMode;
import by.gdev.alert.job.notification.model.dto.AiNotificationPayload;
import by.gdev.alert.job.notification.model.dto.DecryptedCredential;
import by.gdev.alert.job.notification.service.ai.merics.AutoreplyErrorTypes;
import by.gdev.alert.job.notification.service.ai.parser.AutoreplyPlaywrightParser;
import by.gdev.alert.job.notification.service.ai.otp.OtpService;
import by.gdev.alert.job.notification.service.ai.parser.YoudoTariffChecker;
import by.gdev.alert.job.notification.service.ai.proxy.AssignedProxyService;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepResult;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepType;
import by.gdev.common.model.SiteName;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.slf4j.event.Level;


@Slf4j
@Component
public class YouDoAutoreplyParser extends AutoreplyParser implements AutoreplyPlaywrightParser {

    private static final String[] LOGIN_BUTTON_SELECTORS = {
            "data-test=LoginButton",
            "[data-test='LoginButton']",
            "text=Войти"
    };

    private static final String[] LOGIN_EMAIL_BUTTON_SELECTORS = {
            "data-test=LoginWithEmailButton",
            "[data-test='LoginWithEmailButton']",
            "text=Войти через электронную почту"
    };

    private static final String[] EMAIL_ERROR_SELECTORS = {
            "text=Неправильный адрес электронной почты",
            "text=Неправильный адрес почты",
            "[class*='Tooltip_error']:has-text('Неправильный адрес')"
    };

    private static final String EMAIL_ERROR_MESSAGE = "Неправильный адрес электронной почты";

    private final OtpService otpService;

    private final YoudoTariffChecker youdoTariffChecker;

    @Value("${parser.autoreply.headless.youdo.com:true}")
    private void setHeadless(boolean headless) {
        this.headless = headless;
    }

    @Value("${parser.autoreply.proxy.youdo.com:false}")
    private void setProxy(boolean proxy) {
        this.proxy = proxy;
    }

    @Value("${parser.autoreply.send.request.youdo.com:true}")
    private void setOnSendRequest(boolean sendRequest) {
        this.sendRequest = sendRequest;
    }

    @Value("${credential.validation.otp.timeout.ms:120000}")
    private long otpValidationTimeoutMs;

    public YouDoAutoreplyParser(AssignedProxyService assignedProxyService,
                                OtpService otpService,
                                YoudoTariffChecker youdoTariffChecker) {
        super(assignedProxyService);
        this.otpService = otpService;
        this.youdoTariffChecker = youdoTariffChecker;
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.YOUDO;
    }

    @Override
    protected String sessionCookieCheckUrl() {
        return "https://youdo.com";
    }

    @Override
    protected StepResult<Void> login(Page page, AiNotificationPayload payload, DecryptedCredential creds, AutoreplyMode mode) {
        log.info("АВТООТВЕТ: {} -> НАЧАЛО ЛОГИНА, пользователь: {}", getSiteName(), creds.login());

        // Открыть главную страницу
        try {
            safeNavigate(page, "https://youdo.com/");
            log.info("АВТООТВЕТ: {} -> главная страница загружена, пользователь: {}", getSiteName(), creds.login());
        } catch (Exception e) {
            report(Level.WARN,
                    log,
                    String.format("АВТООТВЕТ: %s -> НЕ УДАЛОСЬ ОТКРЫТЬ ГЛАВНУЮ СТРАНИЦУ, пользователь: %s, ошибка: %s", getSiteName(), creds.login(), e.getMessage()), AutoreplyErrorTypes.ERROR_OPEN_PAGE);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Не удалось открыть главную страницу: " + e.getMessage(), captureScreenshot(page));
        }

        try {
            page.waitForLoadState(LoadState.NETWORKIDLE);
        } catch (Exception e) {
            log.debug("АВТООТВЕТ: {} -> NETWORKIDLE не достигнут, продолжаем, пользователь: {}", getSiteName(), creds.login());
        }

        dismissCookieBanner(page);

        if (!clickLoginButton(page)) {
            report(Level.WARN, log,
                    String.format("АВТООТВЕТ: %s -> НЕ НАЙДЕНА КНОПКА 'Войти', пользователь: %s",
                            getSiteName(), creds.login()),
                    AutoreplyErrorTypes.BUTTON_NOT_FOUND);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Кнопка 'Войти' не найдена", captureScreenshot(page));
        }
        log.info("АВТООТВЕТ: {} -> кнопка 'Войти' нажата, пользователь: {}", getSiteName(), creds.login());

        if (!waitForLoginModal(page, 10000)) {
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> НЕ НАЙДЕНА КНОПКА 'Войти через email', пользователь: " + creds.login(),
                    AutoreplyErrorTypes.BUTTON_NOT_FOUND);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Кнопка 'Войти через email' не найдена", captureScreenshot(page));
        }
        log.info("АВТООТВЕТ: {} -> кнопка 'Войти через email' появилась, пользователь: {}", getSiteName(), creds.login());

        if (!clickWithFallback(page, LOGIN_EMAIL_BUTTON_SELECTORS, 8000, "Кнопка 'Войти через email'")) {
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> НЕ УДАЛОСЬ НАЖАТЬ КНОПКУ 'Войти через email', пользователь: " + creds.login(),
                    AutoreplyErrorTypes.BUTTON_NOT_FOUND);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Не удалось нажать кнопку 'Войти через email'", captureScreenshot(page));
        }
        log.info("АВТООТВЕТ: {} -> кнопка 'Войти через email' нажата, пользователь: {}", getSiteName(), creds.login());

        // Ожидание поля ввода email
        if (!waitOrFail(page, "input[name='login']", 8000, "Поле email")) {
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> НЕ НАЙДЕНО ПОЛЕ EMAIL, пользователь: " + creds.login(),
                    AutoreplyErrorTypes.FIELD_NOT_FOUND);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Поле email не найдено", captureScreenshot(page));
        }

        // Заполнение email
        try {
            page.fill("input[name='login']", creds.login());
            log.info("АВТООТВЕТ: {} -> email заполнен: {}", getSiteName(), creds.login());
        } catch (Exception e) {
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> НЕ УДАЛОСЬ ЗАПОЛНИТЬ EMAIL, пользователь: "
                            + creds.login() + ", ошибка: " + e.getMessage(),
                    AutoreplyErrorTypes.FIELD_NOT_FOUND);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Не удалось заполнить email: " + e.getMessage(), captureScreenshot(page));
        }

        // Клик по кнопке "Далее"
        if (!clickOrFail(page, "button:has-text('Далее')", 8000, "Кнопка 'Далее'")) {
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> НЕ НАЙДЕНА КНОПКА 'Далее', пользователь: " + creds.login(),
                    AutoreplyErrorTypes.BUTTON_NOT_FOUND);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Кнопка 'Далее' не найдена", captureScreenshot(page));
        }
        log.info("АВТООТВЕТ: {} -> кнопка 'Далее' нажата, пользователь: {}", getSiteName(), creds.login());

        if (waitForEmailError(page, 5000)) {
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> НЕПРАВИЛЬНЫЙ АДРЕС ЭЛЕКТРОННОЙ ПОЧТЫ, пользователь: " + creds.login(),
                    AutoreplyErrorTypes.EMAIL_INVALID);
            return StepResult.fail(StepType.SEND_AUTOREPLY, EMAIL_ERROR_MESSAGE, captureScreenshot(page));
        }

        // Ожидание поля ввода OTP
        if (!waitOrFail(page, "input[name='code']", 15000, "Поле ввода кода")) {
            if (isEmailErrorPresent(page)) {
                report(Level.WARN, log,
                        "АВТООТВЕТ: " + getSiteName() + " -> НЕПРАВИЛЬНЫЙ АДРЕС ЭЛЕКТРОННОЙ ПОЧТЫ, пользователь: " + creds.login(),
                        AutoreplyErrorTypes.EMAIL_INVALID);
                return StepResult.fail(StepType.SEND_AUTOREPLY, EMAIL_ERROR_MESSAGE, captureScreenshot(page));
            }
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> НЕ НАЙДЕНО ПОЛЕ ВВОДА КОДА, пользователь: " + creds.login(),
                    AutoreplyErrorTypes.FIELD_NOT_FOUND);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Поле ввода кода не найдено", captureScreenshot(page));
        }

        // Получение OTP
        log.info("АВТООТВЕТ: {} -> ожидание OTP для {}", getSiteName(), creds.login());
        String otp = otpService.waitForOtp(SiteName.YOUDO.name(), creds.login(), otpValidationTimeoutMs);
        if (otp == null) {
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> OTP НЕ ПОЛУЧЕН за отведённое время, пользователь: " + creds.login(),
                    AutoreplyErrorTypes.OTP_NOT_RECEIVED);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "OTP не получен за отведённое время", captureScreenshot(page));
        }
        log.info("АВТООТВЕТ: {} -> OTP получен для пользователя: {}", getSiteName(), creds.login());
        setOtp(payload, otp, true);

        // Заполнение поля OTP
        try {
            page.fill("input[name='code']", otp);
            log.info("АВТООТВЕТ: {} -> OTP заполнен для пользователя: {}", getSiteName(), creds.login());
        } catch (Exception e) {
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> НЕ УДАЛОСЬ ЗАПОЛНИТЬ OTP, пользователь: "
                            + creds.login() + ", ошибка: " + e.getMessage(),
                    AutoreplyErrorTypes.FIELD_NOT_FOUND);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Не удалось заполнить OTP: " + e.getMessage(), captureScreenshot(page));
        }

        // Ожидание загрузки страницы после ввода OTP
        try {
            page.waitForLoadState(LoadState.NETWORKIDLE);
            log.info("АВТООТВЕТ: {} -> страница загружена после ввода OTP, пользователь: {}", getSiteName(), creds.login());
        } catch (Exception e) {
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> НЕ УДАЛОСЬ ДОЖДАТЬСЯ ЗАГРУЗКИ ПОСЛЕ OTP, пользователь: "
                            + creds.login() + ", ошибка: " + e.getMessage(),
                    AutoreplyErrorTypes.TIMEOUT);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Не удалось дождаться загрузки после OTP: " + e.getMessage(), captureScreenshot(page));
        }

        log.info("АВТООТВЕТ: {} -> ЛОГИН УСПЕШЕН, пользователь: {}", getSiteName(), creds.login());
        otpService.invalidateOtp(SiteName.YOUDO.name(), creds.login());
        log.info("АВТООТВЕТ: {} -> OTP инвалидирован для {}", getSiteName(), creds.login());
        return StepResult.ok(StepType.SEND_AUTOREPLY, null);
    }

    private void dismissCookieBanner(Page page) {
        try {
            Locator cookieBtn = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Хорошо"));
            cookieBtn.waitFor(new Locator.WaitForOptions().setTimeout(3000));
            cookieBtn.click();
            log.info("АВТООТВЕТ: {} -> cookie-баннер закрыт", getSiteName());
        } catch (Exception e) {
            log.debug("АВТООТВЕТ: {} -> cookie-баннер не найден или уже закрыт", getSiteName());
        }
    }

    private boolean clickLoginButton(Page page) {
        for (String selector : LOGIN_BUTTON_SELECTORS) {
            try {
                Locator locator = page.locator(selector).first();
                locator.waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(8000));
                locator.scrollIntoViewIfNeeded();
                clickLocator(locator);
                log.info("АВТООТВЕТ: {} -> кнопка 'Войти' нажата (селектор: {})", getSiteName(), selector);
                if (waitForLoginModal(page, 5000)) {
                    return true;
                }
                log.debug("АВТООТВЕТ: {} -> модалка логина не открылась после клика '{}'", getSiteName(), selector);
            } catch (Exception e) {
                log.debug("АВТООТВЕТ: {} -> кнопка 'Войти' не найдена через '{}': {}", getSiteName(), selector, e.getMessage());
            }
        }
        return false;
    }

    private boolean waitForLoginModal(Page page, int timeoutMs) {
        for (String selector : LOGIN_EMAIL_BUTTON_SELECTORS) {
            if (waitOrFail(page, selector, timeoutMs, "Модалка логина")) {
                return true;
            }
        }
        return false;
    }

    private boolean clickWithFallback(Page page, String[] selectors, int timeoutMs, String step) {
        for (String selector : selectors) {
            try {
                Locator locator = page.locator(selector).first();
                locator.waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(timeoutMs));
                locator.scrollIntoViewIfNeeded();
                clickLocator(locator);
                log.info("АВТООТВЕТ: {} -> {} (селектор: {})", getSiteName(), step, selector);
                return true;
            } catch (Exception e) {
                log.debug("АВТООТВЕТ: {} -> {} не удалось через '{}': {}", getSiteName(), step, selector, e.getMessage());
            }
        }
        return false;
    }

    private void clickLocator(Locator locator) {
        try {
            locator.click();
        } catch (Exception e) {
            locator.click(new Locator.ClickOptions().setForce(true));
        }
    }

    private boolean waitForEmailError(Page page, int timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (isEmailErrorPresent(page)) {
                return true;
            }
            page.waitForTimeout(300);
        }
        return isEmailErrorPresent(page);
    }

    private boolean isEmailErrorPresent(Page page) {
        for (String selector : EMAIL_ERROR_SELECTORS) {
            try {
                Locator locator = page.locator(selector).first();
                if (locator.isVisible()) {
                    log.debug("АВТООТВЕТ: {} -> ошибка email обнаружена (селектор: {})", getSiteName(), selector);
                    return true;
                }
            } catch (Exception e) {
                log.debug("АВТООТВЕТ: {} -> проверка ошибки email через '{}' не удалась: {}", getSiteName(), selector, e.getMessage());
            }
        }
        try {
            Locator partialMatch = page.getByText("Неправильный адрес", new Page.GetByTextOptions().setExact(false)).first();
            if (partialMatch.isVisible()) {
                log.debug("АВТООТВЕТ: {} -> ошибка email обнаружена (getByText частичное совпадение)", getSiteName());
                return true;
            }
        } catch (Exception e) {
            log.debug("АВТООТВЕТ: {} -> getByText проверка ошибки email не удалась: {}", getSiteName(), e.getMessage());
        }
        return false;
    }

    @Override
    protected StepResult<Void> processAutoReply(Page page, AiNotificationPayload payload, DecryptedCredential creds) {
        String link = payload.getOrder().getLink();
        String login = creds.login();
        log.info("АВТООТВЕТ: {} -> НАЧАЛО ОБРАБОТКИ ЗАКАЗА: {}, пользователь: {}", getSiteName(), link, login);
        try {
            StepResult<Void> tariffResult = checkTariff(page, login);
            if (tariffResult.failed()) {
                return tariffResult; // возвращаем ошибку, если профиль бесплатный или откликов недостаточно
            }
            log.info("АВТООТВЕТ: {} -> тариф активен, продолжаем обработку заказа", getSiteName());
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

        if (!clickOrFail(page, "button:has-text('Откликнуться')", 8000, "Кнопка 'Откликнуться'")) {
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> НЕ НАЙДЕНА КНОПКА 'Откликнуться', пользователь: " + login,
                    AutoreplyErrorTypes.BUTTON_NOT_FOUND);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Кнопка 'Откликнуться' не найдена", captureScreenshot(page));
        }
        log.info("АВТООТВЕТ: {} -> кнопка 'Откликнуться' нажата, пользователь: {}", getSiteName(), login);

        if (!waitOrFail(page, "input[placeholder='В рублях']", 8000, "Поле цены")) {
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> НЕ НАЙДЕНО ПОЛЕ ЦЕНЫ, пользователь: " + login,
                    AutoreplyErrorTypes.FIELD_NOT_FOUND);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Поле цены не найдено", captureScreenshot(page));
        }

        try {
            page.fill("input[placeholder='В рублях']", String.valueOf(defaultPrice));
            log.info("АВТООТВЕТ: {} -> цена установлена: {}, пользователь: {}", getSiteName(), defaultPrice, login);
        } catch (Exception e) {
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> НЕ УДАЛОСЬ ЗАПОЛНИТЬ ЦЕНУ, пользователь: "
                            + login + ", ошибка: " + e.getMessage(),
                    AutoreplyErrorTypes.FIELD_NOT_FOUND);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Не удалось заполнить цену: " + e.getMessage(), captureScreenshot(page));
        }

        if (!waitOrFail(page, "textarea.Textarea_textarea__FjgmX", 8000, "Поле текста отклика")) {
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> НЕ НАЙДЕНО ПОЛЕ ТЕКСТА ОТКЛИКА, пользователь: " + login,
                    AutoreplyErrorTypes.FIELD_NOT_FOUND);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Поле текста отклика не найдено", captureScreenshot(page));
        }

        try {
            String replyText = payload.getDecision().reply();
            page.fill("textarea.Textarea_textarea__FjgmX", replyText);
            log.info("АВТООТВЕТ: {} -> текст ответа вставлен, длина: {}, пользователь: {}", getSiteName(),
                    replyText != null ? replyText.length() : 0, login);
        } catch (Exception e) {
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> НЕ УДАЛОСЬ ЗАПОЛНИТЬ ТЕКСТ ОТКЛИКА, пользователь: "
                            + login + ", ошибка: " + e.getMessage(),
                    AutoreplyErrorTypes.FIELD_NOT_FOUND);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Не удалось заполнить текст отклика: " + e.getMessage(), captureScreenshot(page));
        }

        if (!waitOrFail(page, "button.NewButton_button__2D_5n:has-text('Далее')", 8000, "Кнопка 'Далее'")) {
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> НЕ НАЙДЕНА КНОПКА 'Далее', пользователь: " + login,
                    AutoreplyErrorTypes.BUTTON_NOT_FOUND);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Кнопка 'Далее' не найдена", captureScreenshot(page));
        }
        log.info("АВТООТВЕТ: {} -> кнопка 'Далее' найдена, пользователь: {}", getSiteName(), login);

        Locator nextBtn = page.locator("button.NewButton_button__2D_5n:has-text('Далее')");
        try {
            page.waitForCondition(nextBtn::isEnabled, new Page.WaitForConditionOptions().setTimeout(5000));
            log.info("АВТООТВЕТ: {} -> кнопка 'Далее' активна, пользователь: {}", getSiteName(), login);
        } catch (Exception e) {
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> КНОПКА 'Далее' НЕАКТИВНА, пользователь: "
                            + login + ", ошибка: " + e.getMessage(),
                    AutoreplyErrorTypes.BUTTON_NOT_FOUND);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Кнопка 'Далее' неактивна", captureScreenshot(page));
        }

        if (sendRequest) {
            try {
                nextBtn.click();
                //log.info("АВТООТВЕТ: {} -> кнопка 'Далее' нажата, пользователь: {}", getSiteName(), login);
                log.info("АВТООТВЕТ: {} -> ЗАЯВКА УСПЕШНО ОТПРАВЛЕНА, пользователь: {}", getSiteName(), login);
            } catch (Exception e) {
                report(Level.WARN, log,
                        "АВТООТВЕТ: " + getSiteName() + " -> НЕ УДАЛОСЬ НАЖАТЬ КНОПКУ 'Далее', пользователь: "
                                + login + ", ошибка: " + e.getMessage(),
                        AutoreplyErrorTypes.BUTTON_NOT_FOUND);
                return StepResult.fail(StepType.SEND_AUTOREPLY, "Не удалось нажать кнопку 'Далее': " + e.getMessage(), captureScreenshot(page));
            }
        } else {
            log.info("АВТООТВЕТ: {} -> ЗАЯВКА НЕ ОТПРАВЛЕНА (sendRequest=false), пользователь: {}", getSiteName(), login);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Заявка не отправлена (sendRequest=false)", captureScreenshot(page));
        }

        page.waitForTimeout(3000);
        return StepResult.ok(StepType.SEND_AUTOREPLY, null);
    }

    private StepResult<Void> checkTariff(Page page, String login){
        page.navigate(getProfileUrl(page));
        page.waitForLoadState(LoadState.NETWORKIDLE);
        log.info("АВТООТВЕТ: {} -> страница профиля открыта, пользователь: {}", getSiteName(), login);
        boolean isTariffTab = youdoTariffChecker.isTariffsTabPresent(page);
        if (!isTariffTab) {
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> ПРОФИЛЬ БЕСПЛАТНЫЙ, пользователь: " + login,
                    AutoreplyErrorTypes.TARIFF_LIMIT);
            return StepResult.fail(StepType.SEND_AUTOREPLY,
                    "Профиль бесплатный. Отклик не удастся отправить ", captureScreenshot(page));
        }
        int remainingResponses = youdoTariffChecker.getRemainingResponses(page);
        if (remainingResponses > 0) {
            return StepResult.ok(StepType.SEND_AUTOREPLY, null);
        }
        report(Level.WARN, log,
                "АВТООТВЕТ: " + getSiteName() + " -> ОТКЛИКОВ МАЛО, пользователь: " + login,
                AutoreplyErrorTypes.TARIFF_LIMIT);
        return StepResult.fail(StepType.SEND_AUTOREPLY,
                "Количество откликов мало для отправки автоответа ", captureScreenshot(page));
    }

    private String getProfileUrl(Page page){
        // Находим элемент
        Locator avatarLink = page.locator("a.avatar_block__KOT6G.avatar_s32Square__FqL_i.js-toggleUserNavigationBtn");
        // Получаем значение атрибута href
        String href = avatarLink.getAttribute("href");
        // Формируем полный URL
        String profileUrl = "https://youdo.com" + href;
        log.info("Профиль пользователя: {}", profileUrl);
        return profileUrl;
    }
}