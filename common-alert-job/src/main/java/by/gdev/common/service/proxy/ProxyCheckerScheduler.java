package by.gdev.common.service.proxy;

import by.gdev.common.model.proxy.ProxyCredentials;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProxyCheckerScheduler {

    private final ProxyCheckerService proxyCheckerService;

    private final ProxyAdditionalService proxyAdditionalService;

    @PostConstruct
    public void init() {
        run();
    }

    // Запускаем каждые 15 минут
    @Scheduled(cron = "0 */15 * * * *")
    public void run() {
        log.debug("Запуск проверки прокси...");
        proxyCheckerService.checkAllProxies();
        List<ProxyCredentials> ruProxies =  proxyAdditionalService.fetchProxies();
        proxyCheckerService.checkProxies(ruProxies);
    }
}

