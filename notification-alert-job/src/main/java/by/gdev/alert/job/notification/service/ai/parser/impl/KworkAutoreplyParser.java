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
    @Value("${parser.autoreply.headless.kwork.ru}")
    private void setHeadless(boolean headless) {
        this.headless = headless;
    }

    @Value("${parser.autoreply.proxy.kwork.ru}")
    private void setProxy(boolean proxy) {
        this.proxy = proxy;
    }

    @Value("${parser.autoreply.send.request.kwork.ru}")
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
        page.navigate("https://kwork.ru/login");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        // Логин
        page.waitForSelector("input[placeholder='Электронная почта или логин']");
        page.fill("input[placeholder='Электронная почта или логин']", creds.login());
        // Пароль
        page.fill("input[placeholder='Пароль']", creds.password());
        // Кнопка "Войти"
        Locator loginBtn = page.locator("button.auth-form__button");
        page.waitForCondition(loginBtn::isEnabled);
        loginBtn.click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
        log.debug("Успешный вход в аккаунт {}", creds.login());
        return true;
    }


    @Override
    protected boolean processAutoReply(Page page, AiNotificationPayload payload) {
        String link = payload.getOrder().getLink();
        log.info("Переход на заказ: {}", link);

        try {
            page.navigate(link);
            page.waitForLoadState(LoadState.NETWORKIDLE);
        } catch (Exception e) {
            log.warn("Не удалось открыть заказ {}", link);
            return false;
        }

        // Кнопка "Предложить услугу"
        if (!clickOrFail(page, "span.projects-offer-btn:has-text('Предложить услугу')",
                8000, "Открыть форму отклика")) return false;

        // Ждём редактор
        if (!waitOrFail(page, "div.trumbowyg-editor", 8000, "Редактор ответа")) return false;

        // Текст автоответа
        try {
            page.fill("div.trumbowyg-editor", payload.getDecision().reply());
        } catch (Exception e) {
            log.warn("Не удалось заполнить текст ответа");
            return false;
        }

        // Цена
        try {
            page.fill("#offer-custom-price", String.valueOf(defaultPrice));
        } catch (Exception e) {
            log.warn("Не удалось заполнить цену");
            return false;
        }

        // Срок выполнения
        if (!clickOrFail(page, "div.duration-select", 5000, "Открыть список сроков")) return false;

        if (!waitOrFail(page, "ul.vs__dropdown-menu li", 5000, "Список сроков")) return false;

        try {
            page.locator("ul.vs__dropdown-menu li").first().click();
        } catch (Exception e) {
            log.warn("Не удалось выбрать срок выполнения");
            return false;
        }

        // 6. Кнопка "Предложить"
        if (!waitOrFail(page, "button.kw-button--green:has-text('Предложить')",
                8000, "Кнопка отправки")) return false;

        Locator sendBtn = page.locator("button.kw-button--green:has-text('Предложить')");
        try {
            page.waitForCondition(sendBtn::isEnabled, new Page.WaitForConditionOptions().setTimeout(5000));
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
        log.debug("Заявка успешно отправлена");
        return true;
    }
}