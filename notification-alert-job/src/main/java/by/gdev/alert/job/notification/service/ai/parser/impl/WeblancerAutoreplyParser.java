package by.gdev.alert.job.notification.service.ai.parser.impl;

import by.gdev.alert.job.notification.model.dto.AiNotificationPayload;
import by.gdev.alert.job.notification.model.dto.DecryptedCredential;
import by.gdev.alert.job.notification.service.ai.parser.AutoreplyPlaywrightParser;
import by.gdev.alert.job.notification.service.ai.proxy.AssignedProxyService;
import by.gdev.common.model.SiteName;
import by.gdev.common.service.playwright.PlaywrightManager;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WeblancerAutoreplyParser extends AutoreplyParser implements AutoreplyPlaywrightParser {

    @Value("${parser.autoreply.headless.weblancer.net:true}")
    private void setHeadless(boolean headless) {
        this.headless = headless;
    }

    @Value("${parser.autoreply.send.request.weblancer.net:true}")
    private void setOnSendRequest(boolean sendRequest) {
        this.sendRequest = sendRequest;
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.WEBLANCER;
    }

    public WeblancerAutoreplyParser(PlaywrightManager playwrightManager, AssignedProxyService assignedProxyService) {
        super(playwrightManager, assignedProxyService);
    }

    @Override
    protected boolean processAutoReply(Page page, AiNotificationPayload payload) {
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
        if (sendRequest){
            addBtn.click();
        }

        // 9. Даем время увидеть отправку
        page.waitForTimeout(10000);

        log.debug("Заявка успешно отправлена");
        return true;
    }

    @Override
    protected boolean login(Page page, DecryptedCredential creds) {
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
        return true;
    }
}