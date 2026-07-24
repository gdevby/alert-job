package by.gdev.common.service.proxy;

import by.gdev.common.model.proxy.ProxyCredentials;
import by.gdev.common.service.proxy.supplier.ProxySupplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProxyUpdateScheduler {

    private final ProxySupplier proxySupplier;
    private final ProxyCheckerService proxyCheckerService;

    @Value("${proxy.refresh-interval-hours:24}")
    private int refreshIntervalHours;

    @Scheduled(fixedDelayString = "#{${parser.proxy.refresh-interval-hours:24} * 60 * 60 * 1000}")
    public void refreshProxies() {
        log.debug("Проверка списка прокси на обновления... (интервал {} ч.)", refreshIntervalHours);
        List<ProxyCredentials> fresh = proxySupplier.loadFreshProxies();
        List<ProxyCredentials> current = proxySupplier.getProxies();
        if (fresh.isEmpty()) {
            log.warn("Обновление прокси пропущено — свежий список пустой!");
            return;
        }
        if (!fresh.equals(current)) {
            log.warn("Обнаружены изменения в списке прокси — выполняю обновление...");
            proxySupplier.replaceProxies(fresh);
            log.warn("Список прокси успешно обновлён ({} → {})",
                    current.size(), fresh.size());
            proxyCheckerService.checkAllProxies();
        } else {
            log.debug("Список прокси не изменился");
        }
    }
}
