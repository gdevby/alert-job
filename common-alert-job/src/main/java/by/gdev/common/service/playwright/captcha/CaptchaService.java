package by.gdev.common.service.playwright.captcha;

import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Mouse;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.BoundingBox;
import com.microsoft.playwright.options.Cookie;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("captchaServiceCommon")
@Slf4j
public class CaptchaService {

    private static final String WEBLANCER_COOKIE_URL = "https://www.weblancer.net";

    @Value("${captcha.cloudflare.enabled:true}")
    private boolean cloudflareEnabled;

    @Value("${captcha.cloudflare.timeout-ms:45000}")
    private long cloudflareTimeoutMs;

    private Frame findYandexCaptchaFrame(Page page) {
        for (Frame f : page.frames()) {
            String url = f.url();
            if (url != null && url.contains("checkbox")) {
                return f;
            }
        }
        return null;
    }

    private boolean clickYandexSmartCaptcha(Page page, Frame frame) {
        try {
            Locator iframeLocator = page.locator("iframe[data-testid='checkbox-iframe']");
            BoundingBox box = iframeLocator.boundingBox();
            if (box == null) {
                log.warn("SmartCaptcha: не удалось получить boundingBox iframe");
                return false;
            }
            page.mouse().click(box.x + 5, box.y + 5);
            Thread.sleep(200);

            frame.waitForSelector("input#js-button",
                    new Frame.WaitForSelectorOptions().setTimeout(10000));
            frame.evaluate("window.postMessage(JSON.stringify({methodCall: 'start'}), '*')");
            Thread.sleep(1500);
            Locator input = frame.locator("input#js-button");
            String aria = input.getAttribute("aria-checked");
            if ("true".equals(aria)) {
                log.debug("SmartCaptcha: aria-checked=true — капча пройдена");
                return true;
            }
            String checked = frame.locator(".CheckboxCaptcha-Checkbox")
                    .getAttribute("data-checked");
            if ("true".equals(checked)) {
                log.debug("SmartCaptcha: data-checked=true — капча пройдена");
                return true;
            }
            log.warn("SmartCaptcha: input/data-checked не изменились");
            return false;
        } catch (Exception e) {
            log.error("SmartCaptcha: ошибка при клике", e);
            return false;
        }
    }

    public boolean solveYandexSmartCaptcha(Page page) {
        try {
            Frame frame = findYandexCaptchaFrame(page);
            if (frame == null) {
                log.debug("SmartCaptcha: iframe не найден — капча отсутствует");
                return true;
            }
            log.debug("SmartCaptcha: iframe найден, начинаем обход...");

            boolean result = clickYandexSmartCaptcha(page, frame);
            if (result) {
                log.info("SmartCaptcha: успешно пройдена");
            } else {
                log.warn("SmartCaptcha: не удалось пройти");
            }
            return result;

        } catch (Exception e) {
            log.error("SmartCaptcha: ошибка при обходе", e);
            return false;
        }
    }

    public boolean isCloudflareChallengePresent(Page page) {
        try {
            for (Frame f : page.frames()) {
                String url = f.url();
                if (url != null && (url.contains("challenges.cloudflare.com") || url.contains("turnstile"))) {
                    log.debug("Cloudflare Turnstile: frame detected {}", url);
                    return true;
                }
            }
            String[] textSelectors = {
                    "text=Я человек",
                    "text=Verify you are human",
                    "text=Just a moment",
                    "text=Checking your browser"
            };
            for (String sel : textSelectors) {
                Locator loc = page.locator(sel);
                if (loc.count() > 0 && loc.first().isVisible()) {
                    log.debug("Cloudflare Turnstile: текст challenge найден ({})", sel);
                    return true;
                }
            }
            if (page.locator("iframe[src*='challenges.cloudflare.com']").count() > 0) {
                return true;
            }
            if (page.locator("iframe[src*='turnstile']").count() > 0) {
                return true;
            }
            if (page.locator(".cf-turnstile, [data-turnstile-widget]").count() > 0) {
                return true;
            }
            return false;
        } catch (Exception e) {
            log.debug("Cloudflare Turnstile: ошибка проверки presence: {}", e.getMessage());
            return false;
        }
    }

