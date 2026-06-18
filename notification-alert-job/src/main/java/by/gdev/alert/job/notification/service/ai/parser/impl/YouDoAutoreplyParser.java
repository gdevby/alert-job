package by.gdev.alert.job.notification.service.ai.parser.impl;

import by.gdev.alert.job.notification.model.dto.AiNotificationPayload;
import by.gdev.alert.job.notification.model.dto.DecryptedCredential;
import by.gdev.alert.job.notification.service.ai.parser.AutoreplyPlaywrightParser;
import by.gdev.common.model.SiteName;
import by.gdev.common.service.playwright.PlaywrightManager;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import by.gdev.alert.job.notification.service.ai.otp.OtpService;

@Slf4j
@Component
public class YouDoAutoreplyParser extends AutoreplyParser implements AutoreplyPlaywrightParser {

    private final OtpService otpService;

    @Value("${parser.autoreply.headless.youdo.com}")
    private void setHeadless(boolean headless) {
        this.headless = headless;
    }

    @Value("${parser.autoreply.proxy.youdo.com}")
    private void setProxy(boolean proxy) {
        this.proxy = proxy;
    }

    @Value("${parser.autoreply.send.request.youdo.com}")
    private void setOnSendRequest(boolean sendRequest) {
        this.sendRequest = sendRequest;
    }

    public YouDoAutoreplyParser(PlaywrightManager playwrightManager, OtpService otpService) {
        super(playwrightManager);
        this.otpService = otpService;
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.YOUDO;
    }

    @Override
    protected boolean login(Page page, DecryptedCredential creds) {
        // Навигация
        try {
            safeNavigate(page, "https://youdo.com/");
        } catch (Exception e) {
            log.warn("Не удалось открыть главную страницу YouDo");
            return false;
        }

        // Кнопка Войти
        if (!clickOrFail(page, "span[data-test='LoginButton']", 8000, "Кнопка 'Войти'"))
            return false;

        // Кнопка Войти через электронную почту
        if (!clickOrFail(page, "span[data-test='LoginWithEmailButton']", 8000, "Войти через email"))
            return false;

        // Поле email
        if (!waitOrFail(page, "input[name='login']", 8000, "Поле email"))
            return false;

        try {
            page.fill("input[name='login']", creds.login());
        } catch (Exception e) {
            log.warn("Не удалось заполнить email");
            return false;
        }

        // Кнопка Далее
        if (!clickOrFail(page, "button:has-text('Далее')", 8000, "Кнопка 'Далее'"))
            return false;

        // Поле ввода кода
        if (!waitOrFail(page, "input[name='code']", 15000, "Поле ввода кода"))
            return false;

        // Получаем OTP
        String otp = otpService.waitForOtp(SiteName.YOUDO.name(), creds.login(), 120_000);
        if (otp == null) {
            log.warn("OTP не получен за отведённое время");
            return false;
        }

        try {
            page.fill("input[name='code']", otp);
        } catch (Exception e) {
            log.warn("Не удалось заполнить OTP");
            return false;
        }
        // Ждём загрузку
        try {
            page.waitForLoadState(LoadState.NETWORKIDLE);
        } catch (Exception e) {
            log.warn("Не удалось дождаться загрузки после ввода OTP");
            return false;
        }

        log.debug("Успешный вход в аккаунт {}", creds.login());
        page.waitForTimeout(30000); //!!искусственная задержка 30c для обновления кодов
        otpService.invalidateOtp(SiteName.YOUDO.name(), creds.login());
        return true;
    }

    @Override
    protected boolean processAutoReply(Page page, AiNotificationPayload payload) {
        String link = payload.getOrder().getLink();
        log.debug("Переход на заказ: {}", link);

        // Переходим на заказ
        try {
            page.navigate(link);
            page.waitForLoadState(LoadState.NETWORKIDLE);
        } catch (Exception e) {
            log.warn("Не удалось открыть заказ {}", link);
            return false;
        }

        // Кнопка Откликнуться
        if (!clickOrFail(page, "button:has-text('Откликнуться')", 8000, "Кнопка 'Откликнуться'")) return false;

        // Ждём поле цены
        if (!waitOrFail(page, "input[placeholder='В рублях']", 8000, "Поле цены")) return false;

        // Заполняем цену
        try {
            page.fill("input[placeholder='В рублях']", String.valueOf(defaultPrice));
        } catch (Exception e) {
            log.warn("Не удалось заполнить цену");
            return false;
        }

        // Ждём textarea
        if (!waitOrFail(page, "textarea.Textarea_textarea__FjgmX", 8000, "Поле текста отклика")) return false;

        // Заполняем текст отклика
        try {
            page.fill("textarea.Textarea_textarea__FjgmX", payload.getDecision().reply());
        } catch (Exception e) {
            log.warn("Не удалось заполнить текст отклика");
            return false;
        }

        // Ждём кнопку Далее
        if (!waitOrFail(page, "button.NewButton_button__2D_5n:has-text('Далее')", 8000, "Кнопка 'Далее'")) return false;
        Locator nextBtn = page.locator("button.NewButton_button__2D_5n:has-text('Далее')");

        // Проверяем, что кнопка активна
        try {
            page.waitForCondition(nextBtn::isEnabled,
                    new Page.WaitForConditionOptions().setTimeout(5000));
        } catch (Exception e) {
            log.warn("Кнопка 'Далее' не активна");
            return false;
        }

        // Нажимаем Далее
        if (sendRequest) {
            try {
                nextBtn.click();
            } catch (Exception e) {
                log.warn("Не удалось нажать кнопку 'Далее'");
                return false;
            }
            log.debug("Отклик отправлен (кнопка 'Далее' нажата)");
        } else {
            log.debug("sendRequest=false → кнопку 'Далее' НЕ нажимаем");
        }

        page.waitForTimeout(3000);
        return true;
    }
}
