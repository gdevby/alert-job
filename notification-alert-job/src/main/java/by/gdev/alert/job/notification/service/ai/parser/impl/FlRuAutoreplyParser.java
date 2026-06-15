package by.gdev.alert.job.notification.service.ai.parser.impl;

import by.gdev.alert.job.notification.model.dto.AiNotificationPayload;
import by.gdev.alert.job.notification.model.dto.DecryptedCredential;
import by.gdev.alert.job.notification.service.ai.parser.AutoreplyPlaywrightParser;
import by.gdev.common.model.SiteName;
import by.gdev.common.service.playwright.PlaywrightManager;
import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.BoundingBox;
import com.microsoft.playwright.options.LoadState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Random;

@Slf4j
@Component
public class FlRuAutoreplyParser extends AutoreplyParser implements AutoreplyPlaywrightParser {

    @Value("${parser.autoreply.headless.fl.ru}")
    private void setHeadless(boolean headless) {
        this.headless = headless;
    }

    @Value("${parser.autoreply.proxy.fl.ru}")
    private void setProxy(boolean proxy) {
        this.proxy = proxy;
    }

    @Value("${parser.autoreply.send.request.fl.ru}")
    private void setOnSendRequest(boolean sendRequest) {
        this.sendRequest = sendRequest;
    }

    public FlRuAutoreplyParser(PlaywrightManager playwrightManager) {
        super(playwrightManager);
    }

    @Override
    protected boolean login(Page page, DecryptedCredential creds) {
        try {
            // Открываем страницу логина
            safeNavigate(page, "https://www.fl.ru/account/login/");

            // Ждём поле логина
            if (!waitOrFail(page, "input[name='username']", 8000, "Поле логина"))
                return false;

            // Вводим логин
            try {
                page.fill("input[name='username']", creds.login());
            } catch (Exception e) {
                log.warn("Не удалось заполнить логин");
                return false;
            }

            // Вводим пароль
            try {
                page.fill("input[name='password']", creds.password());
            } catch (Exception e) {
                log.warn("Не удалось заполнить пароль");
                return false;
            }

            // Пытаемся пройти SmartCaptcha
            if (!handleSmartCaptcha(page)) {
                log.warn("FL.ru: SmartCaptcha не прошла");
                return false;
            }

            // Жмём кнопку Войти
            if (!clickOrFail(page, "#submit-button", 8000, "Кнопка 'Войти'"))
                return false;

            // Ждём загрузку
            try {
                page.waitForLoadState(LoadState.NETWORKIDLE);
            } catch (Exception e) {
                log.warn("Не удалось дождаться загрузки после входа");
                return false;
            }

            // Проверяем, что логин успешный
            if (page.url().contains("/account/login")) {
                log.warn("FL.ru: вход не выполнен, остались на странице логина");
                return false;
            }

            log.debug("{}: успешный вход в аккаунт {}", getSiteName(), creds.login());
            return true;

        } catch (Exception e) {
            log.error("{}: ошибка при логине", getSiteName(), e);
            return false;
        }
    }

    private Frame findCaptchaFrame(Page page) {
        for (Frame f : page.frames()) {
            String url = f.url();
            if (url != null && url.contains("captcha")) {
                return f;
            }
        }
        return null;
    }

    public String dumpCaptcha(Page page) {
        Frame f = findCaptchaFrame(page);
        if (f == null) return "NO CAPTCHA FRAME";
        return f.content();
    }

    private boolean handleSmartCaptcha(Page page) {
        try {
            Frame frame = findCaptchaFrame(page);
            if (frame == null) {
                log.debug("SmartCaptcha: iframe не найден — капча отключена");
                return true;
            }
            log.warn("SmartCaptcha: iframe найден, пытаемся пройти...");
            return clickSmartCaptcha(page, frame);
        } catch (Exception e) {
            log.error("SmartCaptcha: ошибка обработки", e);
            return false;
        }
    }

    private boolean clickSmartCaptcha(Page page, Frame frame) {
        try {
            //Клик по странице — обязателен
            page.mouse().click(10, 10);
            Thread.sleep(200);
            //Ждём появления input внутри iframe
            try {
                frame.waitForSelector("input", new Frame.WaitForSelectorOptions().setTimeout(10000));
            } catch (Exception e) {
                log.warn("SmartCaptcha: input не появился в iframe");
                return false;
            }
            //Отправляем "start" в iframe
            frame.evaluate("window.postMessage(JSON.stringify({methodCall: 'start'}), '*')");
            Thread.sleep(1500);
            //Проверяем input
            Locator input = frame.locator("input");
            String aria = input.getAttribute("aria-checked");
            if ("true".equals(aria)) {
                log.debug("SmartCaptcha: aria-checked=true — капча пройдена");
                return true;
            }
            String checked = input.getAttribute("checked");
            if (checked != null) {
                log.info("SmartCaptcha: input[checked] — капча пройдена");
                return true;
            }
            log.warn("SmartCaptcha: input не изменился");
            return false;
        } catch (Exception e) {
            log.error("SmartCaptcha: ошибка при клике", e);
            return false;
        }
    }

    @Override
    protected boolean processAutoReply(Page page, AiNotificationPayload payload) {
        return false;
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.FLRU;
    }
}
