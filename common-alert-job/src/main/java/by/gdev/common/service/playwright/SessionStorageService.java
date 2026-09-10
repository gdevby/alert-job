package by.gdev.common.service.playwright;

import com.microsoft.playwright.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.*;

@Slf4j
@Component
public class SessionStorageService {

    @Autowired
    private PlaywrightManager playwrightManager;

    @Value("${playwright.sessions.dir:./sessions}")
    private String baseDir;

    private Path sessionFile(String siteName, String userUuid) {
        return Paths.get(baseDir)
                .resolve(siteName)
                .resolve(userUuid + ".json");
    }

    public boolean hasSession(String siteName, String userUuid) {
        return Files.exists(sessionFile(siteName, userUuid));
    }

    public BrowserContext load(Browser browser, String siteName, String userUuid) {
        Path file = sessionFile(siteName, userUuid);

        // базовые опции из PlaywrightManager
        Browser.NewContextOptions options = playwrightManager.baseContextOptions();
        if (Files.exists(file)) {
            log.info("SESSION: Загружаем существующую сессию {}", file);
            options.setStorageStatePath(file);
        } else {
            log.info("SESSION: Сессия отсутствует → создаём новый контекст");
        }
        BrowserContext context = browser.newContext(options);
        // антидетект-скрипты из PlaywrightManager
        playwrightManager.applyStealthScripts(context);
        return context;
    }

    public void save(BrowserContext context, String siteName, String userUuid) {
        try {
            Path file = sessionFile(siteName, userUuid);
            Files.createDirectories(file.getParent());
            context.storageState(new BrowserContext.StorageStateOptions().setPath(file));
            log.info("SESSION: storageState сохранён {}", file);
        } catch (Exception e) {
            log.error("SESSION: Ошибка сохранения сессии: {}", e.getMessage());
        }
    }

    public void delete(String siteName, String userUuid) {
        try {
            Path file = sessionFile(siteName, userUuid);
            Files.deleteIfExists(file);
            log.warn("SESSION: Протухшая сессия удалена {}", file);
        } catch (Exception e) {
            log.error("SESSION: Ошибка удаления сессии: {}", e.getMessage());
        }
    }
}
