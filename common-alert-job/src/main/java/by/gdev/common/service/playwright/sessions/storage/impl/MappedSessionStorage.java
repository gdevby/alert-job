package by.gdev.common.service.playwright.sessions.storage.impl;

import by.gdev.common.service.playwright.sessions.utils.MappedSessionFile;
import by.gdev.common.service.playwright.sessions.storage.SessionStorage;
import by.gdev.common.service.playwright.sessions.utils.SessionStorageJsonHelper;
import by.gdev.common.service.playwright.sessions.utils.SessionStorageKeys;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@ConditionalOnProperty(name = "playwright.sessions.mode", havingValue = "mmap")
public class MappedSessionStorage implements SessionStorage {

    private static final int DEFAULT_MAX_PAYLOAD_BYTES = 2 * 1024 * 1024;
    private static final String MMAP_DIR_NAME = "alert-job-sessions-mmap";

    @Value("${playwright.sessions.mmap.max-payload-bytes:" + DEFAULT_MAX_PAYLOAD_BYTES + "}")
    private int maxPayloadBytes;

    private Path baseDir;
    private final ConcurrentHashMap<String, Object> fileLocks = new ConcurrentHashMap<>();

    @PostConstruct
    void initBaseDir() {
        baseDir = Paths.get(System.getProperty("java.io.tmpdir")).resolve(MMAP_DIR_NAME).toAbsolutePath().normalize();
        try {
            Files.createDirectories(baseDir);
        } catch (Exception e) {
            log.error("SESSION: не удалось создать каталог mmap {}: {}", baseDir, e.getMessage());
        }
        log.info("SESSION: режим mmap, каталог (java.io.tmpdir): {}, maxPayload={} байт", baseDir, maxPayloadBytes);
        log.info("SESSION: mmap — сессии переживают рестарт JVM; каталог temp может очищаться ОС при reboot");
    }

    Path sessionPath(String userUuid, String siteName, String login) {
        return SessionStorageKeys.mmapSessionFilePath(baseDir.toString(), userUuid, siteName, login);
    }

    private Object lockFor(Path path) {
        return fileLocks.computeIfAbsent(path.toString(), k -> new Object());
    }

    @Override
    public boolean hasSession(String userUuid, String siteName, String login) {
        return Files.exists(sessionPath(userUuid, siteName, login));
    }

    @Override
    public Optional<Path> storageStatePath(String userUuid, String siteName, String login) {
        return Optional.empty();
    }

    @Override
    public Optional<String> storageStateJson(String userUuid, String siteName, String login) {
        Path path = sessionPath(userUuid, siteName, login);
        synchronized (lockFor(path)) {
            try {
                return MappedSessionFile.read(path, maxPayloadBytes);
            } catch (Exception e) {
                log.warn("SESSION: не удалось прочитать mmap {}: {}", path, e.getMessage());
                return Optional.empty();
            }
        }
    }

    @Override
    public String describeSession(String userUuid, String siteName, String login) {
        return "mmap:" + sessionPath(userUuid, siteName, login).toAbsolutePath().normalize();
    }

    @Override
    public void restoreCookies(BrowserContext context, String userUuid, String siteName, String login) {
        JsonObject root = readRoot(userUuid, siteName, login);
        if (root == null) {
            log.info("SESSION: mmap-файл не найден или пуст для restoreCookies {}/{}/{}", userUuid, siteName, login);
            return;
        }
        try {
            SessionStorageJsonHelper.restoreCookiesFromRoot(log, context, root, userUuid, siteName, login);
        } catch (Exception e) {
            log.warn("SESSION: ошибка restoreCookies (mmap): {}", e.getMessage());
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
            log.warn("SESSION: ошибка восстановления origins (mmap): {}", e.getMessage());
        }
    }

    private JsonObject readRoot(String userUuid, String siteName, String login) {
        Path path = sessionPath(userUuid, siteName, login);
        synchronized (lockFor(path)) {
            try {
                Optional<String> json = MappedSessionFile.read(path, maxPayloadBytes);
                if (json.isEmpty()) {
                    return null;
                }
                return SessionStorageJsonHelper.parseRoot(json.get());
            } catch (JsonSyntaxException e) {
                log.warn("SESSION: повреждённый mmap {}, удаляем", path);
                delete(userUuid, siteName, login);
                return null;
            } catch (Exception e) {
                log.warn("SESSION: ошибка чтения mmap {}: {}", path, e.getMessage());
                return null;
            }
        }
    }

    @Override
    public void save(BrowserContext context, String userUuid, String siteName, String login) {
        Path path = sessionPath(userUuid, siteName, login);
        synchronized (lockFor(path)) {
            try {
                String json = context.storageState();
                MappedSessionFile.write(path, json, maxPayloadBytes);
                log.info("SESSION: storageState сохранён в mmap {}", path.toAbsolutePath().normalize());
            } catch (Exception e) {
                log.error("SESSION: ошибка сохранения (mmap): {}", e.getMessage());
            }
        }
    }

    @Override
    public void delete(String userUuid, String siteName, String login) {
        Path path = sessionPath(userUuid, siteName, login);
        synchronized (lockFor(path)) {
            try {
                Files.deleteIfExists(path);
                fileLocks.remove(path.toString());
                log.warn("SESSION: сессия удалена (mmap) {}/{}/{}", userUuid, siteName, login);
            } catch (Exception e) {
                log.error("SESSION: ошибка удаления (mmap): {}", e.getMessage());
            }
        }
    }
}
