package by.gdev.alert.job.notification.service.ai.parser.impl;

import by.gdev.alert.job.notification.model.dto.AiNotificationPayload;
import by.gdev.alert.job.notification.model.dto.DecryptedCredential;
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
        String userUuid = payload.getUser().getUuid();
        log.info("АВТООТВЕТ: {} -> НАЧАЛО ОБРАБОТКИ ЗАКАЗА: {}, пользователь: {}", getSiteName(), payload.getOrder().getLink(), creds.login());

        StepResult<Void> result;

        result = openOrderPage(page, payload, creds);
        if (result.failed()) return result;

        result = clickOfferButton(page, userUuid, creds);
        if (result.failed()) return result;

        result = waitAndFillReplyEditor(page, payload, creds);
        if (result.failed()) return result;

        result = parseAndSetPrice(page, creds);
        if (result.failed()) return result;

        result = setOrderTitle(page, payload, creds);
        if (result.failed()) return result;

        result = selectDuration(page, creds);
        if (result.failed()) return result;

        result = submitOffer(page, userUuid, creds);
        if (result.failed()) return result;

        return StepResult.ok(StepType.SEND_AUTOREPLY, null);
    }


    private StepResult<Void> openOrderPage(Page page, AiNotificationPayload payload, DecryptedCredential creds) {
        try {
            page.navigate(payload.getOrder().getLink());
            page.waitForLoadState(LoadState.NETWORKIDLE);
            log.info("АВТООТВЕТ: {} -> страница заказа открыта, пользователь: {}", getSiteName(), creds.login());
            takeScreenshot(page, getSiteName(), payload.getUser().getUuid(), "order_page");
            return StepResult.ok(StepType.SEND_AUTOREPLY, null);
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ ОТКРЫТЬ ЗАКАЗ, пользователь: {}, ошибка: {}", getSiteName(), creds.login(), e.getMessage());
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Не удалось открыть заказ: " + e.getMessage(), captureScreenshot(page));
        }
    }

    private StepResult<Void> clickOfferButton(Page page, String userUuid, DecryptedCredential creds) {
        String selector = "span.projects-offer-btn:has-text('Предложить услугу')";
        try {
            Locator offerButton = page.locator(selector);
            // Ждем появления кнопки
            offerButton.waitFor(new Locator.WaitForOptions().setTimeout(8000));
            // Проверяем, есть ли класс disabled или атрибут disabled
            boolean isDisabled = offerButton.getAttribute("class").contains("disabled") || offerButton.isDisabled();
            if (isDisabled) {
                log.warn("АВТООТВЕТ: {} -> КНОПКА 'Предложить услугу' НЕАКТИВНА (disabled), пользователь: {}", getSiteName(), creds.login());
                return StepResult.fail(StepType.SEND_AUTOREPLY, "Кнопка 'Предложить услугу' неактивна - отклики закончились", captureScreenshot(page));
            }
            // Кнопка активна, кликаем
            offerButton.click();
            takeScreenshot(page, getSiteName(), userUuid, "click_propose");
            log.info("АВТООТВЕТ: {} -> кнопка 'Предложить услугу' нажата, пользователь: {}", getSiteName(), creds.login());
            return StepResult.ok(StepType.SEND_AUTOREPLY, null);
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> НЕ НАЙДЕНА КНОПКА 'Предложить услугу' или ошибка, пользователь: {}, ошибка: {}", getSiteName(), creds.login(), e.getMessage());
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Кнопка 'Предложить услугу' не найдена или ошибка: " + e.getMessage(), captureScreenshot(page));
        }
    }

    private StepResult<Void> waitAndFillReplyEditor(Page page, AiNotificationPayload payload, DecryptedCredential creds) {
        if (!waitOrFail(page, "div.trumbowyg-editor", 8000, "Редактор ответа")) {
            log.warn("АВТООТВЕТ: {} -> НЕ НАЙДЕН РЕДАКТОР, пользователь: {}", getSiteName(), creds.login());
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Редактор ответа не найден", captureScreenshot(page));
        }
        log.info("АВТООТВЕТ: {} -> редактор найден, пользователь: {}", getSiteName(), creds.login());

        try {
            String reply = payload.getDecision().reply();
            page.fill("div.trumbowyg-editor", reply);
            log.info("АВТООТВЕТ: {} -> текст ответа вставлен, длина: {}, пользователь: {}", getSiteName(), reply != null ? reply.length() : 0, creds.login());
            return StepResult.ok(StepType.SEND_AUTOREPLY, null);
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ ЗАПОЛНИТЬ ТЕКСТ, пользователь: {}, ошибка: {}", getSiteName(), creds.login(), e.getMessage());
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Не удалось заполнить текст: " + e.getMessage(), captureScreenshot(page));
        }
    }

    private StepResult<Void> parseAndSetPrice(Page page, DecryptedCredential creds) {
        try {
            String priceValue = String.valueOf(defaultPrice);

            // Получаем placeholder у поля ввода цены
            Locator priceInput = page.locator("#offer-custom-price");
            String placeholder = priceInput.getAttribute("placeholder");
            log.info("АВТООТВЕТ: {} -> placeholder поля цены: {}", getSiteName(), placeholder);

            if (placeholder != null && !placeholder.isEmpty()) {
                // Ищем первое число в placeholder (минимальная цена)
                Pattern pattern = Pattern.compile("(\\d+)");
                Matcher matcher = pattern.matcher(placeholder);
                if (matcher.find()) {
                    priceValue = matcher.group(1);
                    log.info("АВТООТВЕТ: {} -> минимальная цена из placeholder: {}", getSiteName(), priceValue);
                } else {
                    log.warn("АВТООТВЕТ: {} -> в placeholder нет чисел, используем цену по умолчанию: {}", getSiteName(), defaultPrice);
                }
            } else {
                log.warn("АВТООТВЕТ: {} -> placeholder не найден, используем цену по умолчанию: {}", getSiteName(), defaultPrice);
            }

            // Устанавливаем цену
            priceInput.fill(priceValue);
            log.info("АВТООТВЕТ: {} -> цена установлена: {}, пользователь: {}", getSiteName(), priceValue, creds.login());

            // Проверяем, не появилась ли ошибка (если цена слишком низкая)
            Locator errorMessage = page.locator("span.form-item__error:has-text('Стоимость может быть от')");
            if (errorMessage.count() > 0) {
                String errorText = errorMessage.textContent();
                Pattern pattern = Pattern.compile("от\\s*(\\d+)\\s*руб");
                Matcher matcher = pattern.matcher(errorText);
                if (matcher.find()) {
                    String correctedPrice = matcher.group(1);
                    log.info("АВТООТВЕТ: {} -> скорректировали цену по ошибке: {}", getSiteName(), correctedPrice);
                    priceInput.fill(correctedPrice);
                    priceValue = correctedPrice;
                }
            }

            log.info("АВТООТВЕТ: {} -> итоговая цена: {}, пользователь: {}", getSiteName(), priceValue, creds.login());
            return StepResult.ok(StepType.SEND_AUTOREPLY, null);
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ УСТАНОВИТЬ ЦЕНУ, пользователь: {}, ошибка: {}", getSiteName(), creds.login(), e.getMessage());
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Не удалось установить цену: " + e.getMessage(), captureScreenshot(page));
        }
    }

    private StepResult<Void> setOrderTitle(Page page, AiNotificationPayload payload, DecryptedCredential creds) {
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
                log.info("АВТООТВЕТ: {} -> название заказа заполнено: {}, пользователь: {}", getSiteName(), orderTitle, creds.login());
                return StepResult.ok(StepType.SEND_AUTOREPLY, null);
            } else {
                log.warn("АВТООТВЕТ: {} -> поле названия заказа не найдено, пользователь: {}", getSiteName(), creds.login());
                return StepResult.fail(StepType.SEND_AUTOREPLY, "Поле названия заказа не найдено", captureScreenshot(page));
            }
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ ЗАПОЛНИТЬ НАЗВАНИЕ ЗАКАЗА, пользователь: {}, ошибка: {}",
                    getSiteName(), creds.login(), e.getMessage());
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Не удалось заполнить название заказа: " + e.getMessage(), captureScreenshot(page));
        }
    }

    private StepResult<Void> selectDuration(Page page, DecryptedCredential creds) {
        if (!clickOrFail(page, "div.duration-select", 5000, "Открыть список сроков")) {
            log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ ОТКРЫТЬ СПИСОК СРОКОВ, пользователь: {}", getSiteName(), creds.login());
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Не удалось открыть список сроков", captureScreenshot(page));
        }
        log.info("АВТООТВЕТ: {} -> список сроков открыт, пользователь: {}", getSiteName(), creds.login());

        if (!waitOrFail(page, "ul.vs__dropdown-menu li", 5000, "Список сроков")) {
            log.warn("АВТООТВЕТ: {} -> НЕ НАЙДЕН СПИСОК СРОКОВ, пользователь: {}", getSiteName(), creds.login());
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Список сроков не найден", captureScreenshot(page));
        }

        try {
            page.locator("ul.vs__dropdown-menu li").first().click();
            log.info("АВТООТВЕТ: {} -> срок выполнения выбран, пользователь: {}", getSiteName(), creds.login());
            return StepResult.ok(StepType.SEND_AUTOREPLY, null);
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ ВЫБРАТЬ СРОК, пользователь: {}, ошибка: {}", getSiteName(), creds.login(), e.getMessage());
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Не удалось выбрать срок: " + e.getMessage(), captureScreenshot(page));
        }
    }

    private StepResult<Void> submitOffer(Page page, String userUuid, DecryptedCredential creds) {
        if (!waitOrFail(page, "button.kw-button--green:has-text('Предложить')", 8000, "Кнопка отправки")) {
            log.warn("АВТООТВЕТ: {} -> НЕ НАЙДЕНА КНОПКА 'Предложить', пользователь: {}", getSiteName(), creds.login());
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Кнопка 'Предложить' не найдена", captureScreenshot(page));
        }
        log.info("АВТООТВЕТ: {} -> кнопка 'Предложить' найдена, пользователь: {}", getSiteName(), creds.login());

        Locator sendBtn = page.locator("button.kw-button--green:has-text('Предложить')");
        try {
            page.waitForCondition(sendBtn::isEnabled, new Page.WaitForConditionOptions().setTimeout(5000));
            log.info("АВТООТВЕТ: {} -> кнопка 'Предложить' активна, пользователь: {}", getSiteName(), creds.login());
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> КНОПКА 'Предложить' НЕАКТИВНА, пользователь: {}, ошибка: {}", getSiteName(), creds.login(), e.getMessage());
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Кнопка 'Предложить' неактивна", captureScreenshot(page));
        }

        takeScreenshot(page, getSiteName(), userUuid, "form_filled");

        if (!sendRequest) {
            log.info("АВТООТВЕТ: {} -> ЗАЯВКА НЕ ОТПРАВЛЕНА (sendRequest=false), пользователь: {}", getSiteName(), creds.login());
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Заявка не отправлена (sendRequest=false)", captureScreenshot(page));
        }

        try {
            sendBtn.click();
            log.info("АВТООТВЕТ: {} -> кнопка 'Предложить' нажата, ждём подтверждения...", getSiteName());

            boolean sent = false;
            try {
                page.waitForCondition(
                        () -> !sendBtn.isVisible() || !sendBtn.isEnabled(),
                        new Page.WaitForConditionOptions().setTimeout(10000)
                );
                sent = true;
            } catch (com.microsoft.playwright.TimeoutError e) {
                log.warn("АВТООТВЕТ: {} -> кнопка не исчезла за 10 секунд, возможно заявка не отправлена, пользователь: {}", getSiteName(), creds.login());
            }

            if (sent) {
                log.info("АВТООТВЕТ: {} -> ЗАЯВКА УСПЕШНО ОТПРАВЛЕНА, пользователь: {}", getSiteName(), creds.login());
                page.navigate("https://kwork.ru/projects");
                page.waitForLoadState(LoadState.NETWORKIDLE);
                log.info("АВТООТВЕТ: {} -> перешли на страницу проектов", getSiteName());
                return StepResult.ok(StepType.SEND_AUTOREPLY, null);
            } else {
                log.warn("АВТООТВЕТ: {} -> кнопка не исчезла, заявка не отправлена, пользователь: {}", getSiteName(), creds.login());
                return StepResult.fail(StepType.SEND_AUTOREPLY, "Заявка не отправлена (кнопка не исчезла)", captureScreenshot(page));
            }
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ ОТПРАВИТЬ ЗАЯВКУ, пользователь: {}, ошибка: {}", getSiteName(), creds.login(), e.getMessage());
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Не удалось отправить заявку: " + e.getMessage(), captureScreenshot(page));
        }
    }
}