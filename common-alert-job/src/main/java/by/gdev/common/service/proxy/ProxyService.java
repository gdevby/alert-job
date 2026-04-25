package by.gdev.common.service.proxy;

import by.gdev.common.model.proxy.ProxyCredentials;
import by.gdev.common.service.proxy.supplier.ProxySupplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class ProxyService {

    private final ProxySupplier proxySupplier;

    public List<ProxyCredentials> getNewAndActiveProxies() {
        return proxySupplier.getWorkingProxies();
    }

    /** * Возвращает случайный активный прокси из базы. */
    public ProxyCredentials getRandomActiveProxy() {
        List<ProxyCredentials> activeProxies = proxySupplier.getWorkingProxies();
        if (activeProxies.isEmpty()) { throw new IllegalStateException("Нет активных прокси!"); }
        return activeProxies.get(ThreadLocalRandom.current().nextInt(activeProxies.size()));
    }

}

