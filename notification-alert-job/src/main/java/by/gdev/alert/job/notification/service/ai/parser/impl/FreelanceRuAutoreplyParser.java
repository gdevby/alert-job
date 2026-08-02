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
        log.info("АВТООТВЕТ: {} -> НАЧАЛО ЛОГИНА, пользователь: {}", getSiteName(), creds.login());

        try {
            safeNavigate(page, "https://freelance.ru/");
            log.info("АВТООТВЕТ: {} -> главная страница загружена, пользователь: {}", getSiteName(), creds.login());
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ ОТКРЫТЬ ГЛАВНУЮ СТРАНИЦУ, пользователь: {}, ошибка: {}", getSiteName(), creds.login(), e.getMessage());
            return false;
        }

        if (!clickOrFail(page, "a[href='/auth/login']", 8000, "Кнопка 'Вход'")) {
            log.warn("АВТООТВЕТ: {} -> НЕ НАЙДЕНА КНОПКА 'Вход', пользователь: {}", getSiteName(), creds.login());
            return false;
        }
        log.info("АВТООТВЕТ: {} -> кнопка 'Вход' нажата, пользователь: {}", getSiteName(), creds.login());

        if (!waitOrFail(page, "input[placeholder='логин или email']", 8000, "Поле логина")) {
            log.warn("АВТООТВЕТ: {} -> НЕ НАЙДЕНО ПОЛЕ ЛОГИНА, пользователь: {}", getSiteName(), creds.login());
            return false;
        }

        try {
            page.fill("input[placeholder='логин или email']", creds.login());
            log.info("АВТООТВЕТ: {} -> логин заполнен: {}", getSiteName(), creds.login());
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ ЗАПОЛНИТЬ ЛОГИН, пользователь: {}, ошибка: {}", getSiteName(), creds.login(), e.getMessage());
            return false;
        }

        try {
            page.fill("input[type='password']", creds.password());
            log.info("АВТООТВЕТ: {} -> пароль заполнен для пользователя: {}", getSiteName(), creds.login());
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ ЗАПОЛНИТЬ ПАРОЛЬ, пользователь: {}, ошибка: {}", getSiteName(), creds.login(), e.getMessage());
            return false;
        }

        if (!clickOrFail(page, "button:has-text('Войти')", 8000, "Кнопка 'Войти'")) {
            log.warn("АВТООТВЕТ: {} -> НЕ НАЙДЕНА КНОПКА 'Войти', пользователь: {}", getSiteName(), creds.login());
            return false;
        }
        log.info("АВТООТВЕТ: {} -> кнопка 'Войти' нажата, пользователь: {}", getSiteName(), creds.login());

        try {
            page.waitForLoadState(LoadState.NETWORKIDLE);
            log.info("АВТООТВЕТ: {} -> страница загружена после входа, пользователь: {}", getSiteName(), creds.login());
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ ДОЖДАТЬСЯ ЗАГРУЗКИ ПОСЛЕ ВХОДА, пользователь: {}, ошибка: {}", getSiteName(), creds.login(), e.getMessage());
            return false;
        }

        log.info("АВТООТВЕТ: {} -> ЛОГИН УСПЕШЕН, пользователь: {}", getSiteName(), creds.login());
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

        if (!clickOrFail(page,
                "button.btn.btn--success.btn--lg.btn--block:has-text('Откликнуться')",
                8000,
                "Кнопка 'Откликнуться'")) {
            log.warn("АВТООТВЕТ: {} -> НЕ НАЙДЕНА КНОПКА 'Откликнуться', пользователь: {}", getSiteName(), login);
            return false;
        }
        log.info("АВТООТВЕТ: {} -> кнопка 'Откликнуться' нажата, пользователь: {}", getSiteName(), login);

        if (!waitOrFail(page,
                "textarea#replyText[name='TaskReply[text]']",
                8000,
                "Поле ответа")) {
            log.warn("АВТООТВЕТ: {} -> НЕ НАЙДЕНО ПОЛЕ ОТВЕТА, пользователь: {}", getSiteName(), login);
            return false;
        }

        try {
            page.fill("textarea#replyText[name='TaskReply[text]']", payload.getDecision().reply());
            log.info("АВТООТВЕТ: {} -> текст ответа вставлен, длина: {}, пользователь: {}", getSiteName(),
                    payload.getDecision().reply() != null ? payload.getDecision().reply().length() : 0, login);
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ ЗАПОЛНИТЬ ТЕКСТ ОТВЕТА, пользователь: {}, ошибка: {}", getSiteName(), login, e.getMessage());
            return false;
        }

        if (!waitOrFail(page,
                "button#createReply.btn.btn--success.btn--sm",
                8000,
                "Кнопка отправки")) {
            log.warn("АВТООТВЕТ: {} -> НЕ НАЙДЕНА КНОПКА ОТПРАВКИ, пользователь: {}", getSiteName(), login);
            return false;
        }
        log.info("АВТООТВЕТ: {} -> кнопка отправки найдена, пользователь: {}", getSiteName(), login);

        Locator sendBtn = page.locator("button#createReply.btn.btn--success.btn--sm");
        try {
            page.waitForCondition(sendBtn::isEnabled,
                    new Page.WaitForConditionOptions().setTimeout(5000));
            log.info("АВТООТВЕТ: {} -> кнопка отправки активна, пользователь: {}", getSiteName(), login);
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> КНОПКА ОТПРАВКИ НЕАКТИВНА, пользователь: {}, ошибка: {}", getSiteName(), login, e.getMessage());
            return false;
        }

        if (sendRequest) {
            try {
                sendBtn.click();
                log.info("АВТООТВЕТ: {} -> ЗАЯВКА УСПЕШНО ОТПРАВЛЕНА, пользователь: {}", getSiteName(), login);
            } catch (Exception e) {
                log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ НАЖАТЬ КНОПКУ ОТПРАВКИ, пользователь: {}, ошибка: {}", getSiteName(), login, e.getMessage());
                return false;
            }
        } else {
            log.info("АВТООТВЕТ: {} -> ЗАЯВКА НЕ ОТПРАВЛЕНА (sendRequest=false), пользователь: {}", getSiteName(), login);
        }

        page.waitForTimeout(2000);
        log.info("АВТООТВЕТ: {} -> ОТКЛИК УСПЕШНО ЗАВЕРШЁН, пользователь: {}", getSiteName(), login);
        return true;
    }
}