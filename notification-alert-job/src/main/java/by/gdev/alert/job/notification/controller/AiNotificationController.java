package by.gdev.alert.job.notification.controller;

import by.gdev.alert.job.notification.model.dto.*;
import by.gdev.alert.job.notification.service.ai.credential.UserCredentialService;
import by.gdev.alert.job.notification.service.ai.parser.AutoreplyParserFactory;
import by.gdev.alert.job.notification.service.ai.parser.AutoreplyPlaywrightParser;
import by.gdev.alert.job.notification.service.ai.queue.UserQueueManager;
import by.gdev.common.model.SiteName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/notification/api/ai")
@Slf4j
@RequiredArgsConstructor
public class AiNotificationController {
    private final UserCredentialService userCredentialService;
    private final AutoreplyParserFactory parserFactory;
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
        // 1) не держать ссылку в памяти вечно (иначе Set разрастётся),
        // 2) позволить повторно обработать этот же заказ, если он придёт позже,
        // 3) не блокировать повторную отправку, если предыдущая попытка упала.
        Schedulers.boundedElastic().schedule(() -> dedup.remove(key), 5, TimeUnit.MINUTES);
        log.debug("QUEUE: accepted AI decision {}", key);
        //кладем пайлоад в очередь обработки пользователя
        userQueueManager.submit(payload);
        // получаем размер очереди для пользователя
        int userQueueSize = userQueueManager.size(payload.getUser().getUuid());
        return Mono.just(ResponseEntity.accepted().body(Map.of("status", "queued", "queueSize", userQueueSize)));
    }

    @GetMapping("/testlogin")
    public Mono<ResponseEntity<?>> receiveTestCase(
            @RequestParam String uuid,
            @RequestParam String site
    ) {
        AiNotificationPayload payload = buildTestAiDecision(uuid);

        // Проверяем enum
        SiteName siteEnum;
        try {
            siteEnum = SiteName.valueOf(site.toUpperCase());
        } catch (Exception e) {
            return Mono.just(
                    ResponseEntity.badRequest().body(
                            Map.of("error", "Unknown site: " + site)
                    )
            );
        }

        // Проверяем наличие парсера
        AutoreplyPlaywrightParser parser;
        try {
            parser = parserFactory.getParser(siteEnum);
        } catch (IllegalArgumentException e) {
            return Mono.just(
                    ResponseEntity.badRequest().body(
                            Map.of("error", "Parser not found for site: " + siteEnum)
                    )
            );
        }

        // Основная логика
        return userCredentialService.getMonoUserCredentials(payload)
                .flatMap(credential -> Mono.fromCallable(() -> {
                                    boolean ok = parser.sendAutoreply(credential, payload);
                                    log.info("Parser result = {}", ok);
                                    return ok;
                                })
                                .subscribeOn(Schedulers.boundedElastic())
                                .map(ok -> ResponseEntity.ok().build())
                );
    }


    private AiNotificationPayload buildTestAiDecision(String uid){
        AiNotificationPayload payload = new AiNotificationPayload();
        AiAppUserDTO user = new AiAppUserDTO();
        user.setUuid(uid);
        user.setEmail("provodnik_new@mail.ru");
        user.setDefaultSendType(true);
        payload.setUser(user);

        AiOrderModulesDTO module = new AiOrderModulesDTO();
        module.setId(16L);
        module.setName("weblancer");
        payload.setModule(module);

        OrderDTO order = new OrderDTO();
        SourceSiteDTO site = new SourceSiteDTO();
        site.setSource(4L);
        order.setSourceSite(site);
        order.setLink("https://www.weblancer.net/freelance/sluzhba-podderzhki-56/administrator-onlain-platformi-udalyonno-1265808/");

        AiDecision decision = new AiDecision(
                true,
                0.92,
                "Совпадение по ключевым словам",
                "Готов выполнить задачу!",
                List.of("java", "spring"),
                List.of("docker"),
                "Категория совпала",
                "Подкатегория совпала"
        );
        payload.setDecision(decision);
        payload.setOrder(order);

        return payload;
    }
}
