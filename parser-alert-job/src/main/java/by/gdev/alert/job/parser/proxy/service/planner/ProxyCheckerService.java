package by.gdev.alert.job.parser.proxy.service.planner;

import by.gdev.alert.job.parser.proxy.db.ProxyInfo;
import by.gdev.alert.job.parser.proxy.db.ProxyState;
import by.gdev.alert.job.parser.proxy.repository.ProxyInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.net.Socket;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProxyCheckerService {

    private final ProxyInfoRepository proxyInfoRepository;

    public void checkAndUpdateProxy(ProxyInfo proxy) {
        boolean available = isProxyAvailable(proxy);

        switch (proxy.getState()) {
            case NEW -> proxy.setState(available ? ProxyState.WARMING_UP : ProxyState.FAILED);
            case ACTIVE, WARMING_UP -> proxy.setState(available ? ProxyState.ACTIVE : ProxyState.QUARANTINE);
            case QUARANTINE -> proxy.setState(available ? ProxyState.ACTIVE : ProxyState.FAILED);
            case INACTIVE, BANNED, FAILED -> {
                // не трогаем
            }
        }

        proxyInfoRepository.save(proxy);
        log.info("Proxy {}:{} -> {}", proxy.getIp(), proxy.getPort(), proxy.getState());
    }

    private boolean isProxyAvailable(ProxyInfo proxy) {
        int attempts = 3;
        int timeoutMs = 5000;
        int delayBetweenAttemptsMs = 2000; // 2 секунды пауза

        for (int i = 0; i < attempts; i++) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(proxy.getIp(), proxy.getPort()), timeoutMs);
                return true; // если хотя бы одна попытка успешна — прокси рабочий
            } catch (Exception e) {
                log.warn("Попытка {} подключения к прокси {}:{} не удалась: {}",
                        i + 1, proxy.getIp(), proxy.getPort(), e.getMessage());
                try {
                    Thread.sleep(delayBetweenAttemptsMs);
                } catch (InterruptedException ignored) {}
            }
        }
        return false; // все 3 попытки провалились
    }


    public void checkAllProxies() {
        proxyInfoRepository.findAll().forEach(this::checkAndUpdateProxy);
    }
}



