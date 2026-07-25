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
        // Получаем актуальный список пользователей с автоответом
        List<String> users = coreClient.getUsersWithAutoReplyEnabled();
        if (users.isEmpty()) {
            userProxyMap.clear();
            log.debug("Нет пользователей с автоответом");
            return;
        }

        // Доступные рабочие прокси
        List<ProxyCredentials> availableProxies = proxySupplier.getProxies().stream()
                .filter(p -> p.getState() == ProxyState.ACTIVE || p.getState() == ProxyState.WARMING_UP)
                .collect(Collectors.toList());

        if (availableProxies.isEmpty()) {
            log.warn("Нет доступных РАБОЧИХ прокси для назначения!");
            userProxyMap.clear();
            return;
        }

        // Сохраняем старые назначения для пользователей, у которых прокси всё ещё рабочий
        Map<String, ProxyCredentials> oldMap = new HashMap<>(userProxyMap);
        Map<String, ProxyCredentials> newMap = new HashMap<>();

        for (String user : users) {
            ProxyCredentials oldProxy = oldMap.get(user);
            if (oldProxy != null && (oldProxy.getState() == ProxyState.ACTIVE || oldProxy.getState() == ProxyState.WARMING_UP)) {
                newMap.put(user, oldProxy);
                log.trace("Пользователь {} оставлен с прежним РАБОЧИМ прокси {}", user, oldProxy.getHost());
            }
        }

        // Пользователи, которым нужен новый прокси (новые или с нерабочим)
        List<String> usersWithoutProxy = users.stream()
                .filter(u -> !newMap.containsKey(u))
                .toList();

        if (!usersWithoutProxy.isEmpty()) {
            Collections.shuffle(availableProxies); // случайное распределение
            for (int i = 0; i < usersWithoutProxy.size(); i++) {
                ProxyCredentials proxy = availableProxies.get(i % availableProxies.size());
                newMap.put(usersWithoutProxy.get(i), proxy);
                log.debug("Пользователю {} назначен новый РАБОЧИЙ прокси {} (state={})",
                        usersWithoutProxy.get(i), proxy.getHost(), proxy.getState());
            }
        }

        // Заменяем карту
        userProxyMap.clear();
        userProxyMap.putAll(newMap);

        log.debug("Назначено {} прокси для {} пользователей (сохранено старых: {})",
                userProxyMap.size(), users.size(), newMap.size() - usersWithoutProxy.size());
    }

    public ProxyCredentials getProxyForUser(String userUuid) {
        return userProxyMap.get(userUuid);
    }
}