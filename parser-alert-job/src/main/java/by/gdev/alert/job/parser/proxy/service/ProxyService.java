package by.gdev.alert.job.parser.proxy.service;

import by.gdev.alert.job.parser.proxy.db.ProxyInfo;
import by.gdev.alert.job.parser.proxy.db.ProxyState;
import by.gdev.alert.job.parser.proxy.db.ProxyType;
import by.gdev.alert.job.parser.proxy.repository.ProxyInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class ProxyService {
    private final ProxyInfoRepository proxyInfoRepository;

    public List<ProxyInfo> getActiveHttpProxies() {
        return proxyInfoRepository.findByTypeAndState(ProxyType.HTTP, ProxyState.ACTIVE);
    }

    public List<ProxyInfo> getAllProxies(){
        return proxyInfoRepository.findAll();
    }

    public List<ProxyInfo> getNewAndActiveProxies() {
        return proxyInfoRepository.findByStateIn(Arrays.asList(ProxyState.NEW, ProxyState.ACTIVE));
    }

    /** * Возвращает случайный активный прокси из базы. */
    public ProxyInfo getRandomActiveProxy() {
        List<ProxyInfo> activeProxies = getNewAndActiveProxies();
        if (activeProxies.isEmpty()) { throw new IllegalStateException("Нет активных прокси!"); }
        return activeProxies.get(ThreadLocalRandom.current().nextInt(activeProxies.size()));
    }


    public void banProxy(Long id) {
        proxyInfoRepository.findById(id).ifPresent(proxy -> {
            proxy.setState(ProxyState.BANNED);
            proxyInfoRepository.save(proxy);
        });
    }
}

