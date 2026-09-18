package by.gdev.common.service.playwright.sessions.storage;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.Cookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface SessionStorage {

    boolean hasSession(String userUuid, String siteName, String login);

    /** Путь к JSON на диске (режим file), если сессия есть. */
    Optional<Path> storageStatePath(String userUuid, String siteName, String login);

    /** JSON storageState (режим memory), если сессия есть. */
    Optional<String> storageStateJson(String userUuid, String siteName, String login);

    String describeSession(String userUuid, String siteName, String login);

    void restoreCookies(BrowserContext context, String userUuid, String siteName, String login);

    void restoreOrigins(Page page, String userUuid, String siteName, String login);

    void save(BrowserContext context, String userUuid, String siteName, String login);

    void delete(String userUuid, String siteName, String login);

    default void logContextCookies(BrowserContext context, String siteUrl, String label) {
        Logger log = LoggerFactory.getLogger(getClass());
        try {
            List<Cookie> cookies = siteUrl != null && !siteUrl.isBlank()
                    ? context.cookies(siteUrl)
                    : context.cookies();
            log.info("SESSION: {} — {} cookies{}", label, cookies.size(),
                    siteUrl != null && !siteUrl.isBlank() ? " для " + siteUrl : "");
            cookies.stream().limit(20).forEach(c ->
                    log.debug("SESSION: cookie name={} domain={}", c.name, c.domain));
        } catch (Exception e) {
            log.warn("SESSION: не удалось прочитать cookies контекста: {}", e.getMessage());
        }
    }
}
