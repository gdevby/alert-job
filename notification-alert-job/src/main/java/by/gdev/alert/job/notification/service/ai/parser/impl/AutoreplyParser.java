package by.gdev.alert.job.notification.service.ai.parser.impl;

import by.gdev.alert.job.notification.model.dto.AiNotificationPayload;
import by.gdev.alert.job.notification.model.dto.DecryptedCredential;
import by.gdev.alert.job.notification.service.ai.proxy.AssignedProxyService;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepResult;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepType;
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

    @Value("${parser.autoreply.default.price:1000}")
    protected int defaultPrice;

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

    public final StepResult<Void> sendAutoreply(DecryptedCredential creds, AiNotificationPayload payload) {
        Playwright playwright = null;
        Browser browser = null;
        BrowserContext context = null;
        Page page = null;
        try {
            playwright = playwrightManager.createPlaywright();
            String userUuid = payload.getUser().getUuid();
            ProxyCredentials proxyCred = assignedProxyService.getProxyForUserAndModule(userUuid, payload.getModule().getId());

            if (proxyCred == null && proxy) {
                proxyCred = playwrightManager.getProxyWithRetry(3, 500);
                log.info("АВТООТВЕТ: {} -> для пользователя {} нет закреплённого прокси, взят случайный: {}:{}",
                        getSiteName(), userUuid,
                        proxyCred != null ? proxyCred.getHost() : "null",
                        proxyCred != null ? proxyCred.getPort() : 0);
            } else if (proxyCred != null) {
                log.info("АВТООТВЕТ: {} -> для пользователя {} используется закреплённый прокси: {}:{}",
                        getSiteName(), userUuid, proxyCred.getHost(), proxyCred.getPort());
            } else {
                log.info("АВТООТВЕТ: {} -> для пользователя {} прокси не используется (proxy=false или отсутствует)", getSiteName(), userUuid);
            }

            browser = playwrightManager.createBrowser(playwright, proxyCred, headless, proxy, getSiteName());
            context = playwrightManager.createBrowserContext(browser, proxyCred, proxy, getSiteName());
            page = context.newPage();

            StepResult<Void> loginResult = login(page, payload, creds);
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
            playwrightManager.closeResources(page, context, browser, playwright, getSiteName());
        }
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

    protected byte[] captureScreenshot(Page page) {
        try {
            return page.screenshot();
        } catch (Exception e) {
            log.warn("Не удалось сделать скриншот: {}", e.getMessage());
            return null;
        }
    }

    protected void takeScreenshot(Page page, SiteName site, String userUuid, String step) {
        if (!screenshotsEnabled) {
            log.info("Сохранение скриншотов для отладочной информации отключено");
            return;
        }
        try {
            byte[] screenshotBytes = captureScreenshot(page);
            if (screenshotBytes == null || screenshotBytes.length == 0) {
                log.warn("Не удалось получить скриншот для шага '{}'", step);
                return;
            }

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
            Files.write(file, screenshotBytes);
            log.info("Скриншот сохранён: {}", file.toAbsolutePath());
        } catch (Exception e) {
            log.warn("Не удалось сохранить скриншот для шага '{}': {}", step, e.getMessage());
        }
    }

    protected void setOpt(AiNotificationPayload payload, String otp, boolean used){
        payload.setOtpUsed(used);
        payload.setOtpValue(otp);
    }

    protected abstract StepResult<Void> login(Page page, AiNotificationPayload payload, DecryptedCredential creds);

    protected abstract StepResult<Void> processAutoReply(Page page, AiNotificationPayload payload, DecryptedCredential creds);

    protected abstract SiteName getSiteName();
}