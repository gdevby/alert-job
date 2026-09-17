package by.gdev.alert.job.notification.service.ai.parser.impl;

import by.gdev.alert.job.notification.model.AutoreplyMode;
import by.gdev.alert.job.notification.model.dto.AiNotificationPayload;
import by.gdev.alert.job.notification.model.dto.DecryptedCredential;
import by.gdev.alert.job.notification.service.ai.parser.debug.AutoreplyReporter;
import by.gdev.alert.job.notification.service.ai.parser.debug.ScreenshotService;
import by.gdev.alert.job.notification.service.ai.proxy.AssignedProxyService;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepResult;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepType;
import by.gdev.common.model.SiteName;
import by.gdev.common.model.proxy.ProxyCredentials;
import by.gdev.common.service.playwright.manager.BrowserLaunchOptions;
import by.gdev.common.service.playwright.manager.PlaywrightBrowserManager;
import by.gdev.common.service.playwright.manager.PlaywrightManagerResolver;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;


import org.slf4j.Logger;

@Slf4j
public abstract class AutoreplyParser {

    //опции автоответа
    //видимость браузера
    protected boolean headless;
    //прожимать ли кнопку отправки ответа на заказ
    protected boolean sendRequest;
    //использовать ли прокси для запуска браузера
    protected boolean proxy;

    @Value("${parser.autoreply.default.price:1000}")
    protected int defaultPrice;

    @Value("${parser.autoreply.default.days:1}")
    protected int defaultDays;

    protected AssignedProxyService assignedProxyService;

    @Autowired
    protected ScreenshotService screenshotService;

    @Autowired
    protected AutoreplyReporter reporter;

    @Autowired
    protected PlaywrightManagerResolver managerResolver;

    protected AutoreplyParser(AssignedProxyService assignedProxyService) {
        this.assignedProxyService = assignedProxyService;
    }

    public final StepResult<Void> sendAutoreply(DecryptedCredential creds, AiNotificationPayload payload,
                                                AutoreplyMode autoreplyMode) {
        Playwright playwright = null;
        Browser browser = null;
        BrowserContext context = null;
        Page page = null;

        PlaywrightBrowserManager manager = managerResolver.resolve(getSiteName());
        BrowserLaunchOptions options = buildLaunchOptions(getSiteName(), payload);

        try {
            playwright = manager.createPlaywright();
            browser = manager.createBrowser(playwright, options, getSiteName());
            context = manager.createBrowserContext(browser, options, getSiteName());

            page = context.newPage();

            StepResult<Void> loginResult = login(page, payload, creds, autoreplyMode);
            if (autoreplyMode.equals(AutoreplyMode.LOGIN_ONLY)) {
                return loginResult;
            }
            if (loginResult.failed()) {
                log.warn("Логин не выполнен для {}", creds.login());
                return loginResult;
            }
            takeScreenshot(page, getSiteName(), payload.getUser().getUuid(), "after_login");
            page.waitForTimeout(1000);

            StepResult<Void> processResult = processAutoReply(page, payload, creds);
            if (processResult.failed()) {
                log.warn("Автоответ НЕ отправлен пользователем {}", creds.login());
                return processResult;
            }

            log.info("Автоответ успешно отправлен пользователем {}", creds.login());
            return StepResult.ok(StepType.SEND_AUTOREPLY, null);

        } catch (Exception e) {
            log.error("Ошибка при отправке автоответа", e);
            byte[] screenshot = page != null ? captureScreenshot(page) : null;
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Необработанная ошибка: " + e.getMessage(), screenshot);

        } finally {
            manager.closeResources(page, context, browser, playwright, getSiteName());
        }
    }

    protected PlaywrightBrowserManager getCurrentManager() {
        return managerResolver.resolve(getSiteName());
    }

    void safeNavigate(Page page, String url) {
        for (int i = 1; i <= 5; i++) {
            try {
                page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
                return;
            } catch (PlaywrightException e) {
                log.warn("Навигация не удалась (попытка {}): {}", i, e.getMessage());
                page.waitForTimeout(1500);
            }
        }
        throw new RuntimeException("Не удалось открыть страницу после 5 попыток: " + url);
    }

    boolean waitOrFail(Page page, String selector, int timeoutMs, String step) {
        try {
            page.waitForSelector(selector, new Page.WaitForSelectorOptions().setTimeout(timeoutMs));
            return true;
        } catch (Exception e) {
            log.warn("TIMEOUT at step '{}': selector '{}' not found within {} ms", step, selector, timeoutMs);
            return false;
        }
    }

    boolean clickOrFail(Page page, String selector, int timeoutMs, String step) {
        if (!waitOrFail(page, selector, timeoutMs, step)) return false;
        try {
            page.locator(selector).click();
            return true;
        } catch (Exception e) {
            log.warn("CLICK FAILED at step '{}': selector '{}'", step, selector);
            return false;
        }
    }

    /**
     * Формирует опции запуска браузера для конкретного сайта.
     * <p>
     * Прокси резолвится только когда он реально нужен:
     * <ul>
     *     <li>используется локальный Playwright (не Camoufox)</li>
     *     <li>и флаг {@code proxy=true} для этого сайта</li>
     * </ul>
     * Для Camoufox прокси задан на Python-сервере и здесь игнорируется.
     */
    protected BrowserLaunchOptions buildLaunchOptions(SiteName site, AiNotificationPayload payload) {
        if (!proxy) {
            return new BrowserLaunchOptions(null, headless, false);
        }
        ProxyCredentials proxyCred = assignedProxyService.getProxyForUserAndModule(
                payload.getUser().getUuid(),
                payload.getModule().getId()
        );
        return new BrowserLaunchOptions(proxyCred, headless, true);
    }
    protected void setOtp(AiNotificationPayload payload, String otp, boolean used){
        payload.setOtpUsed(used);
        payload.setOtpValue(otp);
    }

    protected void report(Level level, Logger logger, String message, String errorType) {
        reporter.report(level, logger, getSiteName(), message, errorType);
    }

    protected byte[] captureScreenshot(Page page) {
        return screenshotService.capture(page);
    }

    protected void takeScreenshot(Page page, SiteName site, String userUuid, String step) {
        screenshotService.take(page, site, userUuid, step);
    }

    protected abstract StepResult<Void> login(Page page, AiNotificationPayload payload, DecryptedCredential creds, AutoreplyMode mode);

    protected abstract StepResult<Void> processAutoReply(Page page, AiNotificationPayload payload, DecryptedCredential creds);

    protected abstract SiteName getSiteName();
}