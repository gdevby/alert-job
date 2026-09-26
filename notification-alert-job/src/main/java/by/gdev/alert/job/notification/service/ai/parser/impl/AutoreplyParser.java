package by.gdev.alert.job.notification.service.ai.parser.impl;

import by.gdev.alert.job.notification.model.AutoreplyMode;
import by.gdev.alert.job.notification.model.dto.AiNotificationPayload;
import by.gdev.alert.job.notification.model.dto.DecryptedCredential;
import by.gdev.alert.job.notification.service.ai.parser.debug.AutoreplyReporter;
import by.gdev.alert.job.notification.service.ai.parser.debug.ScreenshotService;
import by.gdev.alert.job.notification.service.ai.proxy.AssignedProxyService;
import by.gdev.alert.job.notification.service.ai.sessions.AutoreplySessionVerifierFactory;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepResult;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepType;
import by.gdev.common.model.SiteName;
import by.gdev.common.model.proxy.ProxyCredentials;
import by.gdev.common.service.playwright.sessions.storage.SessionStorage;
import by.gdev.common.service.playwright.manager.BrowserLaunchOptions;
import by.gdev.common.service.playwright.manager.PlaywrightBrowserManager;
import by.gdev.common.service.playwright.manager.PlaywrightManagerResolver;
import by.gdev.common.service.playwright.manager.impl.PlaywrightCamoufoxManager;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.slf4j.Logger;

import java.nio.file.Path;

@Slf4j
public abstract class AutoreplyParser {

    protected boolean headless;
    protected boolean sendRequest;
    protected boolean proxy;

    @Value("${parser.autoreply.default.price:1000}")
    protected int defaultPrice;

    @Value("${parser.autoreply.default.days:1}")
    protected int defaultDays;

    @Value("${parser.autoreply.post-login.pause-ms:0}")
    protected long postLoginPauseMs;

    protected AssignedProxyService assignedProxyService;

    @Autowired
    protected ScreenshotService screenshotService;

    @Autowired
    protected AutoreplyReporter reporter;

    @Autowired
    protected SessionStorage sessionStorage;

    @Autowired
    protected PlaywrightManagerResolver managerResolver;

    @Autowired
    protected AutoreplySessionVerifierFactory sessionVerifierFactory;

    protected AutoreplyParser(AssignedProxyService assignedProxyService) {
        this.assignedProxyService = assignedProxyService;
    }

    /** Сколько раз открывать браузер заново (смена прокси между попытками — в наследнике). */
    protected int autoreplyProxySwitchMaxAttempts() {
        return 1;
    }

    protected boolean shouldRetryAutoreplyWithNewProxy(StepResult<Void> result) {
        return false;
    }

    protected void prepareNextAutoreplyProxyAttempt(AiNotificationPayload payload, int nextAttempt) {
    }

    protected void onAutoreplyAttemptStarted(int attempt) {
    }

    /** Не логиниться в том же браузере (например DDoS — нужен другой прокси). */
    protected boolean skipLoginAfterVerifyFailure(StepResult<Void> verifyResult) {
        return false;
    }

