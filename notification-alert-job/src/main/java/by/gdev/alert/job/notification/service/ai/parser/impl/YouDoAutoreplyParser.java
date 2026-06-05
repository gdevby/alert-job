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

        // Кнопка "Войти"
        if (!clickOrFail(page, "span[data-test='LoginButton']", 8000, "Кнопка 'Войти'"))
            return false;

        // Кнопка "Войти через электронную почту"
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

        // Кнопка "Далее"
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
        page.waitForTimeout(30000); //!!искусственная задержка для обновления кодов
        otpService.invalidateOtp(SiteName.YOUDO.name(), creds.login());
        return true;
    }


    @Override
    protected boolean processAutoReply(Page page, AiNotificationPayload payload) {
        String link = payload.getOrder().getLink();
        log.debug("Переход на заказ: {}", link);

        try {
            page.navigate(link);
            page.waitForLoadState(LoadState.NETWORKIDLE);
        } catch (Exception e) {
            log.warn("Не удалось открыть заказ {}", link);
            return false;
        }

        // Кнопка "Откликнуться"
        if (!clickOrFail(page, "button:has-text('Откликнуться')", 8000, "Кнопка отклика"))
            return false;

        //Дальше не проверено!! - нет соответствующего акк
        // Форма
        if (!waitOrFail(page, "textarea[name='Message']", 8000, "Форма отклика"))
            return false;

        // Текст
        try {
            page.fill("textarea[name='Message']", payload.getDecision().reply());
        } catch (Exception e) {
            log.warn("Не удалось заполнить текст ответа");
            return false;
        }

        // Цена
        try {
            page.fill("input[name='Price']", "500");
        } catch (Exception e) {
            log.warn("Не удалось заполнить цену");
            return false;
        }

        // Срок
        try {
            page.fill("input[name='ExecutionTime']", "1");
        } catch (Exception e) {
            log.warn("Не удалось заполнить срок выполнения");
            return false;
        }

        // Кнопка отправки
        if (!waitOrFail(page, "button:has-text('Отправить')", 8000, "Кнопка отправки"))
            return false;

        Locator sendBtn = page.locator("button:has-text('Отправить')");
        try {
            page.waitForCondition(sendBtn::isEnabled);
        } catch (Exception e) {
            log.warn("Кнопка 'Отправить' не активна");
            return false;
        }

        if (sendRequest) {
            try {
                sendBtn.click();
            } catch (Exception e) {
                log.warn("Не удалось нажать кнопку 'Отправить'");
                return false;
            }
        }

        page.waitForTimeout(5000);
        log.debug("Заявка успешно отправлена");
        return true;
    }
}
