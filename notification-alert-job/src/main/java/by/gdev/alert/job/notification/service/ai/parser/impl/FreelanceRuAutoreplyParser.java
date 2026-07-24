package by.gdev.alert.job.notification.service.ai.parser.impl;

import by.gdev.alert.job.notification.model.dto.AiNotificationPayload;
import by.gdev.alert.job.notification.model.dto.DecryptedCredential;
import by.gdev.alert.job.notification.service.ai.parser.AutoreplyPlaywrightParser;
import by.gdev.alert.job.notification.service.ai.proxy.AssignedProxyService;
import by.gdev.common.model.SiteName;
import by.gdev.common.service.playwright.PlaywrightManager;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FreelanceRuAutoreplyParser extends AutoreplyParser implements AutoreplyPlaywrightParser {

    @Value("${parser.autoreply.headless.freelance.ru:true}")
    private void setHeadless(boolean headless) {
        this.headless = headless;
    }

    @Value("${parser.autoreply.proxy.freelance.ru:false}")
    private void setProxy(boolean proxy) {
        this.proxy = proxy;
    }

    @Value("${parser.autoreply.send.request.freelance.ru:true}")
    private void setOnSendRequest(boolean sendRequest) {
        this.sendRequest = sendRequest;
    }

    public FreelanceRuAutoreplyParser(PlaywrightManager playwrightManager, AssignedProxyService assignedProxyService) {
        super(playwrightManager, assignedProxyService);
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

        // Кнопка Вход
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
        String link = payload.getOrder().getLink();
        log.debug("Переход на заказ: {}", link);

        // Переходим на заказ
        try {
            page.navigate(link);
            page.waitForLoadState(LoadState.NETWORKIDLE);
        } catch (Exception e) {
            log.warn("Не удалось открыть заказ {}", link);
            return false;
        }

        // Кнопка "Откликнуться"
        if (!clickOrFail(page,
                "button.btn.btn--success.btn--lg.btn--block:has-text('Откликнуться')",
                8000,
                "Кнопка 'Откликнуться'"))
            return false;

        // Ждём textarea
        if (!waitOrFail(page,
                "textarea#replyText[name='TaskReply[text]']",
                8000,
                "Поле ответа"))
            return false;

        // Вставляем текст автоответа
        try {
            page.fill("textarea#replyText[name='TaskReply[text]']", payload.getDecision().reply());
        } catch (Exception e) {
            log.warn("Не удалось заполнить текст ответа");
            return false;
        }

        // Ждём кнопку отправки
        if (!waitOrFail(page,
                "button#createReply.btn.btn--success.btn--sm",
                8000,
                "Кнопка отправки"))
            return false;

        Locator sendBtn = page.locator("button#createReply.btn.btn--success.btn--sm");

        // Проверяем, что кнопка активна
        try {
            page.waitForCondition(sendBtn::isEnabled,
                    new Page.WaitForConditionOptions().setTimeout(5000));
        } catch (Exception e) {
            log.warn("Кнопка отправки не активна");
            return false;
        }

        // Отправляем отклик
        if (sendRequest) {
            try {
                sendBtn.click();
            } catch (Exception e) {
                log.warn("Не удалось нажать кнопку отправки");
                return false;
            }
        }

        page.waitForTimeout(2000);
        log.debug("Отклик успешно отправлен на Freelance.ru");
        return true;
    }


}
