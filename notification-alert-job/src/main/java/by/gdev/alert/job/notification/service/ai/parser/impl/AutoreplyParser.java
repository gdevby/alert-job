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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

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

    @Value("${autoreply.screenshots.enabled:false}")
    private boolean screenshotsEnabled;

    @Value("${autoreply.screenshots.dir:./autoreply/screenshots}")
    private String screenshotsBaseDir;

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
                log.info("АВТООТВЕТ: {} -> для пользователя {} нет закреплённого прокси, взят случайный: {}:{}",
                        getSiteName(), userUuid,
                        proxyCred != null ? proxyCred.getHost() : "null",
                        proxyCred != null ? proxyCred.getPort() : 0);
            }
            else if (proxyCred != null){
                log.info("АВТООТВЕТ: {} -> для пользователя {} используется закреплённый прокси: {}:{}",
                        getSiteName(), userUuid,
                        proxyCred.getHost(),
                        proxyCred.getPort());
            }
            else {
                log.info("АВТООТВЕТ: {} -> для пользователя {} прокси не используется (proxy=false или отсутствует)", getSiteName(), userUuid);
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
            takeScreenshot(page, getSiteName(), payload.getUser().getUuid(), "after_login");
            page.waitForTimeout(1000);
            if (!processAutoReply(page, payload, creds)) {
                log.warn("Автоответ НЕ отправлен пользователем {}", creds.login());
                return false;
            }

            log.info("Автоответ успешно отправлен пользователем {}", creds.login());
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

    /**
     * Сохраняет скриншот текущей страницы в структурированную папку.
     *
     * @param page     объект страницы Playwright
     * @param site     имя сайта (из перечисления SiteName)
     * @param userUuid идентификатор пользователя
     * @param step     название шага (например, "after_login", "order_page", "form_filled")
     */
        protected void takeScreenshot(Page page, SiteName site, String userUuid, String step) {
        if (!screenshotsEnabled) {
            return;
        }
        try {
            // Формируем путь: base/siteName/yyyy-MM-dd/userUuid/timestamp_uuid/
            String dateStr = LocalDate.now().toString();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss_SSS"));
            String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
            String timeDir = timestamp + "_" + uniqueSuffix;

            Path dir = Paths.get(screenshotsBaseDir)
                    .resolve(site.name())
                    .resolve(dateStr)
                    .resolve(userUuid)
                    .resolve(timeDir);

            Files.createDirectories(dir);
            Path file = dir.resolve(step + ".png");

            page.screenshot(new Page.ScreenshotOptions().setPath(file));
            log.info("Скриншот сохранён: {}", file.toAbsolutePath());
        } catch (Exception e) {
            log.warn("Не удалось сохранить скриншот для шага '{}': {}", step, e.getMessage());
        }
    }

    protected abstract boolean login(Page page, DecryptedCredential creds);

    protected abstract boolean processAutoReply(Page page, AiNotificationPayload payload, DecryptedCredential creds);

    protected abstract SiteName getSiteName();

}
