package by.gdev.alert.job.notification.controller;

import by.gdev.alert.job.notification.service.ai.proxy.AssignedProxyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
@Slf4j
public class ProxyAutoreplyController {

    private final AssignedProxyService assignedProxyService;

    @PostMapping("/reassign-proxies")
    public Mono<ResponseEntity<Void>> reassignProxies() {
        log.info("Получен запрос на перераспределение прокси");
        return assignedProxyService.reassignProxies()
                .thenReturn(ResponseEntity.ok().build());
    }
}