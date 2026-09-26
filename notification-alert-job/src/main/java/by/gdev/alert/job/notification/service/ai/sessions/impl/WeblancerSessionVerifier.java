package by.gdev.alert.job.notification.service.ai.sessions.impl;

import by.gdev.alert.job.notification.model.dto.DecryptedCredential;
import by.gdev.alert.job.notification.service.ai.merics.AutoreplyErrorTypes;
import by.gdev.alert.job.notification.service.ai.parser.debug.AutoreplyReporter;
import by.gdev.alert.job.notification.service.ai.parser.debug.ScreenshotService;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepResult;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepType;
import by.gdev.alert.job.notification.service.ai.sessions.AbstractAutoreplySessionVerifier;
import by.gdev.common.model.SiteName;
import by.gdev.common.service.playwright.captcha.CaptchaService;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WeblancerSessionVerifier extends AbstractAutoreplySessionVerifier {

    private final CaptchaService captchaService;

    @Autowired
    private ScreenshotService screenshotService;

    @Autowired
    private AutoreplyReporter reporter;

    @Value("${captcha.cloudflare.enabled:true}")
    private boolean cloudflareCaptchaEnabled;

    @Value("${parser.autoreply.headless.weblancer.net:true}")
    private void setHeadless(boolean headless) {
        this.headless = headless;
    }

    @Value("${parser.autoreply.proxy.weblancer.net:false}")
    private void setProxy(boolean proxy) {
        this.proxy = proxy;
    }

    public WeblancerSessionVerifier(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.WEBLANCER;
    }

    @Override
    protected String sessionCookieCheckUrl() {
        return "https://www.weblancer.net";
    }

    @Override
    public StepResult<Void> verifySessionOnPage(Page page, DecryptedCredential creds) {
        try {
            String checkUrl = sessionCookieCheckUrl();
            BrowserContext ctx = page.context();
            boolean hasAuth = contextHasCookieValue(ctx, checkUrl, "auth_logged", "true")
                    || contextHasCookie(ctx, checkUrl, "token");
            if (!hasAuth) {
                return StepResult.fail(StepType.SEND_AUTOREPLY, "Нет auth-cookies Weblancer в контексте");
            }
            page.navigate("https://www.weblancer.net/?lang=ru");
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            page.waitForTimeout(1500);

            StepResult<Void> cf = passCloudflareIfNeeded(page, "verify_session");
            if (cf != null) {
                return cf;
            }

            Locator loginBtn = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Вход"));
            if (loginBtn.count() > 0 && loginBtn.first().isVisible()) {
                return StepResult.fail(StepType.SEND_AUTOREPLY, "Кнопка «Вход» видна при наличии cookies");
            }
            log.info("SESSION-VERIFY: {} -> сессия активна, пользователь: {}", getSiteName(), creds.login());
            return StepResult.ok(StepType.SEND_AUTOREPLY, null);
        } catch (Exception e) {
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Ошибка проверки сессии: " + e.getMessage());
        }
    }

    private StepResult<Void> passCloudflareIfNeeded(Page page, String step) {
        if (!cloudflareCaptchaEnabled) {
            return null;
        }
        log.debug("SESSION-VERIFY: {} -> Cloudflare ({})", getSiteName(), step);
        if (!captchaService.solveCloudflareTurnstile(page)) {
            reporter.report(Level.WARN, log, getSiteName(),
                    "SESSION-VERIFY: " + getSiteName() + " -> Cloudflare не пройдена, шаг: " + step,
                    AutoreplyErrorTypes.CAPTCHA_FAILED);
            return StepResult.fail(StepType.SEND_AUTOREPLY,
                    "Cloudflare Turnstile не пройдена (" + step + ")",
                    screenshotService.capture(page));
        }
        return null;
    }
}
