package by.gdev.common.service.proxy;

import by.gdev.common.model.proxy.ProxyCredentials;
import by.gdev.common.model.proxy.ProxyState;
import by.gdev.common.service.proxy.event.ProxyListUpdatedEvent;
import by.gdev.common.service.proxy.supplier.ProxySupplier;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProxyCheckerScheduler {

    private final ProxyCheckerService proxyCheckerService;
    private final ProxyAdditionalService proxyAdditionalService;
    private final ProxySupplier proxySupplier;
    private final ApplicationEventPublisher eventPublisher;

    @PostConstruct
    public void init() {
        run();
    }

    @Scheduled(cron = "0 */30 * * * *")
    public void run() {
        log.info("Запуск обновления и проверки прокси...");

        // Получаем API-прокси
        List<ProxyCredentials> apiProxies = proxyAdditionalService.fetchProxies();
        log.info("API-прокси: {}", apiProxies.size());

        // Добавляем их в supplier (заменяет старые API-прокси)
        if (!apiProxies.isEmpty()) {
            proxySupplier.addApiProxies(apiProxies);
        }

        // Получаем весь объединённый список
        List<ProxyCredentials> allProxies = proxySupplier.getProxies();
        log.info("Всего прокси после добавления API: {}", allProxies.size());

        // Проверяем все прокси
        proxyCheckerService.checkProxies(allProxies);
        log.info("Проверка завершена.");

        // Логируем распределение по странам
        logCountryDistribution(allProxies);

        // Считаем активные
        long active = allProxies.stream()
                .filter(p -> p.getState() == ProxyState.ACTIVE || p.getState() == ProxyState.WARMING_UP)
                .count();
        eventPublisher.publishEvent(new ProxyListUpdatedEvent(this, allProxies));
        log.info("Активных: {}", active);
        log.info("Готово. Всего: {}", allProxies.size());
    }

    private void logCountryDistribution(List<ProxyCredentials> proxies) {
        if (proxies.isEmpty()) {
            log.info("Список прокси пуст.");
            return;
        }
        Map<String, Long> map = proxies.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getCountry() != null ? p.getCountry() : "UNKNOWN",
                        Collectors.counting()
                ));
        log.info("РАСПРЕДЕЛЕНИЕ ПО СТРАНАМ:");
        map.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(e -> log.info("   {}: {} прокси", e.getKey(), e.getValue()));
    }
}