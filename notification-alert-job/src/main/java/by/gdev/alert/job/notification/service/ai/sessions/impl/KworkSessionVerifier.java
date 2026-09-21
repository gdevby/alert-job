package by.gdev.alert.job.notification.service.ai.sessions.impl;

import by.gdev.alert.job.notification.model.dto.DecryptedCredential;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepResult;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepType;
import by.gdev.alert.job.notification.service.ai.sessions.AbstractAutoreplySessionVerifier;
import by.gdev.common.model.SiteName;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KworkSessionVerifier extends AbstractAutoreplySessionVerifier {

    @Value("${parser.autoreply.headless.kwork.ru:true}")
    private void setHeadless(boolean headless) {
        this.headless = headless;
    }

    @Value("${parser.autoreply.proxy.kwork.ru:false}")
    private void setProxy(boolean proxy) {
        this.proxy = proxy;
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.KWORK;
    }

    @Override
    protected String sessionCookieCheckUrl() {
        return "https://kwork.ru";
    }

    @Override
    public StepResult<Void> verifySessionOnPage(Page page, DecryptedCredential creds) {
        try {
            page.navigate("https://kwork.ru/");
            page.waitForLoadState(LoadState.NETWORKIDLE);
            page.waitForTimeout(2000);
            if (page.url().contains("/login")) {
                return StepResult.fail(StepType.SEND_AUTOREPLY, "Редирект на /login");
            }
            Locator loginField = page.locator("input[placeholder='Электронная почта или логин']");
            if (loginField.count() > 0 && loginField.first().isVisible()) {
                return StepResult.fail(StepType.SEND_AUTOREPLY, "Форма логина на главной");
            }
            log.info("SESSION-VERIFY: KWORK сессия активна, пользователь: {}", creds.login());
            return StepResult.ok(StepType.SEND_AUTOREPLY, null);
        } catch (Exception e) {
            return StepResult.fail(StepType.SEND_AUTOREPLY, "Ошибка проверки сессии: " + e.getMessage());
        }
    }
}
