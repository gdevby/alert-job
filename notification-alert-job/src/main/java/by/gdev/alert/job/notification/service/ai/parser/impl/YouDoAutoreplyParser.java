package by.gdev.alert.job.notification.service.ai.parser.impl;

import by.gdev.alert.job.notification.model.dto.AiNotificationPayload;
import by.gdev.alert.job.notification.model.dto.DecryptedCredential;
import by.gdev.alert.job.notification.service.ai.parser.AutoreplyPlaywrightParser;
import by.gdev.alert.job.notification.service.ai.proxy.AssignedProxyService;
import by.gdev.common.model.SiteName;
import by.gdev.common.service.playwright.PlaywrightManager;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import by.gdev.alert.job.notification.service.ai.otp.OtpService;

@Slf4j
@Component
public class YouDoAutoreplyParser extends AutoreplyParser implements AutoreplyPlaywrightParser {

    private final OtpService otpService;

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

    public YouDoAutoreplyParser(PlaywrightManager playwrightManager, AssignedProxyService assignedProxyService, OtpService otpService) {
        super(playwrightManager, assignedProxyService);
        this.otpService = otpService;
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.YOUDO;
    }

    @Override
    protected boolean login(Page page, DecryptedCredential creds) {
        log.info("АВТООТВЕТ: {} -> НАЧАЛО ЛОГИНА, пользователь: {}", getSiteName(), creds.login());

        try {
            safeNavigate(page, "https://youdo.com/");
            log.info("АВТООТВЕТ: {} -> главная страница загружена, пользователь: {}", getSiteName(), creds.login());
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ ОТКРЫТЬ ГЛАВНУЮ СТРАНИЦУ, пользователь: {}, ошибка: {}", getSiteName(), creds.login(), e.getMessage());
            return false;
        }

        if (!clickOrFail(page, "span[data-test='LoginButton']", 8000, "Кнопка 'Войти'")) {
            log.warn("АВТООТВЕТ: {} -> НЕ НАЙДЕНА КНОПКА 'Войти', пользователь: {}", getSiteName(), creds.login());
            return false;
        }
        log.info("АВТООТВЕТ: {} -> кнопка 'Войти' нажата, пользователь: {}", getSiteName(), creds.login());

        if (!clickOrFail(page, "span[data-test='LoginWithEmailButton']", 8000, "Войти через email")) {
            log.warn("АВТООТВЕТ: {} -> НЕ НАЙДЕНА КНОПКА 'Войти через email', пользователь: {}", getSiteName(), creds.login());
            return false;
        }
        log.info("АВТООТВЕТ: {} -> кнопка 'Войти через email' нажата, пользователь: {}", getSiteName(), creds.login());

        if (!waitOrFail(page, "input[name='login']", 8000, "Поле email")) {
            log.warn("АВТООТВЕТ: {} -> НЕ НАЙДЕНО ПОЛЕ EMAIL, пользователь: {}", getSiteName(), creds.login());
            return false;
        }

        try {
            page.fill("input[name='login']", creds.login());
            log.info("АВТООТВЕТ: {} -> email заполнен: {}", getSiteName(), creds.login());
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ ЗАПОЛНИТЬ EMAIL, пользователь: {}, ошибка: {}", getSiteName(), creds.login(), e.getMessage());
            return false;
        }

        if (!clickOrFail(page, "button:has-text('Далее')", 8000, "Кнопка 'Далее'")) {
            log.warn("АВТООТВЕТ: {} -> НЕ НАЙДЕНА КНОПКА 'Далее', пользователь: {}", getSiteName(), creds.login());
            return false;
        }
        log.info("АВТООТВЕТ: {} -> кнопка 'Далее' нажата, пользователь: {}", getSiteName(), creds.login());

        if (!waitOrFail(page, "input[name='code']", 15000, "Поле ввода кода")) {
            log.warn("АВТООТВЕТ: {} -> НЕ НАЙДЕНО ПОЛЕ ВВОДА КОДА, пользователь: {}", getSiteName(), creds.login());
            return false;
        }

        log.info("АВТООТВЕТ: {} -> ожидание OTP для {}", getSiteName(), creds.login());
        String otp = otpService.waitForOtp(SiteName.YOUDO.name(), creds.login(), 120_000);
        if (otp == null) {
            log.warn("АВТООТВЕТ: {} -> OTP НЕ ПОЛУЧЕН за отведённое время, пользователь: {}", getSiteName(), creds.login());
            return false;
        }
        log.info("АВТООТВЕТ: {} -> OTP получен для пользователя: {}", getSiteName(), creds.login());

        try {
            page.fill("input[name='code']", otp);
            log.info("АВТООТВЕТ: {} -> OTP заполнен для пользователя: {}", getSiteName(), creds.login());
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ ЗАПОЛНИТЬ OTP, пользователь: {}, ошибка: {}", getSiteName(), creds.login(), e.getMessage());
            return false;
        }

        try {
            page.waitForLoadState(LoadState.NETWORKIDLE);
            log.info("АВТООТВЕТ: {} -> страница загружена после ввода OTP, пользователь: {}", getSiteName(), creds.login());
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ ДОЖДАТЬСЯ ЗАГРУЗКИ ПОСЛЕ OTP, пользователь: {}, ошибка: {}", getSiteName(), creds.login(), e.getMessage());
            return false;
        }

        log.info("АВТООТВЕТ: {} -> ЛОГИН УСПЕШЕН, пользователь: {}", getSiteName(), creds.login());
        page.waitForTimeout(30000);
        otpService.invalidateOtp(SiteName.YOUDO.name(), creds.login());
        log.debug("АВТООТВЕТ: {} -> OTP инвалидирован для {}", getSiteName(), creds.login());
        return true;
    }

