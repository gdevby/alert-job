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

    @Value("${parser.autoreply.headless.fl.ru}")
    private void setHeadless(boolean headless) {
        this.headless = headless;
    }

    @Value("${parser.autoreply.proxy.fl.ru}")
    private void setProxy(boolean proxy) {
        this.proxy = proxy;
    }

    @Value("${parser.autoreply.send.request.fl.ru}")
    private void setOnSendRequest(boolean sendRequest) {
        this.sendRequest = sendRequest;
    }

    public FlRuAutoreplyParser(PlaywrightManager playwrightManager, AssignedProxyService assignedProxyService, CaptchaService captchaService) {
        super(playwrightManager, assignedProxyService);
        this.captchaService = captchaService;
    }

    @Override
    protected boolean login(Page page, DecryptedCredential creds) {
        try {
            // Открываем страницу логина
            safeNavigate(page, "https://www.fl.ru/account/login/");
            // Ждём поле логина
            if (!waitOrFail(page, "input[name='username']", 8000, "Поле логина"))
                return false;
            // Вводим логин
            try {
                page.fill("input[name='username']", creds.login());
            } catch (Exception e) {
                log.warn("Не удалось заполнить логин");
                return false;
            }
            // Вводим пароль
            try {
                page.fill("input[name='password']", creds.password());
            } catch (Exception e) {
                log.warn("Не удалось заполнить пароль");
                return false;
            }
            // Пытаемся пройти SmartCaptcha
            if (!captchaService.solveYandexSmartCaptcha(page)) {
                log.warn("{}: SmartCaptcha не пройдена", getSiteName());
                return false;
            }
            // Жмём кнопку Войти
            if (!clickOrFail(page, "#submit-button", 8000, "Кнопка 'Войти'"))
                return false;
            // Ждём загрузку
            try {
                page.waitForLoadState(LoadState.NETWORKIDLE);
            } catch (Exception e) {
                log.warn("Не удалось дождаться загрузки после входа");
                return false;
            }
            // Проверяем, что логин успешный
            if (page.url().contains("/account/login")) {
                log.warn("{}: вход не выполнен, остались на странице логина", getSiteName());
                return false;
            }
            log.debug("{}: успешный вход в аккаунт {}", getSiteName(), creds.login());
            return true;
        } catch (Exception e) {
            log.error("{}: ошибка при логине", getSiteName(), e);
            return false;
        }
    }

    @Override
    protected boolean processAutoReply(Page page, AiNotificationPayload payload) {
        String link = payload.getOrder().getLink();
        log.debug("{}: переход на заказ {}", getSiteName(), link);
        //Переходим на заказ
        try {
            page.navigate(link);
            page.waitForLoadState(LoadState.NETWORKIDLE);
        } catch (Exception e) {
            log.warn("{}: не удалось открыть заказ {}", getSiteName(), link);
            return false;
        }
        //Ждём textarea отклика
        if (!waitOrFail(page, "#el-descr", 8000, "Поле текста отклика"))
            return false;
        //Вставляем текст
        try {
            page.fill("#el-descr", payload.getDecision().reply());
        } catch (Exception e) {
            log.warn("{}: не удалось заполнить текст отклика", getSiteName());
            return false;
        }
        //Ставим галку На банковскую карту физ. лица
        try {
            Locator payRadio = page.locator("label[for='el-pay-0']");
            if (payRadio.count() > 0) {
                payRadio.click();
            } else {
                log.warn("{}: радиокнопка оплаты не найдена", getSiteName());
            }
        } catch (Exception e) {
            log.warn("{}: ошибка при выборе способа оплаты", getSiteName());
        }
        //Срок выполнения
        if (!waitOrFail(page, "#el-time_from", 8000, "Поле срока выполнения"))
            return false;
        try {
            String duration = String.valueOf(defaultDays);
            page.fill("#el-time_from", duration);
        } catch (Exception e) {
            log.warn("{}: не удалось заполнить срок выполнения", getSiteName());
            return false;
        }
        //Цена
        if (!waitOrFail(page, "#el-cost_from", 8000, "Поле цены"))
            return false;
        try {
            String price = String.valueOf(defaultPrice);
            page.fill("#el-cost_from", price);
        } catch (Exception e) {
            log.warn("{}: не удалось заполнить цену", getSiteName());
            return false;
        }
        //Кнопка Отправить отклик
        if (!waitOrFail(page, "#el-submit", 8000, "Кнопка отправки отклика"))
            return false;
        Locator sendBtn = page.locator("#el-submit");
        //Проверяем, что кнопка активна
        try {
            page.waitForCondition(sendBtn::isEnabled,
                    new Page.WaitForConditionOptions().setTimeout(5000));
        } catch (Exception e) {
            log.warn("{}: кнопка отправки не активна", getSiteName());
            return false;
        }
        //Отправляем отклик
        if (sendRequest) {
            try {
                sendBtn.click();
            } catch (Exception e) {
                log.warn("{}: не удалось нажать кнопку отправки", getSiteName());
                return false;
            }
        }
        page.waitForTimeout(2000);
        log.debug("{}: отклик успешно отправлен", getSiteName());
        return true;
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.FLRU;
    }
}
