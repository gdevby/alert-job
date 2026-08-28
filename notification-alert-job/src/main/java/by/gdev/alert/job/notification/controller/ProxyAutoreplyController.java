package by.gdev.alert.job.notification.controller;

import by.gdev.alert.job.notification.service.ai.proxy.AssignedProxyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
@Slf4j
public class ProxyAutoreplyController {

    private final AssignedProxyService assignedProxyService;

    @PostMapping("/reassign-proxies")
    public ResponseEntity<Void> reassignProxies() {
        log.info("Получен запрос на перераспределение прокси");
        assignedProxyService.reassignProxies();
        return ResponseEntity.ok().build();
    }
}