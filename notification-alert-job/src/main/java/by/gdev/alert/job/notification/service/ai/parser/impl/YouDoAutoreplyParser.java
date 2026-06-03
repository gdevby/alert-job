package by.gdev.alert.job.notification.service.ai.parser.impl;

import by.gdev.alert.job.notification.model.dto.AiNotificationPayload;
import by.gdev.alert.job.notification.model.dto.DecryptedCredential;
import by.gdev.alert.job.notification.service.ai.parser.AutoreplyPlaywrightParser;
import com.microsoft.playwright.options.WaitUntilState;
import by.gdev.common.model.SiteName;
import by.gdev.common.model.proxy.ProxyCredentials;
import by.gdev.common.service.playwright.PlaywrightManager;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import by.gdev.alert.job.notification.service.ai.otp.OtpService;

@Slf4j
@Component
@RequiredArgsConstructor
public class YouDoAutoreplyParser extends AutoreplyParser implements AutoreplyPlaywrightParser {

    private final PlaywrightManager playwrightManager;
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

    @Override
    public SiteName getSiteName() {
        return SiteName.YOUDO;
    }

    @Override
    public boolean sendAutoreply(DecryptedCredential creds, AiNotificationPayload payload) {

        Playwright playwright = null;
        Browser browser = null;
        BrowserContext context = null;
        Page page = null;

        try {
            playwright = playwrightManager.createPlaywright();

            ProxyCredentials proxyCred = null;
            if (proxy) {
                proxyCred = playwrightManager.getProxyWithRetry(3, 500);
            }

            browser = playwrightManager.createBrowser(
                    playwright,
                    proxyCred,
                    headless,
                    proxy,
                    getSiteName().name()
            );
            context = playwrightManager.createBrowserContext(browser, null, false, getSiteName().name());
            page = context.newPage();

            login(page, creds);
            page.waitForTimeout(3000);

            processAutoReply(page, payload);
            page.waitForTimeout(15000);

            log.debug("Автоответ успешно отправлен пользователем {}", creds.login());
            return true;

        } catch (Exception e) {
            log.error("Ошибка при отправке автоответа", e);
            return false;

        } finally {
            playwrightManager.closeResources(page, context, browser, playwright, getSiteName().name());
        }
    }

    private void login(Page page, DecryptedCredential creds) {
        // Открываем главную через safeNavigate
        safeNavigate(page, "https://youdo.com/");
        // Кликаем "Войти"
        Locator loginBtn = page.locator("span[data-test='LoginButton']");
        page.waitForCondition(loginBtn::isVisible);
        loginBtn.click();
        // Кликаем "Войти через электронную почту"
        Locator loginEmailBtn = page.locator("span[data-test='LoginWithEmailButton']");
        page.waitForCondition(loginEmailBtn::isVisible);
        loginEmailBtn.click();
        // Вводим email
        Locator emailInput = page.locator("input[name='login']");
        page.waitForCondition(emailInput::isVisible);
        emailInput.fill(creds.login());
        // Жмём "Далее"
        Locator nextBtn = page.locator("button:has-text('Далее')");
        page.waitForCondition(nextBtn::isEnabled);
        nextBtn.click();
        //Ждём поле ввода кода
        Locator codeInput = page.locator("input[name='code']");
        page.waitForCondition(codeInput::isVisible);
        // Берём OTP из OtpService
        String otp = otpService.getOtp(SiteName.YOUDO.name(), creds.login());
        log.debug("Используем OTP={} для входа в YouDo", otp);
        codeInput.fill(otp);
        page.waitForLoadState(LoadState.NETWORKIDLE);
        log.debug("Успешный вход в аккаунт {}", creds.login());
    }

    private void safeNavigate(Page page, String url) {
        for (int i = 1; i <= 5; i++) {
            try {
                page.navigate(url, new Page.NavigateOptions()
                        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
                return;
            } catch (PlaywrightException e) {
                log.warn("Навигация не удалась (попытка {}): {}", i, e.getMessage());
                page.waitForTimeout(1500);
            }
        }
        throw new RuntimeException("Не удалось открыть страницу после 5 попыток: " + url);
    }

    private void processAutoReply(Page page, AiNotificationPayload payload) {

        String link = payload.getOrder().getLink();
        log.info("Переход на заказ: {}", link);

        page.navigate(link);
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Кнопка "Откликнуться"
        Locator replyBtn = page.locator("button:has-text('Откликнуться')");
        page.waitForCondition(replyBtn::isVisible);
        replyBtn.click();

        // Ждём появления формы
        page.waitForSelector("textarea[name='Message']");

        // Текст автоответа
        String reply = payload.getDecision().reply();
        page.fill("textarea[name='Message']", reply);

        // Цена
        page.fill("input[name='Price']", "500");

        // Срок выполнения
        page.fill("input[name='ExecutionTime']", "1");

        // Кнопка "Отправить"
        Locator sendBtn = page.locator("button:has-text('Отправить')");
        page.waitForCondition(sendBtn::isEnabled);

        if (sendRequest) {
            sendBtn.click();
        }

        page.waitForTimeout(5000);
        log.debug("Заявка успешно отправлена");
    }
}
