package by.gdev.alert.job.notification.service.ai.parser.debug;

import by.gdev.common.model.SiteName;
import com.microsoft.playwright.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Сохраняет скриншоты страниц Playwright на диск.
 * <p>
 * Структура каталогов:
 * <pre>
 * {baseDir}/{site}/{date}/{userUuid}/{timestamp}_{suffix}/{step}.png
 * </pre>
 */
@Slf4j
@Service
public class ScreenshotService {

    @Value("${autoreply.screenshots.enabled:false}")
    private boolean enabled;

    @Value("${autoreply.screenshots.dir:./autoreply/screenshots}")
    private String baseDir;

    /**
     * Делает скриншот страницы в байтах. Ничего не сохраняет на диск.
     * Возвращает {@code null}, если снимок не удался.
     */
    public byte[] capture(Page page) {
        if (page == null) return null;
        try {
            return page.screenshot();
        } catch (Exception e) {
            log.warn("Не удалось сделать скриншот: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Делает скриншот и сохраняет его на диск.
     * Если сохранение отключено в настройках — просто выходит.
     */
    public void take(Page page, SiteName site, String userUuid, String step) {
        if (!enabled) {
            log.debug("Сохранение скриншотов отключено");
            return;
        }
        byte[] bytes = capture(page);
        if (bytes == null || bytes.length == 0) {
            log.warn("Не удалось получить скриншот для шага '{}'", step);
            return;
        }
        try {
            Path file = buildPath(site, userUuid, step);
            Files.createDirectories(file.getParent());
            Files.write(file, bytes);
            log.info("Скриншот сохранён: {}", file.toAbsolutePath());
        } catch (Exception e) {
            log.warn("Не удалось сохранить скриншот для шага '{}': {}", step, e.getMessage());
        }
    }

    private Path buildPath(SiteName site, String userUuid, String step) {
        String date = LocalDate.now().toString();
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss_SSS"));
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String timeDir = timestamp + "_" + suffix;

        return Paths.get(baseDir)
                .resolve(site.name())
                .resolve(date)
                .resolve(userUuid)
                .resolve(timeDir)
                .resolve(step + ".png");
    }
}