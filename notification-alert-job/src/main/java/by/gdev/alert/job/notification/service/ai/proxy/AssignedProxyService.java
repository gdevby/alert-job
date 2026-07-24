package by.gdev.alert.job.notification.service.ai.proxy;

import by.gdev.alert.job.notification.client.CoreUnifiedClient;
import by.gdev.common.model.proxy.ProxyCredentials;
import by.gdev.common.model.proxy.ProxyState;
import by.gdev.common.service.proxy.supplier.ProxySupplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@DependsOn("proxyUpdateScheduler")
@RequiredArgsConstructor
@Slf4j
public class AssignedProxyService {

    private final ProxySupplier proxySupplier;
    private final CoreUnifiedClient coreClient;

    private final Map<String, ProxyCredentials> userProxyMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        reassignProxies();
    }

    public void reassignProxies() {
        // Получаем пользователей с автоответом из Core
        List<String> users = coreClient.getUsersWithAutoReplyEnabled();
        if (users.isEmpty()) {
            userProxyMap.clear();
            log.debug("Нет пользователей с автоответом");
            return;
        }

        // Берём только рабочие прокси
        List<ProxyCredentials> availableProxies = proxySupplier.getProxies().stream()
                .filter(p -> p.getState() == ProxyState.ACTIVE || p.getState() == ProxyState.WARMING_UP)
                .collect(Collectors.toList());

        if (availableProxies.isEmpty()) {
            log.warn("Нет доступных РАБОЧИХ прокси для назначения!");
            userProxyMap.clear();
            return;
        }

        log.debug("Доступно рабочих прокси: {}", availableProxies.size());

        // ПЕРЕМЕШИВАЕМ список для случайного распределения
        Collections.shuffle(availableProxies);

        userProxyMap.clear();
        for (int i = 0; i < users.size(); i++) {
            ProxyCredentials proxy = availableProxies.get(i % availableProxies.size());
            userProxyMap.put(users.get(i), proxy);
            log.debug("Пользователю {} назначен РАБОЧИЙ прокси {} (state={})",
                    users.get(i), proxy.getHost(), proxy.getState());
        }

        log.debug("Назначено {} прокси для {} пользователей", userProxyMap.size(), users.size());
    }

    public ProxyCredentials getProxyForUser(String userUuid) {
        return userProxyMap.get(userUuid);
    }
}