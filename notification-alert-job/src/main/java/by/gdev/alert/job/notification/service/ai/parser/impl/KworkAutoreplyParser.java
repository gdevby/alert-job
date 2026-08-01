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
    protected boolean  login(Page page, DecryptedCredential creds) {
        log.info("АВТООТВЕТ: {} -> НАЧАЛО ЛОГИНА, пользователь: {}", getSiteName(), creds.login());
        page.navigate("https://kwork.ru/login");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        log.info("АВТООТВЕТ: {} -> страница логина загружена", getSiteName());
        // Логин
        page.waitForSelector("input[placeholder='Электронная почта или логин']");
        page.fill("input[placeholder='Электронная почта или логин']", creds.login());
        log.info("АВТООТВЕТ: {} -> логин заполнен", getSiteName());
        // Пароль
        page.fill("input[placeholder='Пароль']", creds.password());
        log.info("АВТООТВЕТ: {} -> пароль заполнен", getSiteName());
        // Кнопка "Войти"
        Locator loginBtn = page.locator("button.auth-form__button");
        page.waitForCondition(loginBtn::isEnabled);
        loginBtn.click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
        log.info("АВТООТВЕТ: {} -> ЛОГИН УСПЕШЕН", getSiteName());
        return true;
    }


    @Override
    protected boolean processAutoReply(Page page, AiNotificationPayload payload) {
        String link = payload.getOrder().getLink();
        log.info("АВТООТВЕТ: {} -> НАЧАЛО ОБРАБОТКИ ЗАКАЗА: {}", getSiteName(), link);
        log.info("Переход на заказ: {}", link);

        try {
            page.navigate(link);
            page.waitForLoadState(LoadState.NETWORKIDLE);
            log.info("АВТООТВЕТ: {} -> страница заказа открыта", getSiteName());
        } catch (Exception e) {
            log.warn("Не удалось открыть заказ {}", link);
            return false;
        }

        // Кнопка "Предложить услугу"
        if (!clickOrFail(page, "span.projects-offer-btn:has-text('Предложить услугу')",
                8000, "Открыть форму отклика")) {
            log.warn("АВТООТВЕТ: {} -> НЕ НАЙДЕНА КНОПКА 'Предложить услугу'", getSiteName());
            return false;
        }
        log.info("АВТООТВЕТ: {} -> кнопка 'Предложить услугу' нажата", getSiteName());

        // Ждём редактор
        if (!waitOrFail(page, "div.trumbowyg-editor", 8000, "Редактор ответа")) {
            log.warn("АВТООТВЕТ: {} -> НЕ НАЙДЕН РЕДАКТОР", getSiteName());
            return false;
        }
        log.info("АВТООТВЕТ: {} -> редактор найден", getSiteName());

        // Текст автоответа
        try {
            page.fill("div.trumbowyg-editor", payload.getDecision().reply());
            log.info("АВТООТВЕТ: {} -> текст ответа вставлен, длина: {}", getSiteName(),
                    payload.getDecision().reply() != null ? payload.getDecision().reply().length() : 0);
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ ЗАПОЛНИТЬ ТЕКСТ: {}", getSiteName(), e.getMessage());
            return false;
        }

        // Цена
        try {
            page.fill("#offer-custom-price", String.valueOf(defaultPrice));
            log.info("АВТООТВЕТ: {} -> цена установлена: {}", getSiteName(), defaultPrice);
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ УСТАНОВИТЬ ЦЕНУ: {}", getSiteName(), e.getMessage());
            return false;
        }

        // Срок выполнения
        if (!clickOrFail(page, "div.duration-select", 5000, "Открыть список сроков")) {
            log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ ОТКРЫТЬ СПИСОК СРОКОВ", getSiteName());
            return false;
        }
        log.info("АВТООТВЕТ: {} -> список сроков открыт", getSiteName());

        if (!waitOrFail(page, "ul.vs__dropdown-menu li", 5000, "Список сроков")) {
            log.warn("АВТООТВЕТ: {} -> НЕ НАЙДЕН СПИСОК СРОКОВ", getSiteName());
            return false;
        }

        try {
            page.locator("ul.vs__dropdown-menu li").first().click();
            log.info("АВТООТВЕТ: {} -> срок выполнения выбран", getSiteName());
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ ВЫБРАТЬ СРОК: {}", getSiteName(), e.getMessage());
            return false;
        }

        // Кнопка "Предложить"
        if (!waitOrFail(page, "button.kw-button--green:has-text('Предложить')",
                8000, "Кнопка отправки")) return false;
        log.info("АВТООТВЕТ: {} -> кнопка 'Предложить' найдена", getSiteName());

        Locator sendBtn = page.locator("button.kw-button--green:has-text('Предложить')");
        try {
            page.waitForCondition(sendBtn::isEnabled, new Page.WaitForConditionOptions().setTimeout(5000));
            log.info("АВТООТВЕТ: {} -> кнопка 'Предложить' активна", getSiteName());
        } catch (Exception e) {
            log.warn("Кнопка 'Предложить' не активна");
            return false;
        }

        if (sendRequest) {
            try {
                sendBtn.click();
            } catch (Exception e) {
                log.warn("Не удалось нажать кнопку 'Предложить'");
                return false;
            }
        }
        page.waitForTimeout(2000);
        log.info("АВТООТВЕТ: {} -> ЗАЯВКА УСПЕШНО ОТПРАВЛЕНА", getSiteName());
        return true;
    }
}