package by.gdev.alert.job.notification.service.ai.proxy;

import by.gdev.alert.job.notification.client.CoreUnifiedClient;
import by.gdev.alert.job.notification.model.dto.AppUserDTO;
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
    private final Map<String, String> userCountryCache = new ConcurrentHashMap<>();
    private long lastCacheUpdate = 0;
    private static final long CACHE_TTL_MS = 10 * 60 * 1000; // 10 минут

    @PostConstruct
    public void init() {
        reassignProxies();
    }

    private void refreshUserCountryCache() {
        long now = System.currentTimeMillis();
        if (now - lastCacheUpdate < CACHE_TTL_MS && !userCountryCache.isEmpty()) {
            return;
        }
        List<String> users = coreClient.getUsersWithAutoReplyEnabled();
        if (users.isEmpty()) {
            userCountryCache.clear();
            lastCacheUpdate = now;
            return;
        }
        for (String uuid : users) {
            try {
                AppUserDTO userDto = coreClient.getUserByUuid(uuid);
                if (userDto != null && userDto.getCountry() != null) {
                    userCountryCache.put(uuid, userDto.getCountry());
                } else {
                    userCountryCache.remove(uuid);
                }
            } catch (Exception e) {
                log.warn("Не удалось получить страну для пользователя {}", uuid, e);
                userCountryCache.remove(uuid);
            }
        }
        lastCacheUpdate = now;
        log.debug("Обновлён кэш стран для {} пользователей", userCountryCache.size());
    }

    public void reassignProxies() {
        refreshUserCountryCache();

        List<String> users = coreClient.getUsersWithAutoReplyEnabled();
        if (users.isEmpty()) {
            userProxyMap.clear();
            log.debug("Нет пользователей с автоответом");
            return;
        }

        List<ProxyCredentials> availableProxies = proxySupplier.getProxies().stream()
                .filter(p -> p.getState() == ProxyState.ACTIVE || p.getState() == ProxyState.WARMING_UP)
                .collect(Collectors.toList());

        if (availableProxies.isEmpty()) {
            log.warn("Нет доступных РАБОЧИХ прокси для назначения!");
            userProxyMap.clear();
            return;
        }

        Map<String, List<ProxyCredentials>> proxiesByCountry = availableProxies.stream()
                .filter(p -> p.getCountry() != null && !p.getCountry().isEmpty())
                .collect(Collectors.groupingBy(ProxyCredentials::getCountry));

        List<ProxyCredentials> proxiesWithoutCountry = availableProxies.stream()
                .filter(p -> p.getCountry() == null || p.getCountry().isEmpty())
                .collect(Collectors.toList());

        Map<String, ProxyCredentials> oldMap = new HashMap<>(userProxyMap);
        Map<String, ProxyCredentials> newMap = new HashMap<>();

        for (String userUuid : users) {
            ProxyCredentials oldProxy = oldMap.get(userUuid);
            if (oldProxy != null && (oldProxy.getState() == ProxyState.ACTIVE || oldProxy.getState() == ProxyState.WARMING_UP)) {
                newMap.put(userUuid, oldProxy);
                availableProxies.remove(oldProxy);
                String country = oldProxy.getCountry();
                if (country != null && proxiesByCountry.containsKey(country)) {
                    proxiesByCountry.get(country).remove(oldProxy);
                    if (proxiesByCountry.get(country).isEmpty()) {
                        proxiesByCountry.remove(country);
                    }
                } else {
                    proxiesWithoutCountry.remove(oldProxy);
                }
            }
        }

        List<String> usersWithoutProxy = users.stream()
                .filter(u -> !newMap.containsKey(u))
                .toList();

        for (String userUuid : usersWithoutProxy) {
            String userCountry = userCountryCache.get(userUuid);
            ProxyCredentials assignedProxy = null;

            if (userCountry != null && !userCountry.isEmpty() && proxiesByCountry.containsKey(userCountry)) {
                List<ProxyCredentials> candidates = proxiesByCountry.get(userCountry);
                if (!candidates.isEmpty()) {
                    assignedProxy = candidates.remove(0);
                    if (candidates.isEmpty()) {
                        proxiesByCountry.remove(userCountry);
                    }
                }
            }

            if (assignedProxy == null) {
                if (!proxiesWithoutCountry.isEmpty()) {
                    assignedProxy = proxiesWithoutCountry.remove(0);
                } else if (!availableProxies.isEmpty()) {
                    Collections.shuffle(availableProxies);
                    assignedProxy = availableProxies.remove(0);
                }
            }

            if (assignedProxy == null) {
                log.warn("Недостаточно прокси для всех пользователей");
                break;
            }

            newMap.put(userUuid, assignedProxy);
            log.debug("Пользователю {} (страна {}) назначен прокси {} ({}), страна прокси {}",
                    userUuid, userCountry, assignedProxy.getHost(), assignedProxy.getPort(),
                    assignedProxy.getCountry() != null ? assignedProxy.getCountry() : "UNKNOWN");
        }

        userProxyMap.clear();
        userProxyMap.putAll(newMap);
        log.debug("Назначено {} прокси для {} пользователей (сохранено старых: {})",
                userProxyMap.size(), users.size(), newMap.size() - usersWithoutProxy.size());
    }

    public ProxyCredentials getProxyForUser(String userUuid) {
        return userProxyMap.get(userUuid);
    }
}