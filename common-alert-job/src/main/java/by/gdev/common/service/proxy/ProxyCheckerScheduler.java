package by.gdev.common.service.proxy;

import by.gdev.common.model.proxy.ProxyCredentials;
import by.gdev.common.service.proxy.supplier.ProxySupplier;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProxyCheckerScheduler {

    private final ProxyCheckerService proxyCheckerService;
    private final ProxyAdditionalService proxyAdditionalService;
    private final ProxySupplier proxySupplier;

    @PostConstruct
    public void init() {
        run();
    }

    // Запускаем каждые 15 минут
    @Scheduled(cron = "0 */15 * * * *")
    public void run() {
        log.debug("Запуск обновления и проверки прокси...");

        // Получаем текущий список прокси из supplier
        List<ProxyCredentials> mainProxies = proxySupplier.getProxies();

        // Загружаем дополнительные прокси из внешнего API
        List<ProxyCredentials> additionalProxies = proxyAdditionalService.fetchProxies();

        // Объединяем списки, избегая дубликатов по host:port
        Map<String, ProxyCredentials> combinedMap = new HashMap<>();
        // Добавляем основные
        for (ProxyCredentials p : mainProxies) {
            combinedMap.put(p.getHost() + ":" + p.getPort(), p);
        }
        // Добавляем дополнительные (перезаписываем, если такой ключ уже есть)
        for (ProxyCredentials p : additionalProxies) {
            String key = p.getHost() + ":" + p.getPort();
            combinedMap.put(key, p);
        }

        List<ProxyCredentials> combinedList = new ArrayList<>(combinedMap.values());

        // Проверяем объединённый список (обновляем состояния и страны)
        proxyCheckerService.checkProxies(combinedList);

        // Обновляем основной список в ProxySupplier (чтобы другие сервисы имели актуальные данные)
        proxySupplier.replaceProxies(combinedList);

        log.debug("Проверка и обновление прокси завершены. Всего прокси: {}", combinedList.size());
    }
}