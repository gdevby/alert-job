package by.gdev.common.service.proxy;


import by.gdev.common.model.proxy.ProxyCredentials;
import by.gdev.common.model.proxy.ProxyState;
import by.gdev.common.service.IpGeoService;
import by.gdev.common.service.proxy.supplier.ProxySupplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProxyCheckerService {

    private final ProxySupplier proxySupplier;
    private final IpGeoService ipGeoService;


    public void checkAndUpdateProxy(ProxyCredentials proxy) {
        // Определяем страну
        updateProxyCountry(proxy);
        // Определяем доступность
        boolean available = isProxyAvailable(proxy);
        switch (proxy.getState()) {
            case NEW -> proxy.setState(available ? ProxyState.WARMING_UP : ProxyState.FAILED);
            case ACTIVE, WARMING_UP -> proxy.setState(available ? ProxyState.ACTIVE : ProxyState.QUARANTINE);
            case QUARANTINE -> proxy.setState(available ? ProxyState.ACTIVE : ProxyState.FAILED);
            case INACTIVE, BANNED, FAILED -> {
                // не трогаем
            }
        }
        //log.debug("Proxy {}:{} -> {}", proxy.getHost(), proxy.getPort(), proxy.getState());
    }

    private void updateProxyCountry(ProxyCredentials proxy) {
        synchronized (this) {
            String country = ipGeoService.getCountryByIp(proxy.getHost());
            proxy.setCountry(country);
            try {
                Thread.sleep(1500); // задержка между запросами к геосервису
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Задержка прервана при определении страны для прокси {}:{}", proxy.getHost(), proxy.getPort());
            }
        }
    }

    private boolean isProxyAvailable(ProxyCredentials proxy) {
        int attempts = 3;
        int timeoutMs = 5000;
        int delayBetweenAttemptsMs = 2000; // 2 секунды пауза

        for (int i = 0; i < attempts; i++) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(proxy.getHost(), proxy.getPort()), timeoutMs);
                return true; // если хотя бы одна попытка успешна — прокси рабочий
            } catch (Exception e) {
                log.warn("Попытка {} подключения к прокси {}:{} не удалась: {}",
                        i + 1, proxy.getHost(), proxy.getPort(), e.getMessage());
                try {
                    Thread.sleep(delayBetweenAttemptsMs);
                } catch (InterruptedException ignored) {}
            }
        }
        return false; // все 3 попытки провалились
    }

    public void checkAllProxies() {
        var proxies = proxySupplier.getProxies();
        int working = 0;
        int notWorking = 0;
        for (ProxyCredentials proxy : proxies) {
            checkAndUpdateProxy(proxy);

            switch (proxy.getState()) {
                case ACTIVE, WARMING_UP -> working++;
                default -> notWorking++;
            }
        }
        log.debug("Проверка прокси завершена. Статистика: РАБОЧИЕ = {}, НЕ РАБОЧИЕ = {}", working, notWorking);
    }

    /**
     * Проверяет переданный список прокси, обновляя их состояние.
     *
     * @param proxies список прокси для проверки
     */
    public void checkProxies(List<ProxyCredentials> proxies) {
        int total = proxies.size();
        int working = 0;
        int notWorking = 0;
        int processed = 0;
        int logInterval = Math.max(1, Math.min(20, total / 10));

        for (ProxyCredentials proxy : proxies) {
            checkAndUpdateProxy(proxy);
            switch (proxy.getState()) {
                case ACTIVE, WARMING_UP -> working++;
                default -> notWorking++;
            }
            processed++;
            if (processed % logInterval == 0 || processed == total) {
                log.info("Прогресс проверки: {}/{} прокси (рабочих: {}, нерабочих: {})",
                        processed, total, working, notWorking);
            }
        }
        log.info("Проверка списка прокси завершена. Статистика: РАБОЧИЕ = {}, НЕ РАБОЧИЕ = {}", working, notWorking);
    }
}



