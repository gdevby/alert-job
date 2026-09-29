package by.gdev.common.service.playwright.flru;

import by.gdev.common.model.SiteName;
import by.gdev.common.service.playwright.captcha.CaptchaService;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.Locale;

/** Эвристики страниц FL.ru (защита от ботов / DDoS / блок IP). */
public final class FlRuPlaywrightGuards {

    /** Подстрока в тексте ошибки для retry — без hardcode имени сайта в проверке. */
    public static final String DDOS_RETRY_FAIL_MARKER = "не удалось открыть страницу логина";

    private static final String SUSPICIOUS_IP_ACTIVITY_MARKER = "подозрительная активность";

    private FlRuPlaywrightGuards() {
    }

    public static String ddosRetryFailMessage(SiteName site) {
        return site + ": сработала защита от DDoS — " + DDOS_RETRY_FAIL_MARKER;
    }

    public static boolean isSuspiciousIpActivityInBody(String htmlOrText) {
        if (htmlOrText == null || htmlOrText.isEmpty()) {
            return false;
        }
        return containsSuspiciousIpActivityText(htmlOrText.toLowerCase(Locale.ROOT));
    }

    public static boolean isSuspiciousIpActivityBlock(Page page) {
        try {
            if (isSuspiciousIpActivityInBody(page.content())) {
                return true;
            }
            String text = page.locator("body").innerText();
            return isSuspiciousIpActivityInBody(text);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean containsSuspiciousIpActivityText(String lowerText) {
        if (lowerText == null || lowerText.isEmpty()) {
            return false;
        }
        if (!lowerText.contains(SUSPICIOUS_IP_ACTIVITY_MARKER)) {
            return false;
        }
        return lowerText.contains("ip-адрес") || lowerText.contains("ip адрес")
                || lowerText.contains("с вашего ip");
    }

    public static boolean isAntiDdosOrBotWall(Page page, CaptchaService captchaService) {
        try {
            if (isLikelyAuthenticatedFlRuPage(page)) {
                return false;
            }
            if (isSuspiciousIpActivityBlock(page)) {
                return true;
            }
            if (captchaService != null && captchaService.isCloudflareChallengePresent(page)) {
                return true;
            }
            if (page.locator("input[name='username']").count() > 0) {
                Locator login = page.locator("input[name='username']").first();
                if (login.isVisible()) {
                    return false;
                }
            }
            String html = page.content().toLowerCase(Locale.ROOT);
            if (html.contains("ddos-guard") || html.contains("ddguard")) {
                return true;
            }
            if (!looksLikeBlockingChallengePage(page)) {
                return false;
            }
            String text = page.locator("body").innerText().toLowerCase(Locale.ROOT);
            if (text.contains("ddos-guard") || text.contains("distributed denial")) {
                return true;
            }
            if (text.contains("checking your browser") || text.contains("just a moment")) {
                return true;
            }
            if (text.contains("провер") && text.contains("браузер")) {
                return true;
            }
            if ((text.contains("атак") || text.contains("attack"))
                    && (text.contains("защит") || text.contains("protect"))
                    && text.length() < 4000) {
                return true;
            }
        } catch (RuntimeException ignored) {
            // страница в переходе — ниже по таймауту поля логина решим отдельно
        }
        return false;
    }

    public static boolean isLikelyAuthenticatedFlRuPage(Page page) {
        try {
            String url = page.url().toLowerCase(Locale.ROOT);
            if (url.contains("/account/login")) {
                return false;
            }
            String[] loggedInSelectors = {
                    "a[href*='/account/logout']",
                    "a[href*='logout.php']",
                    "a[href*='/users/'][href$='.html']",
                    ".header-user-menu",
                    "a[href='/account/']",
                    "a[href*='/account/?']",
            };
            for (String selector : loggedInSelectors) {
                Locator loc = page.locator(selector);
                if (loc.count() > 0 && loc.first().isVisible()) {
                    return true;
                }
            }
        } catch (RuntimeException ignored) {
        }
        return false;
    }

    private static boolean looksLikeBlockingChallengePage(Page page) {
        try {
            String url = page.url().toLowerCase(Locale.ROOT);
            if (url.contains("/account/login")) {
                return true;
            }
            String text = page.locator("body").innerText();
            return text != null && text.length() < 6000;
        } catch (RuntimeException e) {
            return true;
        }
    }
}