    @Override
    protected boolean processAutoReply(Page page, AiNotificationPayload payload, DecryptedCredential creds) {
        String link = payload.getOrder().getLink();
        String login = creds.login();
        log.info("АВТООТВЕТ: {} -> НАЧАЛО ОБРАБОТКИ ЗАКАЗА: {}, пользователь: {}", getSiteName(), link, login);

        try {
            page.navigate(link);
            page.waitForLoadState(LoadState.NETWORKIDLE);
            log.info("АВТООТВЕТ: {} -> страница заказа открыта, пользователь: {}", getSiteName(), login);
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ ОТКРЫТЬ ЗАКАЗ, пользователь: {}, ошибка: {}", getSiteName(), login, e.getMessage());
            return false;
        }

        if (!clickOrFail(page, "button:has-text('Откликнуться')", 8000, "Кнопка 'Откликнуться'")) {
            log.warn("АВТООТВЕТ: {} -> НЕ НАЙДЕНА КНОПКА 'Откликнуться', пользователь: {}", getSiteName(), login);
            return false;
        }
        log.info("АВТООТВЕТ: {} -> кнопка 'Откликнуться' нажата, пользователь: {}", getSiteName(), login);

        if (!waitOrFail(page, "input[placeholder='В рублях']", 8000, "Поле цены")) {
            log.warn("АВТООТВЕТ: {} -> НЕ НАЙДЕНО ПОЛЕ ЦЕНЫ, пользователь: {}", getSiteName(), login);
            return false;
        }

        try {
            page.fill("input[placeholder='В рублях']", String.valueOf(defaultPrice));
            log.info("АВТООТВЕТ: {} -> цена установлена: {}, пользователь: {}", getSiteName(), defaultPrice, login);
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ ЗАПОЛНИТЬ ЦЕНУ, пользователь: {}, ошибка: {}", getSiteName(), login, e.getMessage());
            return false;
        }

        if (!waitOrFail(page, "textarea.Textarea_textarea__FjgmX", 8000, "Поле текста отклика")) {
            log.warn("АВТООТВЕТ: {} -> НЕ НАЙДЕНО ПОЛЕ ТЕКСТА ОТКЛИКА, пользователь: {}", getSiteName(), login);
            return false;
        }

        try {
            String replyText = payload.getDecision().reply();
            page.fill("textarea.Textarea_textarea__FjgmX", replyText);
            log.info("АВТООТВЕТ: {} -> текст ответа вставлен, длина: {}, пользователь: {}", getSiteName(),
                    replyText != null ? replyText.length() : 0, login);
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ ЗАПОЛНИТЬ ТЕКСТ ОТКЛИКА, пользователь: {}, ошибка: {}", getSiteName(), login, e.getMessage());
            return false;
        }

        if (!waitOrFail(page, "button.NewButton_button__2D_5n:has-text('Далее')", 8000, "Кнопка 'Далее'")) {
            log.warn("АВТООТВЕТ: {} -> НЕ НАЙДЕНА КНОПКА 'Далее', пользователь: {}", getSiteName(), login);
            return false;
        }
        log.info("АВТООТВЕТ: {} -> кнопка 'Далее' найдена, пользователь: {}", getSiteName(), login);

        Locator nextBtn = page.locator("button.NewButton_button__2D_5n:has-text('Далее')");
        try {
            page.waitForCondition(nextBtn::isEnabled, new Page.WaitForConditionOptions().setTimeout(5000));
            log.info("АВТООТВЕТ: {} -> кнопка 'Далее' активна, пользователь: {}", getSiteName(), login);
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> КНОПКА 'Далее' НЕАКТИВНА, пользователь: {}, ошибка: {}", getSiteName(), login, e.getMessage());
            return false;
        }

        if (sendRequest) {
            try {
                nextBtn.click();
                log.info("АВТООТВЕТ: {} -> кнопка 'Далее' нажата, пользователь: {}", getSiteName(), login);
            } catch (Exception e) {
                log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ НАЖАТЬ КНОПКУ 'Далее', пользователь: {}, ошибка: {}", getSiteName(), login, e.getMessage());
                return false;
            }
            log.info("АВТООТВЕТ: {} -> ЗАЯВКА УСПЕШНО ОТПРАВЛЕНА, пользователь: {}", getSiteName(), login);
        } else {
            log.info("АВТООТВЕТ: {} -> ЗАЯВКА НЕ ОТПРАВЛЕНА (sendRequest=false), пользователь: {}", getSiteName(), login);
        }

        page.waitForTimeout(3000);
        return true;
    }
}