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

@Slf4j
@Component
public class KworkAutoreplyParser extends AutoreplyParser implements AutoreplyPlaywrightParser {
    @Value("${parser.autoreply.headless.kwork.ru:true}")
    private void setHeadless(boolean headless) {
        this.headless = headless;
    }

    @Value("${parser.autoreply.proxy.kwork.ru:false}")
    private void setProxy(boolean proxy) {
        this.proxy = proxy;
    }

    @Value("${parser.autoreply.send.request.kwork.ru:true}")
    private void setOnSendRequest(boolean sendRequest) {
        this.sendRequest = sendRequest;
    }

    public KworkAutoreplyParser(PlaywrightManager playwrightManager, AssignedProxyService assignedProxyService) {
        super(playwrightManager, assignedProxyService);
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.KWORK;
    }

    @Override
    protected boolean login(Page page, DecryptedCredential creds) {
        log.info("АВТООТВЕТ: {} -> НАЧАЛО ЛОГИНА, пользователь: {}", getSiteName(), creds.login());

        page.navigate("https://kwork.ru/login");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        log.info("АВТООТВЕТ: {} -> страница логина загружена, пользователь: {}", getSiteName(), creds.login());

        page.waitForSelector("input[placeholder='Электронная почта или логин']");
        page.fill("input[placeholder='Электронная почта или логин']", creds.login());
        log.info("АВТООТВЕТ: {} -> логин заполнен: {}", getSiteName(), creds.login());

        page.fill("input[placeholder='Пароль']", creds.password());
        log.info("АВТООТВЕТ: {} -> пароль заполнен для пользователя: {}", getSiteName(), creds.login());

        Locator loginBtn = page.locator("button.auth-form__button");
        page.waitForCondition(loginBtn::isEnabled);
        loginBtn.click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
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

        if (!clickOrFail(page, "span.projects-offer-btn:has-text('Предложить услугу')",
                8000, "Открыть форму отклика")) {
            log.warn("АВТООТВЕТ: {} -> НЕ НАЙДЕНА КНОПКА 'Предложить услугу', пользователь: {}", getSiteName(), login);
            return false;
        }
        log.info("АВТООТВЕТ: {} -> кнопка 'Предложить услугу' нажата, пользователь: {}", getSiteName(), login);

        if (!waitOrFail(page, "div.trumbowyg-editor", 8000, "Редактор ответа")) {
            log.warn("АВТООТВЕТ: {} -> НЕ НАЙДЕН РЕДАКТОР, пользователь: {}", getSiteName(), login);
            return false;
        }
        log.info("АВТООТВЕТ: {} -> редактор найден, пользователь: {}", getSiteName(), login);

        try {
            page.fill("div.trumbowyg-editor", payload.getDecision().reply());
            log.info("АВТООТВЕТ: {} -> текст ответа вставлен, длина: {}, пользователь: {}", getSiteName(),
                    payload.getDecision().reply() != null ? payload.getDecision().reply().length() : 0, login);
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ ЗАПОЛНИТЬ ТЕКСТ, пользователь: {}, ошибка: {}", getSiteName(), login, e.getMessage());
            return false;
        }

        try {
            page.fill("#offer-custom-price", String.valueOf(defaultPrice));
            log.info("АВТООТВЕТ: {} -> цена установлена: {}, пользователь: {}", getSiteName(), defaultPrice, login);
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ УСТАНОВИТЬ ЦЕНУ, пользователь: {}, ошибка: {}", getSiteName(), login, e.getMessage());
            return false;
        }

        if (!clickOrFail(page, "div.duration-select", 5000, "Открыть список сроков")) {
            log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ ОТКРЫТЬ СПИСОК СРОКОВ, пользователь: {}", getSiteName(), login);
            return false;
        }
        log.info("АВТООТВЕТ: {} -> список сроков открыт, пользователь: {}", getSiteName(), login);

        if (!waitOrFail(page, "ul.vs__dropdown-menu li", 5000, "Список сроков")) {
            log.warn("АВТООТВЕТ: {} -> НЕ НАЙДЕН СПИСОК СРОКОВ, пользователь: {}", getSiteName(), login);
            return false;
        }

        try {
            page.locator("ul.vs__dropdown-menu li").first().click();
            log.info("АВТООТВЕТ: {} -> срок выполнения выбран, пользователь: {}", getSiteName(), login);
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ ВЫБРАТЬ СРОК, пользователь: {}, ошибка: {}", getSiteName(), login, e.getMessage());
            return false;
        }

        if (!waitOrFail(page, "button.kw-button--green:has-text('Предложить')",
                8000, "Кнопка отправки")) {
            log.warn("АВТООТВЕТ: {} -> НЕ НАЙДЕНА КНОПКА 'Предложить', пользователь: {}", getSiteName(), login);
            return false;
        }
        log.info("АВТООТВЕТ: {} -> кнопка 'Предложить' найдена, пользователь: {}", getSiteName(), login);

        Locator sendBtn = page.locator("button.kw-button--green:has-text('Предложить')");
        try {
            page.waitForCondition(sendBtn::isEnabled, new Page.WaitForConditionOptions().setTimeout(5000));
            log.info("АВТООТВЕТ: {} -> кнопка 'Предложить' активна, пользователь: {}", getSiteName(), login);
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> КНОПКА 'Предложить' НЕАКТИВНА, пользователь: {}, ошибка: {}", getSiteName(), login, e.getMessage());
            return false;
        }

        if (sendRequest) {
            try {
                sendBtn.click();
                log.info("АВТООТВЕТ: {} -> ЗАЯВКА УСПЕШНО ОТПРАВЛЕНА, пользователь: {}", getSiteName(), login);
            } catch (Exception e) {
                log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ ОТПРАВИТЬ ЗАЯВКУ, пользователь: {}, ошибка: {}", getSiteName(), login, e.getMessage());
                return false;
            }
        } else {
            log.info("АВТООТВЕТ: {} -> ЗАЯВКА НЕ ОТПРАВЛЕНА (sendRequest=false), пользователь: {}", getSiteName(), login);
        }

        page.waitForTimeout(2000);
        return true;
    }
}