    public boolean waitForCfClearance(Page page, String cookieDomain, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (hasCfClearanceCookie(page, cookieDomain)) {
                log.info("Cloudflare Turnstile: cf_clearance получен для {}", cookieDomain);
                return true;
            }
            page.waitForTimeout(500);
        }
        log.warn("Cloudflare Turnstile: timeout ожидания cf_clearance ({} ms)", timeoutMs);
        return false;
    }

    private boolean hasCfClearanceCookie(Page page, String cookieDomain) {
        try {
            List<Cookie> cookies = page.context().cookies(cookieDomain);
            return cookies.stream().anyMatch(c -> "cf_clearance".equals(c.name));
        } catch (Exception e) {
            return false;
        }
    }

    private Locator findTurnstileWidget(Page page) {
        String[] selectors = {
                "iframe[src*='challenges.cloudflare.com']",
                "iframe[src*='turnstile']",
                ".cf-turnstile",
                "[data-turnstile-widget]"
        };
        for (String sel : selectors) {
            Locator loc = page.locator(sel);
            if (loc.count() > 0) {
                return loc.first();
            }
        }
        return null;
    }

    private void humanLikeClickOnWidget(Page page, Locator widget) {
        BoundingBox box = widget.boundingBox();
        if (box == null) {
            log.warn("Cloudflare Turnstile: boundingBox виджета не получен");
            return;
        }
        double cx = box.x + box.width / 2 + (Math.random() * 10 - 5);
        double cy = box.y + box.height / 2 + (Math.random() * 10 - 5);
        int steps = 8 + (int) (Math.random() * 8);
        page.mouse().move(cx, cy, new Mouse.MoveOptions().setSteps(steps));
        page.waitForTimeout(200 + (int) (Math.random() * 300));
        page.mouse().click(cx, cy);
        log.info("Cloudflare Turnstile: клик по виджету ({}, {})", (int) cx, (int) cy);
    }

    private void tryClickInsideTurnstileFrame(Page page) {
        String[] iframeSelectors = {
                "iframe[src*='challenges.cloudflare.com']",
                "iframe[src*='turnstile']"
        };
        for (String sel : iframeSelectors) {
            if (page.locator(sel).count() == 0) {
                continue;
            }
            try {
                Locator inner = page.frameLocator(sel)
                        .locator("input[type=checkbox], .ctp-checkbox-label, label, [role=checkbox]")
                        .first();
                inner.click(new Locator.ClickOptions().setTimeout(5000));
                log.info("Cloudflare Turnstile: клик внутри iframe ({})", sel);
                return;
            } catch (Exception e) {
                log.debug("Cloudflare Turnstile: клик внутри iframe не удался ({}): {}", sel, e.getMessage());
            }
        }
    }

    private boolean isLoginFormVisible(Page page) {
        try {
            Locator login = page.locator("input[name='login']");
            return login.count() > 0 && login.first().isVisible();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean solveCloudflareTurnstile(Page page) {
        if (!cloudflareEnabled) {
            log.debug("Cloudflare Turnstile: отключено в конфиге");
            return true;
        }
        try {
            if (!isCloudflareChallengePresent(page)) {
                log.debug("Cloudflare Turnstile: challenge не обнаружен");
                return true;
            }
            log.info("Cloudflare Turnstile: начинаем прохождение...");

            Locator widget = findTurnstileWidget(page);
            if (widget != null) {
                humanLikeClickOnWidget(page, widget);
            } else {
                log.warn("Cloudflare Turnstile: виджет не найден, пробуем клик внутри iframe");
            }
            tryClickInsideTurnstileFrame(page);

            page.waitForTimeout(1500);

            if (waitForCfClearance(page, WEBLANCER_COOKIE_URL, cloudflareTimeoutMs)) {
                return true;
            }
            if (!isCloudflareChallengePresent(page) && isLoginFormVisible(page)) {
                log.info("Cloudflare Turnstile: challenge исчез, форма логина доступна");
                return true;
            }
            if (!isCloudflareChallengePresent(page)) {
                log.info("Cloudflare Turnstile: challenge исчез после клика");
                return true;
            }
            log.warn("Cloudflare Turnstile: не удалось пройти");
            return false;
        } catch (Exception e) {
            log.error("Cloudflare Turnstile: ошибка при обходе", e);
            return false;
        }
    }
}
