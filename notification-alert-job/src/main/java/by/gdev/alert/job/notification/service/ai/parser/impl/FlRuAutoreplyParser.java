package by.gdev.alert.job.notification.service.ai.parser.impl;

import by.gdev.alert.job.notification.model.dto.AiNotificationPayload;
import by.gdev.alert.job.notification.model.dto.DecryptedCredential;
import by.gdev.alert.job.notification.service.ai.parser.AutoreplyPlaywrightParser;
import by.gdev.alert.job.notification.service.ai.proxy.AssignedProxyService;
import by.gdev.common.model.SiteName;
import by.gdev.common.service.playwright.CaptchaService;
import by.gdev.common.service.playwright.PlaywrightManager;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FlRuAutoreplyParser extends AutoreplyParser implements AutoreplyPlaywrightParser {

    private final CaptchaService captchaService;

    @Value("${parser.autoreply.headless.fl.ru:true}")
    private void setHeadless(boolean headless) {
        this.headless = headless;
    }

    @Value("${parser.autoreply.proxy.fl.ru:false}")
    private void setProxy(boolean proxy) {
        this.proxy = proxy;
    }

    @Value("${parser.autoreply.send.request.fl.ru:true}")
    private void setOnSendRequest(boolean sendRequest) {
        this.sendRequest = sendRequest;
    }

    public FlRuAutoreplyParser(PlaywrightManager playwrightManager, AssignedProxyService assignedProxyService, CaptchaService captchaService) {
        super(playwrightManager, assignedProxyService);
        this.captchaService = captchaService;
    }

    @Override
    protected boolean login(Page page, DecryptedCredential creds) {
        log.info("АВТООТВЕТ: {} -> НАЧАЛО ЛОГИНА, пользователь: {}", getSiteName(), creds.login());

        try {
            safeNavigate(page, "https://www.fl.ru/account/login/");
            log.info("АВТООТВЕТ: {} -> страница логина загружена, пользователь: {}", getSiteName(), creds.login());

            if (!waitOrFail(page, "input[name='username']", 8000, "Поле логина")) {
                log.warn("АВТООТВЕТ: {} -> НЕ НАЙДЕНО ПОЛЕ ЛОГИНА, пользователь: {}", getSiteName(), creds.login());
                return false;
            }

            try {
                page.fill("input[name='username']", creds.login());
                log.info("АВТООТВЕТ: {} -> логин заполнен: {}", getSiteName(), creds.login());
            } catch (Exception e) {
                log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ ЗАПОЛНИТЬ ЛОГИН, пользователь: {}, ошибка: {}", getSiteName(), creds.login(), e.getMessage());
                return false;
            }

            try {
                page.fill("input[name='password']", creds.password());
                log.info("АВТООТВЕТ: {} -> пароль заполнен для пользователя: {}", getSiteName(), creds.login());
            } catch (Exception e) {
                log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ ЗАПОЛНИТЬ ПАРОЛЬ, пользователь: {}, ошибка: {}", getSiteName(), creds.login(), e.getMessage());
                return false;
            }

            log.info("АВТООТВЕТ: {} -> попытка прохождения SmartCaptcha для пользователя: {}", getSiteName(), creds.login());
            if (!captchaService.solveYandexSmartCaptcha(page)) {
                log.warn("АВТООТВЕТ: {} -> SmartCaptcha НЕ ПРОЙДЕНА, пользователь: {}", getSiteName(), creds.login());
                return false;
            }
            log.info("АВТООТВЕТ: {} -> SmartCaptcha пройдена, пользователь: {}", getSiteName(), creds.login());

            if (!clickOrFail(page, "#submit-button", 8000, "Кнопка 'Войти'")) {
                log.warn("АВТООТВЕТ: {} -> НЕ НАЙДЕНА КНОПКА 'Войти', пользователь: {}", getSiteName(), creds.login());
                return false;
            }
            log.info("АВТООТВЕТ: {} -> кнопка 'Войти' нажата, пользователь: {}", getSiteName(), creds.login());

            try {
                page.waitForLoadState(LoadState.NETWORKIDLE);
                log.info("АВТООТВЕТ: {} -> страница загружена после входа, пользователь: {}", getSiteName(), creds.login());
            } catch (Exception e) {
                log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ ДОЖДАТЬСЯ ЗАГРУЗКИ ПОСЛЕ ВХОДА, пользователь: {}, ошибка: {}", getSiteName(), creds.login(), e.getMessage());
                return false;
            }

            if (page.url().contains("/account/login")) {
                log.warn("АВТООТВЕТ: {} -> ВХОД НЕ ВЫПОЛНЕН, остались на странице логина, пользователь: {}", getSiteName(), creds.login());
                return false;
            }

            log.info("АВТООТВЕТ: {} -> ЛОГИН УСПЕШЕН, пользователь: {}", getSiteName(), creds.login());
            return true;

        } catch (Exception e) {
            log.error("АВТООТВЕТ: {} -> ОШИБКА ПРИ ЛОГИНЕ, пользователь: {}, ошибка: {}", getSiteName(), creds.login(), e.getMessage(), e);
            return false;
        }
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

        if (!waitOrFail(page, "#el-descr", 8000, "Поле текста отклика")) {
            log.warn("АВТООТВЕТ: {} -> НЕ НАЙДЕНО ПОЛЕ ТЕКСТА ОТКЛИКА, пользователь: {}", getSiteName(), login);
            return false;
        }

        try {
            page.fill("#el-descr", payload.getDecision().reply());
            log.info("АВТООТВЕТ: {} -> текст ответа вставлен, длина: {}, пользователь: {}", getSiteName(),
                    payload.getDecision().reply() != null ? payload.getDecision().reply().length() : 0, login);
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ ЗАПОЛНИТЬ ТЕКСТ ОТКЛИКА, пользователь: {}, ошибка: {}", getSiteName(), login, e.getMessage());
            return false;
        }

        try {
            Locator payRadio = page.locator("label[for='el-pay-0']");
            if (payRadio.count() > 0) {
                payRadio.click();
                log.info("АВТООТВЕТ: {} -> выбран способ оплаты 'На банковскую карту физ. лица', пользователь: {}", getSiteName(), login);
            } else {
                log.warn("АВТООТВЕТ: {} -> радиокнопка оплаты не найдена, пользователь: {}", getSiteName(), login);
            }
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> ошибка при выборе способа оплаты, пользователь: {}, ошибка: {}", getSiteName(), login, e.getMessage());
        }

        if (!waitOrFail(page, "#el-time_from", 8000, "Поле срока выполнения")) {
            log.warn("АВТООТВЕТ: {} -> НЕ НАЙДЕНО ПОЛЕ СРОКА ВЫПОЛНЕНИЯ, пользователь: {}", getSiteName(), login);
            return false;
        }

        try {
            String duration = String.valueOf(defaultDays);
            page.fill("#el-time_from", duration);
            log.info("АВТООТВЕТ: {} -> срок выполнения установлен: {} дней, пользователь: {}", getSiteName(), defaultDays, login);
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ ЗАПОЛНИТЬ СРОК ВЫПОЛНЕНИЯ, пользователь: {}, ошибка: {}", getSiteName(), login, e.getMessage());
            return false;
        }

        if (!waitOrFail(page, "#el-cost_from", 8000, "Поле цены")) {
            log.warn("АВТООТВЕТ: {} -> НЕ НАЙДЕНО ПОЛЕ ЦЕНЫ, пользователь: {}", getSiteName(), login);
            return false;
        }

        try {
            String price = String.valueOf(defaultPrice);
            page.fill("#el-cost_from", price);
            log.info("АВТООТВЕТ: {} -> цена установлена: {}, пользователь: {}", getSiteName(), defaultPrice, login);
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ ЗАПОЛНИТЬ ЦЕНУ, пользователь: {}, ошибка: {}", getSiteName(), login, e.getMessage());
            return false;
        }

        if (!waitOrFail(page, "#el-submit", 8000, "Кнопка отправки отклика")) {
            log.warn("АВТООТВЕТ: {} -> НЕ НАЙДЕНА КНОПКА ОТПРАВКИ ОТКЛИКА, пользователь: {}", getSiteName(), login);
            return false;
        }
        log.info("АВТООТВЕТ: {} -> кнопка отправки найдена, пользователь: {}", getSiteName(), login);

        Locator sendBtn = page.locator("#el-submit");
        try {
            page.waitForCondition(sendBtn::isEnabled,
                    new Page.WaitForConditionOptions().setTimeout(5000));
            log.info("АВТООТВЕТ: {} -> кнопка отправки активна, пользователь: {}", getSiteName(), login);
        } catch (Exception e) {
            log.warn("АВТООТВЕТ: {} -> КНОПКА ОТПРАВКИ НЕАКТИВНА, пользователь: {}, ошибка: {}", getSiteName(), login, e.getMessage());
            return false;
        }

        if (sendRequest) {
            try {
                sendBtn.click();
                log.info("АВТООТВЕТ: {} -> ЗАЯВКА УСПЕШНО ОТПРАВЛЕНА, пользователь: {}", getSiteName(), login);
            } catch (Exception e) {
                log.warn("АВТООТВЕТ: {} -> НЕ УДАЛОСЬ НАЖАТЬ КНОПКУ ОТПРАВКИ, пользователь: {}, ошибка: {}", getSiteName(), login, e.getMessage());
                return false;
            }
        } else {
            log.info("АВТООТВЕТ: {} -> ЗАЯВКА НЕ ОТПРАВЛЕНА (sendRequest=false), пользователь: {}", getSiteName(), login);
        }

        page.waitForTimeout(2000);
        log.info("АВТООТВЕТ: {} -> ОТКЛИК УСПЕШНО ЗАВЕРШЁН, пользователь: {}", getSiteName(), login);
        return true;
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.FLRU;
    }
}