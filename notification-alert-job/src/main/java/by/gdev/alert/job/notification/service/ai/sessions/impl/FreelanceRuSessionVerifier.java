package by.gdev.alert.job.notification.service.ai.sessions.impl;

import by.gdev.alert.job.notification.model.dto.DecryptedCredential;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepResult;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepType;
import by.gdev.alert.job.notification.service.ai.sessions.AbstractAutoreplySessionVerifier;
import by.gdev.common.model.SiteName;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FreelanceRuSessionVerifier extends AbstractAutoreplySessionVerifier {

    @Value("${parser.autoreply.headless.freelance.ru:true}")
    private void setHeadless(boolean headless) {
        this.headless = headless;
    }

    @Value("${parser.autoreply.proxy.freelance.ru:false}")
    private void setProxy(boolean proxy) {
        this.proxy = proxy;
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.FREELANCERU;
    }

    @Override
    protected String sessionCookieCheckUrl() {
        return "https://freelance.ru";
    }

    @Override
    public StepResult<Void> verifySessionOnPage(Page page, DecryptedCredential creds) {
        try {
            safeNavigate(page, "https://freelance.ru/auth/login");
            page.waitForLoadState(LoadState.NETWORKIDLE);
            page.waitForTimeout(1500);
            if (!page.url().contains("/auth/login")) {
                log.info("SESSION-VERIFY: FREELANCERU сессия активна (редирект с login), пользователь: {}",
                        creds.login());
                return StepResult.ok(StepType.SEND_AUTOREPLY, null);
            }
            if (waitOrFail(page, "input[placeholder='логин или email']", 3000, "Поле логина")) {
                return StepResult.fail(StepType.SEND_AUTOREPLY, "Форма логина на /auth/login");
            }
            log.info("SESSION-VERIFY: FREELANCERU сессия активна, пользователь: {}", creds.login());
            return StepResult.ok(StepType.SEND_AUTOREPLY, null);
        } catch (Exception e) {
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Ошибка проверки сессии: " + e.getMessage());
        }
    }
}
