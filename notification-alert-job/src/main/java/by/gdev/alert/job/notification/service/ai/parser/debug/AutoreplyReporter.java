package by.gdev.alert.job.notification.service.ai.parser.debug;

import by.gdev.alert.job.notification.service.ai.merics.AutoreplyMetrics;
import by.gdev.common.model.SiteName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.event.Level;
import org.springframework.stereotype.Component;

/**
 * Регистрирует проблемные ситуации при отправке автооткликов:
 * пишет в лог и инкрементирует Prometheus-счётчик
 * {@code autoreply_problems_total} с тегами {@code site} и {@code error_type}.
 * <p>
 * Допустимы только {@link Level#WARN} и {@link Level#ERROR}, остальные трактуются как WARN.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoreplyReporter {

    private final AutoreplyMetrics autoreplyMetrics;

    /**
     * @param level     WARN или ERROR (остальные → WARN)
     * @param logger    логгер вызывающего класса (обычно {@code log} из {@code @Slf4j})
     * @param site      сайт-источник (идёт в тег {@code site})
     * @param message   текст сообщения
     * @param errorType тип ошибки (идёт в тег {@code error_type})
     */
    public void report(Level level, Logger logger, SiteName site, String message, String errorType) {
        if (Level.ERROR.equals(level)) {
            logger.error(message);
        } else {
            logger.warn(message);
        }
        autoreplyMetrics.incrementProblem(site.name(), errorType);
    }
}