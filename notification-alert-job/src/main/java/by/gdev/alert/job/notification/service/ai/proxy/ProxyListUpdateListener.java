package by.gdev.alert.job.notification.service.ai.proxy;

import by.gdev.common.service.proxy.event.ProxyListUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProxyListUpdateListener {

    private final AssignedProxyService assignedProxyService;

    @EventListener
    public void onProxyListUpdated(ProxyListUpdatedEvent event) {
        log.info("Получено событие обновления прокси. Перераспределяем...");
        assignedProxyService.reassignProxies();
    }
}