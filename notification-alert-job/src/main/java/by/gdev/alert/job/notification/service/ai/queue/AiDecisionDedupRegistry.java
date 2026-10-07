package by.gdev.alert.job.notification.service.ai.queue;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.scheduler.Schedulers;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Защита от параллельной постановки в очередь одного и того же заказа (по ссылке).
 * После ошибки обработки ключ снимается — повторный POST /decision допускается.
 * После успешного автоответа ссылка блокируется на {@link #SUCCESS_RETENTION_MINUTES} мин.
 */
@Slf4j
@Component
public class AiDecisionDedupRegistry {

    static final long SUCCESS_RETENTION_MINUTES = 5;

    private final Set<String> inFlightOrBlocked = ConcurrentHashMap.newKeySet();

    /**
     * @return {@code true}, если заказ можно принять; {@code false} — дубликат (уже в очереди или недавно успешно обработан)
     */
    public boolean tryAcquire(String orderLink) {
        if (orderLink == null || orderLink.isBlank()) {
            return true;
        }
        return inFlightOrBlocked.add(orderLink);
    }

    /** Снять блокировку после неуспешной обработки (можно повторить запрос). */
    public void releaseAfterFailure(String orderLink) {
        if (orderLink == null || orderLink.isBlank()) {
            return;
        }
        if (inFlightOrBlocked.remove(orderLink)) {
            log.debug("DEDUP: снята блокировка после ошибки для {}", orderLink);
        }
    }

    /** Успешный автоответ — держим ключ, чтобы не отправить дважды; потом снимаем по таймеру. */
    public void retainAfterSuccess(String orderLink) {
        if (orderLink == null || orderLink.isBlank()) {
            return;
        }
        Schedulers.boundedElastic().schedule(
                () -> {
                    if (inFlightOrBlocked.remove(orderLink)) {
                        log.debug("DEDUP: истёк срок блокировки после успеха для {}", orderLink);
                    }
                },
                SUCCESS_RETENTION_MINUTES,
                TimeUnit.MINUTES
        );
    }
}
