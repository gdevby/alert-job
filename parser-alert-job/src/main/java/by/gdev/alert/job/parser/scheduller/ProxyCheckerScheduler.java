package by.gdev.alert.job.parser.scheduller;

import by.gdev.alert.job.parser.proxy.service.planner.ProxyCheckerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProxyCheckerScheduler {

    private final ProxyCheckerService proxyCheckerService;

    // Запускаем каждые 15 минут
    @Scheduled(cron = "0 */15 * * * *")
    public void run() {
        log.debug("Запуск проверки прокси...");
        proxyCheckerService.checkAllProxies();
    }
}

