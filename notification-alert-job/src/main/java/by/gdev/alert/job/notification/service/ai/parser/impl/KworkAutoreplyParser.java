package by.gdev.alert.job.notification.service.ai.parser.impl;

import by.gdev.alert.job.notification.model.dto.AiNotificationPayload;
import by.gdev.alert.job.notification.model.dto.DecryptedCredential;
import by.gdev.alert.job.notification.service.ai.parser.AutoreplyPlaywrightParser;
import by.gdev.common.model.SiteName;
import by.gdev.common.model.proxy.ProxyCredentials;
import by.gdev.common.service.playwright.PlaywrightManager;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
/// !!!!НЕ ДО КОНЦА ПРОВЕРЕНО
public class KworkAutoreplyParser extends AutoreplyParser implements AutoreplyPlaywrightParser {

    private final PlaywrightManager playwrightManager;

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

    @Override
    public SiteName getSiteName() {
        return SiteName.KWORK;
    }

    @Override
    public boolean sendAutoreply(DecryptedCredential creds, AiNotificationPayload payload) {

        Playwright playwright = null;
        Browser browser = null;
        BrowserContext context = null;
        Page page = null;

        try {
            playwright = playwrightManager.createPlaywright();
            ProxyCredentials proxyCred = playwrightManager.getProxyWithRetry(3, 500);

            browser = playwrightManager.createBrowser(
                    playwright,
                    proxyCred,
                    headless,
                    proxy,
                    getSiteName().name()
            );

            context = playwrightManager.createBrowserContext(browser, proxyCred, true, getSiteName().name());
            page = context.newPage();
            // ЛОГИН
            login(page, creds);
            page.waitForTimeout(3000);
            // Переходим на заказ и отправляем автоответ
            processAutoReply(page, payload);
            page.waitForTimeout(15000);
            log.debug("Автоответ успешно отправлен пользователем {}", creds.login());
            return true;

        } catch (Exception e) {
            log.error("Ошибка при отправке автоответа", e);
            return false;

        } finally {
            playwrightManager.closeResources(page, context, browser, playwright, getSiteName().name());
        }
    }

    private void login(Page page, DecryptedCredential creds) {
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
    }


    private void processAutoReply(Page page, AiNotificationPayload payload) {
        String link = payload.getOrder().getLink();
        log.info("Переход на заказ: {}", link);
        page.navigate(link);
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Кнопка "Предложить услугу"
        Locator replyBtn = page.locator("span.projects-offer-btn:has-text('Предложить услугу')");
        page.waitForCondition(replyBtn::isVisible);
        replyBtn.click();

        // Ждём появления редактора
        page.waitForSelector("div.trumbowyg-editor");

        // Вставляем текст автоответа
        String reply = payload.getDecision().reply();
        page.fill("div.trumbowyg-editor", reply);

        // Цена
        String price = "500";
        page.fill("#offer-custom-price", price);

        // Срок выполнения
        Locator durationSelect = page.locator("div.duration-select");
        durationSelect.click();

        // Ждём появления списка
        page.waitForSelector("ul.vs__dropdown-menu li");
        // Выбираем первый вариант
        page.locator("ul.vs__dropdown-menu li").first().click();

        // Кнопка "Отправить предложение"
        Locator sendBtn = page.locator("button.kw-button--green:has-text('Предложить')");
        page.waitForCondition(sendBtn::isEnabled);

        if (sendRequest) {
            sendBtn.click();
        }

        page.waitForTimeout(5000);

        log.debug("Заявка успешно отправлена");
    }

}
