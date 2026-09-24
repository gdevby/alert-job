package by.gdev.common.service.playwright.sessions.storage.impl;

import by.gdev.common.service.playwright.sessions.storage.SessionStorage;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import by.gdev.common.service.playwright.sessions.utils.SessionStorageJsonHelper;
import by.gdev.common.service.playwright.sessions.utils.SessionStorageKeys;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Slf4j
@Component
@ConditionalOnProperty(name = "playwright.sessions.mode", havingValue = "file", matchIfMissing = true)
public class FileSessionStorage implements SessionStorage {

    @Value("${playwright.sessions.dir:./sessions}")
    private String baseDir;

    @PostConstruct
    void logSessionsDir() {
        log.info("SESSION: режим file, каталог (absolute): {}", Paths.get(baseDir).toAbsolutePath().normalize());
    }

    Path sessionPath(String userUuid, String siteName, String login) {
        return SessionStorageKeys.sessionFilePath(baseDir, userUuid, siteName, login);
    }

    @Override
    public boolean hasSession(String userUuid, String siteName, String login) {
        return Files.exists(sessionPath(userUuid, siteName, login));
    }

    @Override
    public Optional<Path> storageStatePath(String userUuid, String siteName, String login) {
        Path file = sessionPath(userUuid, siteName, login);
        return Files.exists(file) ? Optional.of(file) : Optional.empty();
    }

    @Override
    public Optional<String> storageStateJson(String userUuid, String siteName, String login) {
        return Optional.empty();
    }

    @Override
    public String describeSession(String userUuid, String siteName, String login) {
        return sessionPath(userUuid, siteName, login).toAbsolutePath().normalize().toString();
    }

    @Override
    public void restoreCookies(BrowserContext context, String userUuid, String siteName, String login) {
        Path file = sessionPath(userUuid, siteName, login);
        if (!Files.exists(file)) {
            log.info("SESSION: файл сессии не найден для restoreCookies {}/{}/{}", userUuid, siteName, login);
            return;
        }
        try {
            JsonObject root = readSessionRoot(file, userUuid, siteName, login);
            SessionStorageJsonHelper.restoreCookiesFromRoot(log, context, root, userUuid, siteName, login);
        } catch (Exception e) {
            log.warn("SESSION: ошибка restoreCookies (файл не удалён): {}", e.getMessage());
        }
    }

    @Override
    public void restoreOrigins(Page page, String userUuid, String siteName, String login) {
        Path file = sessionPath(userUuid, siteName, login);
        if (!Files.exists(file)) {
            return;
        }
        try {
            JsonObject root = readSessionRoot(file, userUuid, siteName, login);
            SessionStorageJsonHelper.restoreOriginsFromRoot(log, page, root);
        } catch (Exception e) {
            log.warn("SESSION: ошибка восстановления origins: {}", e.getMessage());
        }
    }

    private JsonObject readSessionRoot(Path file, String userUuid, String siteName, String login) {
        try {
            String json = Files.readString(file);
            return SessionStorageJsonHelper.parseRoot(json);
        } catch (JsonSyntaxException e) {
            log.warn("SESSION: повреждённый файл сессии {}, удаляем", file);
            delete(userUuid, siteName, login);
            return null;
        } catch (Exception e) {
            log.warn("SESSION: не удалось прочитать {}: {}", file, e.getMessage());
            return null;
        }
    }

    @Override
    public void save(BrowserContext context, String userUuid, String siteName, String login) {
        try {
            Path file = sessionPath(userUuid, siteName, login);
            Files.createDirectories(file.getParent());
            context.storageState(new BrowserContext.StorageStateOptions().setPath(file));
            log.info("SESSION: storageState сохранён {}", file.toAbsolutePath().normalize());
        } catch (Exception e) {
            log.error("SESSION: ошибка сохранения: {}", e.getMessage());
        }
    }

    @Override
    public void delete(String userUuid, String siteName, String login) {
        try {
            Files.deleteIfExists(sessionPath(userUuid, siteName, login));
            log.warn("SESSION: сессия удалена {}/{}/{}", userUuid, siteName, login);
        } catch (Exception e) {
            log.error("SESSION: ошибка удаления: {}", e.getMessage());
        }
    }
}
