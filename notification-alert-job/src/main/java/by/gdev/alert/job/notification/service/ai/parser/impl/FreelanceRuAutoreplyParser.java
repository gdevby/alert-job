package by.gdev.alert.job.notification.service.ai.parser.impl;

import by.gdev.alert.job.notification.model.dto.AiNotificationPayload;
import by.gdev.alert.job.notification.model.dto.DecryptedCredential;
import by.gdev.alert.job.notification.service.ai.parser.AutoreplyPlaywrightParser;
import by.gdev.common.model.SiteName;
import by.gdev.common.service.playwright.PlaywrightManager;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FreelanceRuAutoreplyParser extends AutoreplyParser implements AutoreplyPlaywrightParser {

    @Value("${parser.autoreply.headless.freelance.ru}")
    private void setHeadless(boolean headless) {
        this.headless = headless;
    }

    @Value("${parser.autoreply.proxy.freelance.ru}")
    private void setProxy(boolean proxy) {
        this.proxy = proxy;
    }

    @Value("${parser.autoreply.send.request.freelance.ru}")
    private void setOnSendRequest(boolean sendRequest) {
        this.sendRequest = sendRequest;
    }

    public FreelanceRuAutoreplyParser(PlaywrightManager playwrightManager) {
        super(playwrightManager);
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.FREELANCERU;
    }

    @Override
    protected boolean login(Page page, DecryptedCredential creds) {
        // Открываем главную
        try {
            safeNavigate(page, "https://freelance.ru/");
        } catch (Exception e) {
            log.warn("Не удалось открыть главную страницу Freelance.ru");
            return false;
        }

        // Кнопка "Вход"
        if (!clickOrFail(page, "a[href='/auth/login']", 8000, "Кнопка 'Вход'"))
            return false;

        // Ждём форму
        if (!waitOrFail(page, "input[placeholder='логин или email']", 8000, "Поле логина"))
            return false;

        // Вводим логин
        try {
            page.fill("input[placeholder='логин или email']", creds.login());
        } catch (Exception e) {
            log.warn("Не удалось заполнить логин");
            return false;
        }

        // Вводим пароль
        try {
            page.fill("input[type='password']", creds.password());
        } catch (Exception e) {
            log.warn("Не удалось заполнить пароль");
            return false;
        }

        // Кнопка "Войти"
        if (!clickOrFail(page, "button:has-text('Войти')", 8000, "Кнопка 'Войти'"))
            return false;

        // Ждём загрузку
        try {
            page.waitForLoadState(LoadState.NETWORKIDLE);
        } catch (Exception e) {
            log.warn("Не удалось дождаться загрузки после входа");
            return false;
        }

        log.debug("Успешный вход в аккаунт {}", creds.login());
        return true;
    }


    @Override
    protected boolean processAutoReply(Page page, AiNotificationPayload payload) {
        return false;
    }

}
