package by.gdev.alert.job.notification.service.ai.parser.impl;

import by.gdev.alert.job.notification.model.dto.AiNotificationPayload;
import by.gdev.alert.job.notification.model.dto.DecryptedCredential;
import by.gdev.alert.job.notification.service.ai.parser.AutoreplyPlaywrightParser;
import by.gdev.alert.job.notification.service.ai.proxy.AssignedProxyService;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepResult;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepType;
import by.gdev.common.model.SiteName;
import by.gdev.common.service.playwright.PlaywrightManager;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    protected StepResult<Void> login(Page page, DecryptedCredential creds) {
        log.info("АВТООТВЕТ: {} -> НАЧАЛО ЛОГИНА, пользователь: {}", getSiteName(), creds.login());

        try {
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
            return StepResult.ok(StepType.SEND_AUTOREPLY, null);
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> ОШИБКА ЛОГИНА, пользователь: {}, ошибка: {}", getSiteName(), creds.login(), e.getMessage());
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Ошибка логина: " + e.getMessage(), captureScreenshot(page));
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

        if (!clickOrFail(page, "span.projects-offer-btn:has-text('Предложить услугу')",
                8000, "Открыть форму отклика")) {
            log.warn("АВТООТВЕТ: {} -> НЕ НАЙДЕНА КНОПКА 'Предложить услугу', пользователь: {}", getSiteName(), login);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Кнопка 'Предложить услугу' не найдена", captureScreenshot(page));
        }
        takeScreenshot(page, getSiteName(), payload.getUser().getUuid(), "click_propose");
        log.info("АВТООТВЕТ: {} -> кнопка 'Предложить услугу' нажата, пользователь: {}", getSiteName(), login);

        if (!waitOrFail(page, "div.trumbowyg-editor", 8000, "Редактор ответа")) {
            log.warn("АВТООТВЕТ: {} -> НЕ НАЙДЕН РЕДАКТОР, пользователь: {}", getSiteName(), login);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Редактор ответа не найден", captureScreenshot(page));
        }
        log.info("АВТООТВЕТ: {} -> редактор найден, пользователь: {}", getSiteName(), login);

        try {
            page.fill("div.trumbowyg-editor", payload.getDecision().reply());
            log.info("АВТООТВЕТ: {} -> текст ответа вставлен, длина: {}, пользователь: {}", getSiteName(),
                    payload.getDecision().reply() != null ? payload.getDecision().reply().length() : 0, login);
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ ЗАПОЛНИТЬ ТЕКСТ, пользователь: {}, ошибка: {}", getSiteName(), login, e.getMessage());
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Не удалось заполнить текст: " + e.getMessage(), captureScreenshot(page));
        }

        // ЦЕНА: ПАРСИМ МИНИМУМ ИЗ БЮДЖЕТА
        try {
            String priceValue = String.valueOf(defaultPrice);
            String budgetText = null;

            Locator budgetLocator = page.locator("span.kw-budget");
            if (budgetLocator.count() > 0) {
                budgetText = budgetLocator.textContent();
                log.info("АВТООТВЕТ: {} -> найден бюджет: {}", getSiteName(), budgetText);
            }

            if (budgetText != null && !budgetText.isEmpty()) {
                Pattern pattern = Pattern.compile("(\\d+)");
                Matcher matcher = pattern.matcher(budgetText);
                if (matcher.find()) {
                    priceValue = matcher.group(1);
                    log.info("АВТООТВЕТ: {} -> минимальная цена из бюджета: {}", getSiteName(), priceValue);
                }
            }

            page.fill("#offer-custom-price", priceValue);
            log.info("АВТООТВЕТ: {} -> цена установлена: {}, пользователь: {}", getSiteName(), priceValue, login);
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ УСТАНОВИТЬ ЦЕНУ, пользователь: {}, ошибка: {}", getSiteName(), login, e.getMessage());
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Не удалось установить цену: " + e.getMessage(), captureScreenshot(page));
        }

        // НАЗВАНИЕ ЗАКАЗА (ЧЕРЕЗ JAVASCRIPT)
        try {
            page.waitForSelector("div.trumbowyg-editor[data-placeholder-mobile='Введите название заказа']",
                    new Page.WaitForSelectorOptions().setTimeout(5000));

            String orderTitle = payload.getOrder().getTitle();
            if (orderTitle == null || orderTitle.isEmpty()) {
                orderTitle = "Заказ " + System.currentTimeMillis();
            }

            String safeTitle = orderTitle.replace("'", "\\'").replace("\"", "\\\"");

            String js = String.format("""
                    (function() {
                        var editor = document.querySelector('div.trumbowyg-editor[data-placeholder-mobile="Введите название заказа"]');
                        if (editor) {
                            editor.click();
                            editor.focus();
                            editor.innerText = '%s';
                            var evt = new Event('input', { bubbles: true });
                            editor.dispatchEvent(evt);
                            return true;
                        }
                        return false;
                    })();
                    """, safeTitle);

            boolean success = (boolean) page.evaluate(js);
            if (success) {
                log.info("АВТООТВЕТ: {} -> название заказа заполнено: {}, пользователь: {}", getSiteName(), orderTitle, login);
            } else {
                log.warn("АВТООТВЕТ: {} -> поле названия заказа не найдено, пользователь: {}", getSiteName(), login);
            }
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ ЗАПОЛНИТЬ НАЗВАНИЕ ЗАКАЗА, пользователь: {}, ошибка: {}",
                    getSiteName(), login, e.getMessage());
        }

        if (!clickOrFail(page, "div.duration-select", 5000, "Открыть список сроков")) {
            log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ ОТКРЫТЬ СПИСОК СРОКОВ, пользователь: {}", getSiteName(), login);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Не удалось открыть список сроков", captureScreenshot(page));
        }
        log.info("АВТООТВЕТ: {} -> список сроков открыт, пользователь: {}", getSiteName(), login);

        if (!waitOrFail(page, "ul.vs__dropdown-menu li", 5000, "Список сроков")) {
            log.warn("АВТООТВЕТ: {} -> НЕ НАЙДЕН СПИСОК СРОКОВ, пользователь: {}", getSiteName(), login);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Список сроков не найден", captureScreenshot(page));
        }

        try {
            page.locator("ul.vs__dropdown-menu li").first().click();
            log.info("АВТООТВЕТ: {} -> срок выполнения выбран, пользователь: {}", getSiteName(), login);
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ ВЫБРАТЬ СРОК, пользователь: {}, ошибка: {}", getSiteName(), login, e.getMessage());
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Не удалось выбрать срок: " + e.getMessage(), captureScreenshot(page));
        }

        if (!waitOrFail(page, "button.kw-button--green:has-text('Предложить')",
                8000, "Кнопка отправки")) {
            log.warn("АВТООТВЕТ: {} -> НЕ НАЙДЕНА КНОПКА 'Предложить', пользователь: {}", getSiteName(), login);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Кнопка 'Предложить' не найдена", captureScreenshot(page));
        }
        log.info("АВТООТВЕТ: {} -> кнопка 'Предложить' найдена, пользователь: {}", getSiteName(), login);

        Locator sendBtn = page.locator("button.kw-button--green:has-text('Предложить')");
        try {
            page.waitForCondition(sendBtn::isEnabled, new Page.WaitForConditionOptions().setTimeout(5000));
            log.info("АВТООТВЕТ: {} -> кнопка 'Предложить' активна, пользователь: {}", getSiteName(), login);
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> КНОПКА 'Предложить' НЕАКТИВНА, пользователь: {}, ошибка: {}", getSiteName(), login, e.getMessage());
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Кнопка 'Предложить' неактивна", captureScreenshot(page));
        }
        takeScreenshot(page, getSiteName(), payload.getUser().getUuid(), "form_filled");

        if (sendRequest) {
            try {
                sendBtn.click();
                log.info("АВТООТВЕТ: {} -> кнопка 'Предложить' нажата, ждём подтверждения...", getSiteName());

                // Ждём, пока кнопка исчезнет
                boolean sent = false;
                try {
                    page.waitForCondition(
                            () -> !sendBtn.isVisible() || !sendBtn.isEnabled(),
                            new Page.WaitForConditionOptions().setTimeout(10000)
                    );
                    sent = true;
                } catch (com.microsoft.playwright.TimeoutError e) {
                    log.warn("АВТООТВЕТ: {} -> кнопка не исчезла за 10 секунд, возможно заявка не отправлена, пользователь: {}", getSiteName(), login);
                }

                if (sent) {
                    log.info("АВТООТВЕТ: {} -> ЗАЯВКА УСПЕШНО ОТПРАВЛЕНА, пользователь: {}", getSiteName(), login);
                    // Переход на страницу проектов
                    page.navigate("https://kwork.ru/projects");
                    page.waitForLoadState(LoadState.NETWORKIDLE);
                    log.info("АВТООТВЕТ: {} -> перешли на страницу проектов", getSiteName());
                } else {
                    log.warn("АВТООТВЕТ: {} -> кнопка не исчезла, заявка не отправлена, пользователь: {}", getSiteName(), login);
                    return StepResult.fail(StepType.SEND_AUTOREPLY, "Заявка не отправлена (кнопка не исчезла)", captureScreenshot(page));
                }
            } catch (Exception e) {
                log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ ОТПРАВИТЬ ЗАЯВКУ, пользователь: {}, ошибка: {}", getSiteName(), login, e.getMessage());
                return StepResult.fail(StepType.SEND_AUTOREPLY, "Не удалось отправить заявку: " + e.getMessage(), captureScreenshot(page));
            }
        } else {
            log.info("АВТООТВЕТ: {} -> ЗАЯВКА НЕ ОТПРАВЛЕНА (sendRequest=false), пользователь: {}", getSiteName(), login);
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Заявка не отправлена (sendRequest=false)", captureScreenshot(page));
        }

        return StepResult.ok(StepType.SEND_AUTOREPLY, null);
    }
}