    public final StepResult<Void> sendAutoreply(DecryptedCredential creds, AiNotificationPayload payload,
                                                AutoreplyMode autoreplyMode) {
        int maxAttempts = Math.max(1, autoreplyProxySwitchMaxAttempts());
        StepResult<Void> last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            onAutoreplyAttemptStarted(attempt);
            if (attempt > 1) {
                prepareNextAutoreplyProxyAttempt(payload, attempt);
                log.warn("АВТООТВЕТ: {} -> повтор после DDoS/защиты, попытка {}/{}",
                        getSiteName(), attempt, maxAttempts);
            }
            last = runAutoreplyOnce(creds, payload, autoreplyMode);
            if (!last.failed() || attempt >= maxAttempts || !shouldRetryAutoreplyWithNewProxy(last)) {
                return last;
            }
        }
        return last;
    }

    private StepResult<Void> runAutoreplyOnce(DecryptedCredential creds, AiNotificationPayload payload,
                                              AutoreplyMode autoreplyMode) {
        Playwright playwright = null;
        Browser browser = null;
        BrowserContext context = null;
        Page page = null;

        PlaywrightBrowserManager manager = managerResolver.resolve(getSiteName());
        BrowserLaunchOptions options = buildLaunchOptions(getSiteName(), payload);
        String siteName = getSiteName().name();
        String userUuid = payload.getUser().getUuid();
        String login = creds.login();
        boolean camoufox = manager instanceof PlaywrightCamoufoxManager;

        try {
            playwright = manager.createPlaywright();
            browser = manager.createBrowser(playwright, options, getSiteName());

            boolean hasSession = sessionStorage.hasSession(userUuid, siteName, login);
            log.info("SESSION: location={} hasSession={} manager={}",
                    sessionStorage.describeSession(userUuid, siteName, login), hasSession,
                    camoufox ? "Camoufox" : "Chromium");

            Path chromiumPath = null;
            String chromiumJson = null;
            if (hasSession && !camoufox) {
                chromiumPath = sessionStorage.storageStatePath(userUuid, siteName, login).orElse(null);
                if (chromiumPath == null) {
                    chromiumJson = sessionStorage.storageStateJson(userUuid, siteName, login).orElse(null);
                }
            }
            context = manager.createBrowserContext(browser, options, getSiteName(), chromiumPath, chromiumJson);

            if (hasSession && camoufox) {
                sessionStorage.restoreCookies(context, userUuid, siteName, login);
            }

            page = context.newPage();

            if (hasSession && camoufox) {
                sessionStorage.restoreOrigins(page, userUuid, siteName, login);
            }

            sessionStorage.logContextCookies(context, sessionCookieCheckUrl(), "after-restore");

            StepResult<Void> loginResult = resolveLogin(page, payload, creds, autoreplyMode,
                    userUuid, siteName, login, hasSession);

            if (!loginResult.failed()) {
                pauseForSessionVerify(page, userUuid);
                // Строго до сохранения: пока дополнительный шаг не пройден, вход не завершён,
                // и сохранённая сессия на следующем запуске упрётся в тот же шаг.
                StepResult<Void> afterLogin = checkAfterLogin(page, creds);
                if (afterLogin != null) {
                    loginResult = afterLogin;
                }
            }

            if (!loginResult.failed()) {
                try {
                    sessionStorage.save(context, userUuid, siteName, login);
                    log.info("SESSION: save после успешного login/verify {}/{}", siteName, login);
                } catch (Exception ex) {
                    log.warn("Не удалось сохранить сессию: {}", ex.getMessage());
                }
            } else {
                log.warn("SESSION: save пропущен — login/verify не успешен {}/{}", siteName, login);
            }

            if (autoreplyMode.equals(AutoreplyMode.LOGIN_ONLY)) {
                return loginResult;
            }
            if (loginResult.failed()) {
                log.warn("Логин не выполнен для {}", login);
                return loginResult;
            }

            takeScreenshot(page, getSiteName(), userUuid, "after_login");
            page.waitForTimeout(1000);

            StepResult<Void> processResult = processAutoReply(page, payload, creds);
            if (processResult.failed()) {
                log.warn("Автоответ НЕ отправлен пользователем {}", login);
                return processResult;
            }

            log.info("Автоответ успешно отправлен пользователем {}", login);
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

    protected void humanWarmup(Page page) {
        getCurrentManager().humanMouse(page);
        getCurrentManager().humanDelay(page);
    }

    /** URL для проверки cookies после restore (домен биржи). */
    protected String sessionCookieCheckUrl() {
        return null;
    }

    protected StepResult<Void> resolveLogin(Page page, AiNotificationPayload payload,
                                            DecryptedCredential creds, AutoreplyMode autoreplyMode,
                                            String userUuid, String siteName, String login,
                                            boolean hasSession) {
        if (hasSession) {
            StepResult<Void> verified = verifyExistingSession(page, creds, autoreplyMode);
            if (!verified.failed()) {
                log.info("SESSION: verifyExistingSession OK для {}/{}", siteName, login);
                return verified;
            }
            if (skipLoginAfterVerifyFailure(verified)) {
                return verified;
            }
            log.warn("SESSION: verify не прошёл для {}/{} ({}), пробуем полный login без delete",
                    siteName, login, verified.getErrorMessage());
        }
        StepResult<Void> loginResult = login(page, payload, creds, autoreplyMode);
        if (loginResult.failed() && hasSession) {
            log.warn("SESSION: полный login не удался — удаляем устаревшую сессию {}/{}", siteName, login);
            sessionStorage.delete(userUuid, siteName, login);
        }
        return loginResult;
    }

    protected StepResult<Void> verifyExistingSession(Page page, DecryptedCredential creds, AutoreplyMode mode) {
        return sessionVerifierFactory.find(getSiteName())
                .map(v -> v.verifySessionOnPage(page, creds))
                .orElseGet(() -> StepResult.fail(StepType.SEND_AUTOREPLY,
                        "Проверка сессии не реализована для сайта"));
    }

    /**
     * Проверка страницы после успешного login/verify — выполняется до сохранения сессии.
     * Нужна, когда биржа пускает по сессии, но требует дополнительное действие (например FL.ru просит код из письма).
     *
     * @return ошибку для пользователя либо {@code null}, если всё в порядке
     */
    protected StepResult<Void> checkAfterLogin(Page page, DecryptedCredential creds) {
        return null;
    }

    protected void pauseForSessionVerify(Page page, String userUuid) {
        long pause = postLoginPauseMs;
        if (pause <= 0 && !headless) {
            pause = 5000;
        }
        if (pause <= 0 || page == null) {
            return;
        }
        log.info("SESSION: пауза {} ms для проверки UI (headless={})", pause, headless);
        takeScreenshot(page, getSiteName(), userUuid, "session_verify");
        page.waitForTimeout(pause);
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
            getCurrentManager().humanClick(page, selector);
            return true;
        } catch (Exception e) {
            log.warn("CLICK FAILED at step '{}': selector '{}'", step, selector);
            return false;
        }
    }

    protected BrowserLaunchOptions buildLaunchOptions(SiteName site, AiNotificationPayload payload) {
        String userEmail = payload.getUser() != null ? payload.getUser().getEmail() : null;
        if (!proxy) {
            return new BrowserLaunchOptions(null, headless, false, userEmail);
        }
        ProxyCredentials proxyCred = assignedProxyService.getProxyForUserAndModule(
                payload.getUser().getUuid(),
                payload.getModule().getId()
        );
        if (proxyCred == null) {
            proxyCred = managerResolver.getLocalManager().getProxyWithRetry(3, 500);
            log.info("АВТООТВЕТ: {} -> нет закреплённого прокси, взят случайный: {}", site,
                    proxyCred != null ? proxyCred.getHost() + ":" + proxyCred.getPort() : "нет активных");
        }
        return new BrowserLaunchOptions(proxyCred, headless, true, userEmail);
    }

    protected void setOtp(AiNotificationPayload payload, String otp, boolean used) {
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

    protected abstract StepResult<Void> login(Page page, AiNotificationPayload payload,
                                              DecryptedCredential creds, AutoreplyMode mode);

    protected abstract StepResult<Void> processAutoReply(Page page, AiNotificationPayload payload,
                                                         DecryptedCredential creds);

    protected abstract SiteName getSiteName();
}
