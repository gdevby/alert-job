package by.gdev.alert.job.notification.service.ai.parser.impl;

import by.gdev.alert.job.notification.model.AutoreplyMode;
import by.gdev.alert.job.notification.model.dto.AiNotificationPayload;
import by.gdev.alert.job.notification.model.dto.DecryptedCredential;
import by.gdev.alert.job.notification.service.ai.parser.AutoreplyPlaywrightParser;
import by.gdev.alert.job.notification.service.ai.proxy.AssignedProxyService;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepResult;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepType;
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

    @Value("${parser.autoreply.proxy.weblancer.net:false}")
    private void setProxy(boolean proxy) {
        this.proxy = proxy;
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.WEBLANCER;
    }

    public WeblancerAutoreplyParser(PlaywrightManager playwrightManager, AssignedProxyService assignedProxyService) {
        super(playwrightManager, assignedProxyService);
    }

    @Override
    protected StepResult<Void> login(Page page, AiNotificationPayload payload, DecryptedCredential creds, AutoreplyMode mode) {
        log.info("АВТООТВЕТ: {} -> НАЧАЛО ЛОГИНА, пользователь: {}", getSiteName(), creds.login());

        try {
            page.navigate("https://www.weblancer.net/?lang=ru");
            log.info("АВТООТВЕТ: {} -> главная страница загружена, пользователь: {}", getSiteName(), creds.login());

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Вход"))
                    .click();
            log.info("АВТООТВЕТ: {} -> кнопка 'Вход' нажата, пользователь: {}", getSiteName(), creds.login());

            page.waitForSelector("input[name='login']");
            log.debug("АВТООТВЕТ: {} -> форма логина загружена, пользователь: {}", getSiteName(), creds.login());

            page.getByPlaceholder("Ваш логин, телефон или email")
                    .fill(creds.login());
            log.info("АВТООТВЕТ: {} -> логин заполнен: {}", getSiteName(), creds.login());

            page.getByPlaceholder("Ваш пароль")
                    .fill(creds.password());
            log.info("АВТООТВЕТ: {} -> пароль заполнен для пользователя: {}", getSiteName(), creds.login());

            Locator loginBtn = page.getByRole(
                    AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("Войти в аккаунт")
            );

            page.waitForCondition(() -> loginBtn.isEnabled());
            log.debug("АВТООТВЕТ: {} -> кнопка 'Войти в аккаунт' активна, пользователь: {}", getSiteName(), creds.login());

            loginBtn.click();
            log.info("АВТООТВЕТ: {} -> кнопка 'Войти в аккаунт' нажата, пользователь: {}", getSiteName(), creds.login());

            page.waitForLoadState(LoadState.NETWORKIDLE);
            log.info("АВТООТВЕТ: {} -> страница загружена после входа, пользователь: {}", getSiteName(), creds.login());

            log.info("АВТООТВЕТ: {} -> ЛОГИН УСПЕШЕН, пользователь: {}", getSiteName(), creds.login());
            setOpt(payload, null, false);
            return StepResult.ok(StepType.SEND_AUTOREPLY, null);

        } catch (Exception e) {
            log.error("АВТООТВЕТ: {} -> ОШИБКА ПРИ ЛОГИНЕ, пользователь: {}, ошибка: {}", getSiteName(), creds.login(), e.getMessage(), e);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Ошибка при логине: " + e.getMessage(), captureScreenshot(page));
        }
    }

    @Override
    protected StepResult<Void> processAutoReply(Page page, AiNotificationPayload payload, DecryptedCredential creds) {
        String link = payload.getOrder().getLink();
        String login = creds.login();
        log.info("АВТООТВЕТ: {} -> НАЧАЛО ОБРАБОТКИ ЗАКАЗА: {}, пользователь: {}", getSiteName(), link, login);

        try {
            page.navigate(link);
            page.waitForLoadState(LoadState.NETWORKIDLE);
            log.info("АВТООТВЕТ: {} -> страница заказа открыта, пользователь: {}", getSiteName(), login);
            takeScreenshot(page, getSiteName(), payload.getUser().getUuid(), "order_page");
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ ОТКРЫТЬ ЗАКАЗ, пользователь: {}, ошибка: {}", getSiteName(), login, e.getMessage());
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Не удалось открыть заказ: " + e.getMessage(), captureScreenshot(page));
        }

        try {
            Locator openFormBtn = page.locator("button:has-text('Добавить заявку')");
            page.waitForCondition(openFormBtn::isVisible);
            log.debug("АВТООТВЕТ: {} -> кнопка 'Добавить заявку' видна, пользователь: {}", getSiteName(), login);

            openFormBtn.click();
            log.info("АВТООТВЕТ: {} -> кнопка 'Добавить заявку' нажата, пользователь: {}", getSiteName(), login);
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ НАЖАТЬ КНОПКУ 'Добавить заявку', пользователь: {}, ошибка: {}", getSiteName(), login, e.getMessage());
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Не удалось нажать кнопку 'Добавить заявку': " + e.getMessage(), captureScreenshot(page));
        }

        try {
            page.waitForSelector("textarea[placeholder='Комментарий']");
            log.debug("АВТООТВЕТ: {} -> форма подачи заявки загружена, пользователь: {}", getSiteName(), login);
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> НЕ ДОЖДАЛИСЬ ФОРМЫ ПОДАЧИ ЗАЯВКИ, пользователь: {}, ошибка: {}", getSiteName(), login, e.getMessage());
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Не дождались формы подачи заявки: " + e.getMessage(), captureScreenshot(page));
        }

        try {
            String reply = payload.getDecision().reply();
            page.fill("textarea[placeholder='Комментарий']", reply);
            log.info("АВТООТВЕТ: {} -> текст ответа вставлен, длина: {}, пользователь: {}", getSiteName(),
                    reply != null ? reply.length() : 0, login);
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ ЗАПОЛНИТЬ ТЕКСТ ОТВЕТА, пользователь: {}, ошибка: {}", getSiteName(), login, e.getMessage());
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Не удалось заполнить текст ответа: " + e.getMessage(), captureScreenshot(page));
        }

        page.waitForTimeout(10000);
        log.debug("АВТООТВЕТ: {} -> ожидание 10 секунд перед отправкой, пользователь: {}", getSiteName(), login);

        try {
            Locator addBtn = page.getByRole(
                    AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("Добавить")
            );
            page.waitForCondition(addBtn::isEnabled);
            log.debug("АВТООТВЕТ: {} -> кнопка 'Добавить' активна, пользователь: {}", getSiteName(), login);
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> КНОПКА 'Добавить' НЕАКТИВНА, пользователь: {}, ошибка: {}", getSiteName(), login, e.getMessage());
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Кнопка 'Добавить' неактивна", captureScreenshot(page));
        }

        if (sendRequest) {
            try {
                Locator addBtn = page.getByRole(
                        AriaRole.BUTTON,
                        new Page.GetByRoleOptions().setName("Добавить")
                );
                addBtn.click();
                log.info("АВТООТВЕТ: {} -> ЗАЯВКА УСПЕШНО ОТПРАВЛЕНА, пользователь: {}", getSiteName(), login);
            } catch (Exception e) {
                log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ ОТПРАВИТЬ ЗАЯВКУ, пользователь: {}, ошибка: {}", getSiteName(), login, e.getMessage());
                return StepResult.fail(StepType.SEND_AUTOREPLY, "Не удалось отправить заявку: " + e.getMessage(), captureScreenshot(page));
            }
        } else {
            log.info("АВТООТВЕТ: {} -> ЗАЯВКА НЕ ОТПРАВЛЕНА (sendRequest=false), пользователь: {}", getSiteName(), login);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Заявка не отправлена (sendRequest=false)", captureScreenshot(page));
        }

        page.waitForTimeout(10000);
        log.info("АВТООТВЕТ: {} -> ОТКЛИК УСПЕШНО ЗАВЕРШЁН, пользователь: {}", getSiteName(), login);
        return StepResult.ok(StepType.SEND_AUTOREPLY, null);
    }
}