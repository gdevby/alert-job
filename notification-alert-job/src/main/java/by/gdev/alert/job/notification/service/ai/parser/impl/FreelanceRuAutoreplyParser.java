package by.gdev.alert.job.notification.service.ai.parser.impl;

import by.gdev.alert.job.notification.model.AutoreplyMode;
import by.gdev.alert.job.notification.model.dto.AiNotificationPayload;
import by.gdev.alert.job.notification.model.dto.DecryptedCredential;
import by.gdev.alert.job.notification.service.ai.merics.AutoreplyErrorTypes;
import by.gdev.alert.job.notification.service.ai.parser.AutoreplyPlaywrightParser;
import by.gdev.alert.job.notification.service.ai.proxy.AssignedProxyService;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepResult;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepType;
import by.gdev.common.model.SiteName;
import by.gdev.common.service.playwright.PlaywrightManager;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.event.Level;
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
    protected StepResult<Void> login(Page page, AiNotificationPayload payload, DecryptedCredential creds, AutoreplyMode mode) {
        log.info("АВТООТВЕТ: {} -> НАЧАЛО ЛОГИНА, пользователь: {}", getSiteName(), creds.login());

        try {
            safeNavigate(page, "https://freelance.ru/");
            log.info("АВТООТВЕТ: {} -> главная страница загружена, пользователь: {}", getSiteName(), creds.login());
        } catch (Exception e) {
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> НЕ УДАЛОСЬ ОТКРЫТЬ ГЛАВНУЮ СТРАНИЦУ, пользователь: "
                            + creds.login() + ", ошибка: " + e.getMessage(),
                    AutoreplyErrorTypes.ERROR_OPEN_PAGE);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Не удалось открыть главную страницу: " + e.getMessage(), captureScreenshot(page));
        }

        if (!clickOrFail(page, "a[href='/auth/login']", 8000, "Кнопка 'Вход'")) {
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> НЕ НАЙДЕНА КНОПКА 'Вход', пользователь: " + creds.login(),
                    AutoreplyErrorTypes.BUTTON_NOT_FOUND);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Кнопка 'Вход' не найдена", captureScreenshot(page));
        }
        log.info("АВТООТВЕТ: {} -> кнопка 'Вход' нажата, пользователь: {}", getSiteName(), creds.login());

        if (!waitOrFail(page, "input[placeholder='логин или email']", 8000, "Поле логина")) {
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> НЕ НАЙДЕНО ПОЛЕ ЛОГИНА, пользователь: " + creds.login(),
                    AutoreplyErrorTypes.FIELD_NOT_FOUND);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Поле логина не найдено", captureScreenshot(page));
        }

        try {
            page.fill("input[placeholder='логин или email']", creds.login());
            log.info("АВТООТВЕТ: {} -> логин заполнен: {}", getSiteName(), creds.login());
        } catch (Exception e) {
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> НЕ УДАЛОСЬ ЗАПОЛНИТЬ ЛОГИН, пользователь: "
                            + creds.login() + ", ошибка: " + e.getMessage(),
                    AutoreplyErrorTypes.FIELD_NOT_FOUND);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Не удалось заполнить логин: " + e.getMessage(), captureScreenshot(page));
        }

        try {
            page.fill("input[type='password']", creds.password());
            log.info("АВТООТВЕТ: {} -> пароль заполнен для пользователя: {}", getSiteName(), creds.login());
        } catch (Exception e) {
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> НЕ УДАЛОСЬ ЗАПОЛНИТЬ ПАРОЛЬ, пользователь: "
                            + creds.login() + ", ошибка: " + e.getMessage(),
                    AutoreplyErrorTypes.FIELD_NOT_FOUND);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Не удалось заполнить пароль: " + e.getMessage(), captureScreenshot(page));
        }

        if (!clickOrFail(page, "button:has-text('Войти')", 8000, "Кнопка 'Войти'")) {
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> НЕ НАЙДЕНА КНОПКА 'Войти', пользователь: " + creds.login(),
                    AutoreplyErrorTypes.BUTTON_NOT_FOUND);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Кнопка 'Войти' не найдена", captureScreenshot(page));
        }
        log.info("АВТООТВЕТ: {} -> кнопка 'Войти' нажата, пользователь: {}", getSiteName(), creds.login());

        try {
            page.waitForLoadState(LoadState.NETWORKIDLE);
            log.info("АВТООТВЕТ: {} -> страница загружена после входа, пользователь: {}", getSiteName(), creds.login());
        } catch (Exception e) {
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> НЕ УДАЛОСЬ ДОЖДАТЬСЯ ЗАГРУЗКИ ПОСЛЕ ВХОДА, пользователь: "
                            + creds.login() + ", ошибка: " + e.getMessage(),
                    AutoreplyErrorTypes.TIMEOUT);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Не удалось дождаться загрузки после входа: " + e.getMessage(), captureScreenshot(page));
        }

        if (isLoginErrorPresent(page)) {
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> НЕВЕРНЫЙ ЛОГИН ИЛИ ПАРОЛЬ, пользователь: " + creds.login(),
                    AutoreplyErrorTypes.LOGIN_FAILED);
            return StepResult.fail(StepType.SEND_AUTOREPLY,
                    "Неверный логин или пароль",
                    captureScreenshot(page));
        }

        log.info("АВТООТВЕТ: {} -> ЛОГИН УСПЕШЕН, пользователь: {}", getSiteName(), creds.login());
        setOpt(payload, null, false);
        return StepResult.ok(StepType.SEND_AUTOREPLY, null);
    }

    private boolean isLoginErrorPresent(Page page) {
        try {
            Locator errorLocator = page.locator("div.fi-err:has-text('Неверный логин или пароль')");
            return errorLocator.count() > 0;
        } catch (Exception e) {
            return false;
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
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> НЕ УДАЛОСЬ ОТКРЫТЬ ЗАКАЗ, пользователь: "
                            + login + ", ошибка: " + e.getMessage(),
                    AutoreplyErrorTypes.ERROR_OPEN_PAGE);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Не удалось открыть заказ: " + e.getMessage(), captureScreenshot(page));
        }

        if (!clickOrFail(page,
                "button.btn.btn--success.btn--lg.btn--block:has-text('Откликнуться')",
                8000,
                "Кнопка 'Откликнуться'")) {
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> НЕ НАЙДЕНА КНОПКА 'Откликнуться', пользователь: " + login,
                    AutoreplyErrorTypes.BUTTON_NOT_FOUND);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Кнопка 'Откликнуться' не найдена", captureScreenshot(page));
        }
        log.info("АВТООТВЕТ: {} -> кнопка 'Откликнуться' нажата, пользователь: {}", getSiteName(), login);

        if (!waitOrFail(page,
                "textarea#replyText[name='TaskReply[text]']",
                8000,
                "Поле ответа")) {
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> НЕ НАЙДЕНО ПОЛЕ ОТВЕТА, пользователь: " + login,
                    AutoreplyErrorTypes.FIELD_NOT_FOUND);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Поле ответа не найдено", captureScreenshot(page));
        }

        try {
            page.fill("textarea#replyText[name='TaskReply[text]']", payload.getDecision().reply());
            log.info("АВТООТВЕТ: {} -> текст ответа вставлен, длина: {}, пользователь: {}", getSiteName(),
                    payload.getDecision().reply() != null ? payload.getDecision().reply().length() : 0, login);
        } catch (Exception e) {
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> НЕ УДАЛОСЬ ЗАПОЛНИТЬ ТЕКСТ ОТВЕТА, пользователь: "
                            + login + ", ошибка: " + e.getMessage(),
                    AutoreplyErrorTypes.FIELD_NOT_FOUND);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Не удалось заполнить текст ответа: " + e.getMessage(), captureScreenshot(page));
        }

        if (!waitOrFail(page,
                "button#createReply.btn.btn--success.btn--sm",
                8000,
                "Кнопка отправки")) {
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> НЕ НАЙДЕНА КНОПКА ОТПРАВКИ, пользователь: " + login,
                    AutoreplyErrorTypes.BUTTON_NOT_FOUND);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Кнопка отправки не найдена", captureScreenshot(page));
        }
        log.info("АВТООТВЕТ: {} -> кнопка отправки найдена, пользователь: {}", getSiteName(), login);

        Locator sendBtn = page.locator("button#createReply.btn.btn--success.btn--sm");
        try {
            page.waitForCondition(sendBtn::isEnabled,
                    new Page.WaitForConditionOptions().setTimeout(5000));
            log.info("АВТООТВЕТ: {} -> кнопка отправки активна, пользователь: {}", getSiteName(), login);
        } catch (Exception e) {
            report(Level.WARN, log,
                    "АВТООТВЕТ: " + getSiteName() + " -> КНОПКА ОТПРАВКИ НЕАКТИВНА, пользователь: "
                            + login + ", ошибка: " + e.getMessage(),
                    AutoreplyErrorTypes.BUTTON_NOT_FOUND);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Кнопка отправки неактивна", captureScreenshot(page));
        }

        if (sendRequest) {
            try {
                sendBtn.click();
                log.info("АВТООТВЕТ: {} -> ЗАЯВКА УСПЕШНО ОТПРАВЛЕНА, пользователь: {}", getSiteName(), login);
            } catch (Exception e) {
                report(Level.WARN, log,
                        "АВТООТВЕТ: " + getSiteName() + " -> НЕ УДАЛОСЬ НАЖАТЬ КНОПКУ ОТПРАВКИ, пользователь: "
                                + login + ", ошибка: " + e.getMessage(),
                        AutoreplyErrorTypes.BUTTON_NOT_FOUND);
                return StepResult.fail(StepType.SEND_AUTOREPLY, "Не удалось нажать кнопку отправки: " + e.getMessage(), captureScreenshot(page));
            }
        } else {
            log.info("АВТООТВЕТ: {} -> ЗАЯВКА НЕ ОТПРАВЛЕНА (sendRequest=false), пользователь: {}", getSiteName(), login);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Заявка не отправлена (sendRequest=false)", captureScreenshot(page));
        }

        page.waitForTimeout(2000);
        log.info("АВТООТВЕТ: {} -> ОТКЛИК УСПЕШНО ЗАВЕРШЁН, пользователь: {}", getSiteName(), login);
        return StepResult.ok(StepType.SEND_AUTOREPLY, null);
    }
}