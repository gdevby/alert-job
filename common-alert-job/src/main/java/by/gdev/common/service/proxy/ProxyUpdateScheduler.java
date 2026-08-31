package by.gdev.common.service.proxy;

import by.gdev.common.model.proxy.ProxyCredentials;
import by.gdev.common.service.proxy.event.ProxyListUpdatedEvent;
import by.gdev.common.service.proxy.supplier.ProxySupplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProxyUpdateScheduler {

    private final ProxySupplier proxySupplier;
    private final ProxyCheckerService proxyCheckerService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${parser.proxy.refresh-interval-hours:24}")
    private int refreshIntervalHours;

    @Scheduled(fixedDelayString = "#{${parser.proxy.refresh-interval-hours:24} * 60 * 60 * 1000}")
    public void refreshProxies() {
        log.debug("Проверка списка прокси на обновления... (интервал {} ч.)", refreshIntervalHours);
        List<ProxyCredentials> fresh = proxySupplier.loadFreshProxies();
        if (fresh.isEmpty()) {
            log.warn("Обновление прокси пропущено — свежий список пустой!");
            return;
        }

        // Считаем текущие SUPPLIER-прокси ДО замены
        List<ProxyCredentials> current = proxySupplier.getProxies();
        long oldSupplierCount = current.stream()
                .filter(p -> p.getSource() == ProxySource.SUPPLIER)
                .count();

        // Заменяем только SUPPLIER-прокси
        proxySupplier.replaceSupplierProxies(fresh);

        // Новое количество SUPPLIER-прокси
        long newSupplierCount = fresh.size();
        long totalAfter = proxySupplier.getProxies().size();
        long apiCount = totalAfter - newSupplierCount;

        log.warn("Список SUPPLIER-прокси обновлён (было {} SUPPLIER, стало {} SUPPLIER)",
                oldSupplierCount, newSupplierCount);
        log.warn("Всего прокси после обновления: {} (SUPPLIER: {}, API: {})",
                totalAfter, newSupplierCount, apiCount);

        // Проверяем все прокси (включая API)
        proxyCheckerService.checkAllProxies();
        eventPublisher.publishEvent(new ProxyListUpdatedEvent(this, proxySupplier.getProxies()));
    }
}