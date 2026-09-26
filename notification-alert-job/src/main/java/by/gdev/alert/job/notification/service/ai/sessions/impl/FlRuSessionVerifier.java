package by.gdev.alert.job.notification.service.ai.sessions.impl;

import by.gdev.alert.job.notification.model.dto.DecryptedCredential;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepResult;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepType;
import by.gdev.common.service.playwright.flru.FlRuPlaywrightGuards;
import by.gdev.alert.job.notification.service.ai.sessions.AbstractAutoreplySessionVerifier;
import by.gdev.common.model.SiteName;
import by.gdev.common.service.playwright.captcha.CaptchaService;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FlRuSessionVerifier extends AbstractAutoreplySessionVerifier {

    private final CaptchaService captchaService;

    public FlRuSessionVerifier(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    @Value("${parser.autoreply.headless.fl.ru:true}")
    private void setHeadless(boolean headless) {
        this.headless = headless;
    }

    @Value("${parser.autoreply.proxy.fl.ru:false}")
    private void setProxy(boolean proxy) {
        this.proxy = proxy;
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.FLRU;
    }

    @Override
    protected String sessionCookieCheckUrl() {
        return "https://www.fl.ru";
    }

    @Override
    public StepResult<Void> verifySessionOnPage(Page page, DecryptedCredential creds) {
        try {
            String checkUrl = sessionCookieCheckUrl();
            BrowserContext ctx = page.context();
            boolean hasAuthCookies = contextHasCookie(ctx, checkUrl, "PHPSESSID")
                    || contextHasCookie(ctx, checkUrl, "id");
            if (!hasAuthCookies) {
                return StepResult.fail(StepType.SEND_AUTOREPLY, "Нет auth-cookies FL.ru в контексте");
            }
            safeNavigate(page, "https://www.fl.ru/");
            page.waitForTimeout(2000);
            if (FlRuPlaywrightGuards.isLikelyAuthenticatedFlRuPage(page)) {
                log.info("SESSION-VERIFY: {} -> сессия активна (UI), пользователь: {}", getSiteName(), creds.login());
                return StepResult.ok(StepType.SEND_AUTOREPLY, null);
            }
            if (FlRuPlaywrightGuards.isAntiDdosOrBotWall(page, captchaService)) {
                return StepResult.fail(StepType.SEND_AUTOREPLY,
                        FlRuPlaywrightGuards.ddosRetryFailMessage(getSiteName()));
            }
            if (page.url().contains("/account/login")) {
                return StepResult.fail(StepType.SEND_AUTOREPLY, "Редирект на страницу логина");
            }
            log.info("SESSION-VERIFY: {} -> сессия активна, пользователь: {}", getSiteName(), creds.login());
            return StepResult.ok(StepType.SEND_AUTOREPLY, null);
        } catch (Exception e) {
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Ошибка проверки сессии: " + e.getMessage());
        }
    }
}
