package by.gdev.alert.job.notification.service.ai.parser.impl;

import by.gdev.alert.job.notification.model.dto.AiNotificationPayload;
import by.gdev.alert.job.notification.model.dto.DecryptedCredential;
import by.gdev.alert.job.notification.service.ai.proxy.AssignedProxyService;
import by.gdev.common.model.SiteName;
import by.gdev.common.model.proxy.ProxyCredentials;
import by.gdev.common.service.playwright.PlaywrightManager;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
public abstract class AutoreplyParser {
    protected boolean headless;
    protected boolean sendRequest;
    protected boolean proxy;

    protected PlaywrightManager playwrightManager;

    /** Значение по умолчанию для цены */
    @Value("${parser.autoreply.default.price:1000}")
    protected int defaultPrice;

    /** Значение по умолчанию для срока выполнения (в днях) */
    @Value("${parser.autoreply.default.days:1}")
    protected int defaultDays;

    protected AssignedProxyService assignedProxyService;

    protected AutoreplyParser(PlaywrightManager playwrightManager, AssignedProxyService assignedProxyService) {
        this.playwrightManager = playwrightManager;
        this.assignedProxyService = assignedProxyService;
    }

    public final boolean sendAutoreply(DecryptedCredential creds, AiNotificationPayload payload) {
        Playwright playwright = null;
        Browser browser = null;
        BrowserContext context = null;
        Page page = null;
        try {
            playwright = playwrightManager.createPlaywright();
            String userUuid = payload.getUser().getUuid();
            ProxyCredentials proxyCred = assignedProxyService.getProxyForUser(userUuid);

            // Если закреплённого нет, но proxy=true – пробуем взять случайный
            if (proxyCred == null && proxy) {
                proxyCred = playwrightManager.getProxyWithRetry(3, 500);
                log.debug("Для пользователя {} нет закреплённого прокси, взят случайный", userUuid);
            }

            browser = playwrightManager.createBrowser(
                    playwright,
                    proxyCred,
                    headless,
                    proxy,
                    getSiteName()
            );

            context = playwrightManager.createBrowserContext(browser, proxyCred, proxy, getSiteName());
            page = context.newPage();

            if (!login(page, creds)) {
                log.warn("Логин не выполнен для {}", creds.login());
                return false;
            }
            page.waitForTimeout(1000);
            if (!processAutoReply(page, payload)) {
                log.warn("Автоответ НЕ отправлен пользователем {}", creds.login());
                return false;
            }

            log.debug("Автоответ успешно отправлен пользователем {}", creds.login());
            return true;

        } catch (Exception e) {
            log.error("Ошибка при отправке автоответа", e);
            return false;

        } finally {
            playwrightManager.closeResources(page, context, browser, playwright, getSiteName());
        }
    }

    void safeNavigate(Page page, String url) {
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

    protected abstract boolean login(Page page, DecryptedCredential creds);

    protected abstract boolean processAutoReply(Page page, AiNotificationPayload payload);

    protected abstract SiteName getSiteName();

}
