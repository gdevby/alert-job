package by.gdev.common.service.playwright.sessions.storage.impl;

import by.gdev.common.service.playwright.sessions.utils.SessionStorageJsonHelper;
import by.gdev.common.service.playwright.sessions.utils.SessionStorageKeys;
import by.gdev.common.service.playwright.sessions.storage.SessionStorage;
import com.google.gson.JsonSyntaxException;
import com.google.gson.JsonObject;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@ConditionalOnProperty(name = "playwright.sessions.mode", havingValue = "memory")
public class InMemorySessionStorage implements SessionStorage {

    private final ConcurrentHashMap<String, String> sessions = new ConcurrentHashMap<>();

    @PostConstruct
    void logMode() {
        log.info("SESSION: режим memory (сессии не переживают рестарт JVM)");
    }

    private String key(String userUuid, String siteName, String login) {
        return SessionStorageKeys.memoryKey(userUuid, siteName, login);
    }

    @Override
    public boolean hasSession(String userUuid, String siteName, String login) {
        return sessions.containsKey(key(userUuid, siteName, login));
    }

    @Override
    public Optional<Path> storageStatePath(String userUuid, String siteName, String login) {
        return Optional.empty();
    }

    @Override
    public Optional<String> storageStateJson(String userUuid, String siteName, String login) {
        String json = sessions.get(key(userUuid, siteName, login));
        return json != null && !json.isBlank() ? Optional.of(json) : Optional.empty();
    }

    @Override
    public String describeSession(String userUuid, String siteName, String login) {
        return "memory:" + key(userUuid, siteName, login);
    }

    @Override
    public void restoreCookies(BrowserContext context, String userUuid, String siteName, String login) {
        JsonObject root = readRoot(userUuid, siteName, login);
        if (root == null) {
            log.info("SESSION: сессия в памяти не найдена для restoreCookies {}/{}/{}", userUuid, siteName, login);
            return;
        }
        try {
            SessionStorageJsonHelper.restoreCookiesFromRoot(log, context, root, userUuid, siteName, login);
        } catch (Exception e) {
            log.warn("SESSION: ошибка restoreCookies (memory): {}", e.getMessage());
        }
    }

    @Override
    public void restoreOrigins(Page page, String userUuid, String siteName, String login) {
        JsonObject root = readRoot(userUuid, siteName, login);
        if (root == null) {
            return;
        }
        try {
            SessionStorageJsonHelper.restoreOriginsFromRoot(log, page, root);
        } catch (Exception e) {
            log.warn("SESSION: ошибка восстановления origins (memory): {}", e.getMessage());
        }
    }

    private JsonObject readRoot(String userUuid, String siteName, String login) {
        String json = sessions.get(key(userUuid, siteName, login));
        if (json == null) {
            return null;
        }
        try {
            return SessionStorageJsonHelper.parseRoot(json);
        } catch (JsonSyntaxException e) {
            log.warn("SESSION: повреждённая сессия в memory {}, удаляем", key(userUuid, siteName, login));
            delete(userUuid, siteName, login);
            return null;
        }
    }

    @Override
    public void save(BrowserContext context, String userUuid, String siteName, String login) {
        try {
            String json = context.storageState();
            sessions.put(key(userUuid, siteName, login), json);
            log.info("SESSION: storageState сохранён в memory {}", describeSession(userUuid, siteName, login));
        } catch (Exception e) {
            log.error("SESSION: ошибка сохранения (memory): {}", e.getMessage());
        }
    }

    @Override
    public void delete(String userUuid, String siteName, String login) {
        sessions.remove(key(userUuid, siteName, login));
        log.warn("SESSION: сессия удалена из memory {}/{}/{}", userUuid, siteName, login);
    }
}
