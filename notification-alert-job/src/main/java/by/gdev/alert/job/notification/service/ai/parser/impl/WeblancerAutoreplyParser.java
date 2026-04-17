package by.gdev.alert.job.notification.service.ai.parser.impl;

import by.gdev.alert.job.notification.model.dto.AiNotificationPayload;
import by.gdev.alert.job.notification.model.dto.DecryptedCredential;
import by.gdev.alert.job.notification.service.ai.parser.AutoreplyPlaywrightParser;
import by.gdev.common.model.SiteName;
import by.gdev.common.model.proxy.ProxyCredentials;
import by.gdev.common.service.playwright.PlaywrightManager;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeblancerAutoreplyParser implements AutoreplyPlaywrightParser {

    private final PlaywrightManager playwrightManager;

    @Override
    public SiteName getSiteName() {
        return SiteName.WEBLANCER;
    }

    @Override
    public boolean sendAutoreply(DecryptedCredential creds, AiNotificationPayload payload) {

        Playwright playwright = null;
        Browser browser = null;
        BrowserContext context = null;
        Page page = null;

        String site = "WEBLANCER";

        try {
            playwright = playwrightManager.createPlaywright();
            ProxyCredentials proxy = playwrightManager.getProxyWithRetry(3, 500);

            browser = playwrightManager.createBrowser(
                    playwright,
                    proxy,
                    false,
                    true,
                    site
            );

            context = playwrightManager.createBrowserContext(browser, proxy, true, site);
            page = context.newPage();

            // ЛОГИН
            login(page, creds);

            // Даем время увидеть, что логин успешный
            page.waitForTimeout(3000);

            // Переходим на заказ и пытаемся подать заявку
            processOrder(page, payload);

            page.waitForTimeout(15000);

            log.debug("Автоответ успешно отправлен пользователем {}", creds.login());
            return true;

        } catch (Exception e) {
            log.error("Ошибка при отправке автоответа", e);
            return false;

        } finally {
            playwrightManager.closeResources(page, context, browser, playwright, site);
        }
    }

    private void processOrder(Page page, AiNotificationPayload payload) {

        // Переходим на страницу заказа
        String link = payload.getOrder().getLink();
        log.info("Переход на заказ: {}", link);

        page.navigate(link);
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Ждём кнопку "Добавить заявку"
        Locator openFormBtn = page.locator("button:has-text('Добавить заявку')");
        page.waitForCondition(openFormBtn::isVisible);

        // Нажимаем кнопку "Добавить заявку"
        openFormBtn.click();

        // Ждём появления формы подачи заявки
        page.waitForSelector("textarea[placeholder='Комментарий']");

        // Вставляем текст автоответа
        String reply = payload.getDecision().reply();
        page.fill("textarea[placeholder='Комментарий']", reply);

        page.waitForTimeout(10000);

        // вставляем цену
        //page.fill("input[name='amount']", "5");

        // 7. Ждём активации кнопки "Добавить"
        Locator addBtn = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Добавить")
        );
        page.waitForCondition(addBtn::isEnabled);

        // 8. Нажимаем кнопку "Добавить"
        //addBtn.click();

        // 9. Даем время увидеть отправку
        page.waitForTimeout(10000);

        log.debug("Заявка успешно отправлена");
    }




    private void login(Page page, DecryptedCredential creds) {

        // Переходим на сайт
        page.navigate("https://www.weblancer.net/?lang=ru");

        // Кликаем "Вход"
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Вход"))
                .click();

        // Ждём появления формы
        page.waitForSelector("input[name='login']");

        // Вводим логин
        page.getByPlaceholder("Ваш логин, телефон или email")
                .fill(creds.login());

        // Вводим пароль
        page.getByPlaceholder("Ваш пароль")
                .fill(creds.password());

        // Находим кнопку
        Locator loginBtn = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Войти в аккаунт")
        );

        // Ждём, пока кнопка станет активной
        page.waitForCondition(() -> loginBtn.isEnabled());

        // Кликаем
        loginBtn.click();

        // Ждём загрузки
        page.waitForLoadState(LoadState.NETWORKIDLE);

        log.debug("Успешный вход в аккаунт {}", creds.login());
    }
}

