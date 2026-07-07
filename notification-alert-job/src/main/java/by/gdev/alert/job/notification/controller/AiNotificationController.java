package by.gdev.alert.job.notification.controller;

import by.gdev.alert.job.notification.model.dto.*;
import by.gdev.alert.job.notification.service.ai.queue.UserQueueManager;
import by.gdev.common.model.HeaderName;
import by.gdev.common.model.SiteName;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/notification/api/ai")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "AI Notification", description = "Управление уведомлениями и очередями AI")
public class AiNotificationController {
    private final UserQueueManager userQueueManager;
    private final Set<String> dedup = ConcurrentHashMap.newKeySet();

    @PostMapping("/decision")
    public Mono<ResponseEntity<?>> receiveAiDecision(@RequestBody AiNotificationPayload payload) {
        String key = payload.getOrder().getLink();
        if (!dedup.add(key)) {
            log.warn("DUPLICATE DROPPED at NotificationController: {}", key);
            return Mono.just(ResponseEntity.ok(Map.of("status", "duplicate")));
        }
        // Через 5 минут удаляем ключ из dedup, чтобы:
        // не держать ссылку в памяти вечно (иначе Set разрастётся),
        // позволить повторно обработать этот же заказ, если он придёт позже,
        // не блокировать повторную отправку, если предыдущая попытка упала.
        Schedulers.boundedElastic().schedule(() -> dedup.remove(key), 5, TimeUnit.MINUTES);
        log.debug("QUEUE: accepted AI decision {}", key);
        //кладем пайлоад в очередь обработки пользователя
        userQueueManager.submit(payload);
        // получаем размер очереди для пользователя
        int userQueueSize = userQueueManager.size(payload.getUser().getUuid());
        return Mono.just(ResponseEntity.accepted().body(Map.of("status", "queued", "queueSize", userQueueSize)));
    }

    @Operation(
            summary = "Получить детали очереди пользователя",
            description = "Возвращает список всех заказов в очереди для данного пользователя (без удаления)."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Информация об очереди",
            content = @Content(schema = @Schema(
                    example = "{" +
                            "\"userUuid\": \"eba609a4-...\"," +
                            "\"queueSize\": 2," +
                            "\"items\": [" +
                            "{\"orderLink\": \"https://...\", \"site\": \"KWORK\", \"module\": \"kwork\"}," +
                            "{\"orderLink\": \"https://...\", \"site\": \"KWORK\", \"module\": \"kwork\"}" +
                            "]" +
                            "}"
            ))
    )
    @GetMapping("/queue-details")
    public ResponseEntity<?> getQueueDetails(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid) {
        BlockingQueue<AiNotificationPayload> queue = userQueueManager.getQueue(uuid);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("userUuid", uuid != null ? uuid : "unknown");
        response.put("queueSize", queue != null ? queue.size() : 0);

        if (queue == null || queue.isEmpty()) {
            response.put("items", List.of());
            return ResponseEntity.ok(response);
        }

        List<Map<String, Object>> items = queue.stream()
                .map(payload -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    if (payload.getOrder() != null) {
                        item.put("orderLink", payload.getOrder().getLink());
                        String siteName = "unknown";
                        if (payload.getOrder().getSourceSite() != null) {
                            Long sourceId = payload.getOrder().getSourceSite().getSource();
                            if (sourceId != null) {
                                try {
                                    siteName = SiteName.fromId(sourceId).name();
                                } catch (IllegalArgumentException e) {
                                    siteName = "unknown";
                                }
                            }
                        }
                        item.put("site", siteName);
                    }
                    item.put("module", payload.getModule() != null ?
                            payload.getModule().getName() : "unknown");
                    return item;
                })
                .collect(Collectors.toList());

        response.put("items", items);
        return ResponseEntity.ok(response);
    }


    @Operation(
            summary = "Получить размер очереди автоответов пользователя",
            description = "Возвращает количество заказов в очереди автоответов для данного пользователя."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Размер очереди",
            content = @Content(schema = @Schema(implementation = Integer.class))
    )
    @GetMapping("/queue-size")
    public ResponseEntity<Integer> getQueueSize(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid) {
        int size = userQueueManager.size(uuid);
        log.debug("Queue size for user {}: {}", uuid, size);
        return ResponseEntity.ok(size);
    }
}
