package by.gdev.alert.job.parser.service.order;

import by.gdev.alert.job.parser.util.SiteName;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

@Component
@Slf4j
public class CloudflareChallengeService {

    private static final int CHALLENGE_TIMEOUT_MS = 60_000;

    @Value("${parser.cloudflare.storage.freelancehunt.com:}")
    private String freelancehuntStoragePath;

    public boolean passChallengeIfNeeded(Page page, BrowserContext context, SiteName site) {
        if (!isChallengePage(page)) {
            return true;
        }

        log.info("Cloudflare challenge обнаружен для {}, ожидаем прохождение...", site);
        tryClickVisibleTurnstile(page);

        if (!waitForChallengeResolved(page, CHALLENGE_TIMEOUT_MS)) {
            log.warn("Cloudflare challenge не пройден за {} мс для {}", CHALLENGE_TIMEOUT_MS, site);
            return false;
        }

        log.info("Cloudflare challenge пройден для {}", site);
        saveStorageState(context, site);
        return true;
    }

    public boolean isChallengePage(Page page) {
        try {
            String title = page.title().toLowerCase(Locale.ROOT);
            if (title.contains("один момент") || title.contains("just a moment")) {
                return true;
            }
            String html = page.content();
            return html.contains("_cf_chl_opt")
                    || html.contains("challenge-platform")
                    || html.contains("challenges.cloudflare.com/turnstile")
                    || html.contains("cdn-cgi/challenge-platform");
        } catch (Exception e) {
            log.debug("Не удалось проверить Cloudflare challenge: {}", e.getMessage());
            return false;
        }
    }

    public Path resolveStoragePath(SiteName site) {
        if (site != SiteName.FREELANCEHUNT) {
            return null;
        }
        if (freelancehuntStoragePath == null || freelancehuntStoragePath.isBlank()) {
            return null;
        }
        return Paths.get(freelancehuntStoragePath);
    }

    private boolean waitForChallengeResolved(Page page, int timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;

        while (System.currentTimeMillis() < deadline) {
            if (hasCfClearance(page.context()) && !isChallengePage(page)) {
                return true;
            }
            if (page.locator("div.job-list-item, ul.tree > li.tree-item").count() > 0) {
                return true;
            }

            tryClickVisibleTurnstile(page);
            page.waitForTimeout(500);
        }

        return hasCfClearance(page.context()) && !isChallengePage(page);
    }

    private void tryClickVisibleTurnstile(Page page) {
        try {
            Locator cloudflareIframe = page.locator("iframe[src*='challenges.cloudflare.com']");
            if (cloudflareIframe.count() > 0 && cloudflareIframe.first().isVisible()) {
                log.debug("Клик по видимому Cloudflare Turnstile iframe");
                cloudflareIframe.first().click();
                page.waitForTimeout(1000);
                return;
            }

            Locator container = page.locator("#ncOB5");
            if (container.count() == 0) {
                return;
            }
            Locator nestedIframe = container.locator("iframe");
            if (nestedIframe.count() > 0 && nestedIframe.first().isVisible()) {
                log.debug("Клик по Turnstile iframe в #ncOB5");
                nestedIframe.first().click();
                page.waitForTimeout(1000);
            }
        } catch (Exception e) {
            log.debug("Turnstile click skipped: {}", e.getMessage());
        }
    }

    private boolean hasCfClearance(BrowserContext context) {
        return context.cookies().stream()
                .anyMatch(cookie -> "cf_clearance".equals(cookie.name));
    }

    private void saveStorageState(BrowserContext context, SiteName site) {
        Path path = resolveStoragePath(site);
        if (path == null) {
            return;
        }
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            context.storageState(new BrowserContext.StorageStateOptions().setPath(path));
            log.info("Cloudflare storageState сохранён: {}", path);
        } catch (Exception e) {
            log.warn("Не удалось сохранить Cloudflare storageState в {}: {}", path, e.getMessage());
        }
    }
}
