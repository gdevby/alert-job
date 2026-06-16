package by.gdev.alert.job.notification.service.ai.parser.impl;

import by.gdev.alert.job.notification.model.dto.AiNotificationPayload;
import by.gdev.alert.job.notification.model.dto.DecryptedCredential;
import by.gdev.alert.job.notification.service.ai.parser.AutoreplyPlaywrightParser;
import by.gdev.common.model.SiteName;
import by.gdev.common.service.playwright.CaptchaService;
import by.gdev.common.service.playwright.PlaywrightManager;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FlRuAutoreplyParser extends AutoreplyParser implements AutoreplyPlaywrightParser {

    private final CaptchaService captchaService;

    @Value("${parser.autoreply.headless.fl.ru}")
    private void setHeadless(boolean headless) {
        this.headless = headless;
    }

    @Value("${parser.autoreply.proxy.fl.ru}")
    private void setProxy(boolean proxy) {
        this.proxy = proxy;
    }

    @Value("${parser.autoreply.send.request.fl.ru}")
    private void setOnSendRequest(boolean sendRequest) {
        this.sendRequest = sendRequest;
    }

    public FlRuAutoreplyParser(PlaywrightManager playwrightManager, CaptchaService captchaService) {
        super(playwrightManager);
        this.captchaService = captchaService;
    }

    @Override
    protected boolean login(Page page, DecryptedCredential creds) {
        try {
            // Открываем страницу логина
            safeNavigate(page, "https://www.fl.ru/account/login/");
            // Ждём поле логина
            if (!waitOrFail(page, "input[name='username']", 8000, "Поле логина"))
                return false;
            // Вводим логин
            try {
                page.fill("input[name='username']", creds.login());
            } catch (Exception e) {
                log.warn("Не удалось заполнить логин");
                return false;
            }
            // Вводим пароль
            try {
                page.fill("input[name='password']", creds.password());
            } catch (Exception e) {
                log.warn("Не удалось заполнить пароль");
                return false;
            }
            // Пытаемся пройти SmartCaptcha
            if (!captchaService.solveYandexSmartCaptcha(page)) {
                log.warn("{}: SmartCaptcha не пройдена", getSiteName());
                return false;
            }
            // Жмём кнопку Войти
            if (!clickOrFail(page, "#submit-button", 8000, "Кнопка 'Войти'"))
                return false;
            // Ждём загрузку
            try {
                page.waitForLoadState(LoadState.NETWORKIDLE);
            } catch (Exception e) {
                log.warn("Не удалось дождаться загрузки после входа");
                return false;
            }
            // Проверяем, что логин успешный
            if (page.url().contains("/account/login")) {
                log.warn("{}: вход не выполнен, остались на странице логина", getSiteName());
                return false;
            }
            log.debug("{}: успешный вход в аккаунт {}", getSiteName(), creds.login());
            return true;
        } catch (Exception e) {
            log.error("{}: ошибка при логине", getSiteName(), e);
            return false;
        }
    }

    @Override
    protected boolean processAutoReply(Page page, AiNotificationPayload payload) {
        return false;
    }


    @Override
    public SiteName getSiteName() {
        return SiteName.FLRU;
    }
}
