package by.gdev.alert.job.notification.service.ai.proxy;

import by.gdev.alert.job.notification.client.CoreUnifiedClient;
import by.gdev.alert.job.notification.model.dto.ModuleSiteDto;
import by.gdev.common.model.SiteName;
import by.gdev.common.model.proxy.ProxyCredentials;
import by.gdev.common.model.proxy.ProxyState;
import by.gdev.common.service.IpGeoService;
import by.gdev.common.service.proxy.supplier.ProxySupplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssignedProxyService {

    private final ProxySupplier proxySupplier;
    private final CoreUnifiedClient coreClient;
    private final IpGeoService ipGeoService;

    // userUuid -> (moduleId -> proxy)
    private final Map<String, Map<Long, ProxyCredentials>> userModuleProxyMap = new ConcurrentHashMap<>();
    private final Map<SiteName, String> siteCountryCache = new ConcurrentHashMap<>();

    /**
     * Перераспределяет прокси для всех пользователей и их модулей.
     * Для каждого модуля определяется страна сайта и подбирается прокси.
     */
    public Mono<Void> reassignProxies() {
        return Mono.fromCallable(() -> {
                    log.debug("Перераспределение прокси для пользователей и модулей");

                    List<String> users = coreClient.getUsersWithAutoReplyEnabled().block();
                    if (users == null || users.isEmpty()) {
                        userModuleProxyMap.clear();
                        log.info("Нет пользователей с автоответом");
                        return null;
                    }

                    Map<String, List<ModuleSiteDto>> userModulesMap = new HashMap<>();
                    for (String userUuid : users) {
                        List<ModuleSiteDto> modules = coreClient.getAutoReplyEnabledModules(userUuid);
                        if (!modules.isEmpty()) {
                            userModulesMap.put(userUuid, modules);
                        }
                    }

                    if (userModulesMap.isEmpty()) {
                        userModuleProxyMap.clear();
                        log.info("У пользователей нет активных модулей с автоответом");
                        return null;
                    }

                    List<ProxyCredentials> workingProxies = proxySupplier.getWorkingProxies();
                    if (workingProxies.isEmpty()) {
                        log.warn("Нет доступных РАБОЧИХ прокси для назначения!");
                        userModuleProxyMap.clear();
                        return null;
                    }

                    Map<String, Map<Long, ProxyCredentials>> newMap = new HashMap<>();

                    for (String userUuid : users) {
                        Map<Long, ProxyCredentials> oldUserMap = userModuleProxyMap.get(userUuid);
                        if (oldUserMap != null) {
                            Map<Long, ProxyCredentials> newUserMap = new HashMap<>();
                            for (Map.Entry<Long, ProxyCredentials> entry : oldUserMap.entrySet()) {
                                ProxyCredentials proxy = entry.getValue();
                                if (proxy.getState() == ProxyState.ACTIVE || proxy.getState() == ProxyState.WARMING_UP) {
                                    newUserMap.put(entry.getKey(), proxy);
                                    workingProxies.remove(proxy);
                                }
                            }
                            if (!newUserMap.isEmpty()) {
                                newMap.put(userUuid, newUserMap);
                            }
                        }
                    }

                    for (String userUuid : users) {
                        List<ModuleSiteDto> modules = userModulesMap.get(userUuid);
                        if (modules == null) continue;

                        Map<Long, ProxyCredentials> userMap = newMap.computeIfAbsent(userUuid, k -> new HashMap<>());

                        for (ModuleSiteDto dto : modules) {
                            Long moduleId = dto.getModuleId();
                            if (userMap.containsKey(moduleId)) continue;

                            SiteName site = SiteName.fromId(dto.getSiteId());
                            String targetCountry = getSiteCountry(site);
                            ProxyCredentials assignedProxy = null;

                            if (targetCountry != null) {
                                List<ProxyCredentials> candidates = workingProxies.stream()
                                        .filter(p -> targetCountry.equalsIgnoreCase(p.getCountry()))
                                        .toList();
                                if (!candidates.isEmpty()) {
                                    assignedProxy = candidates.get(0);
                                    workingProxies.remove(assignedProxy);
                                }
                            }

                            if (assignedProxy == null && !workingProxies.isEmpty()) {
                                assignedProxy = workingProxies.remove(0);
                            }

                            if (assignedProxy == null) {
                                log.warn("Недостаточно прокси для пользователя {} модуля {}", userUuid, moduleId);
                                continue;
                            }

                            userMap.put(moduleId, assignedProxy);
                            log.info("Пользователю {} для модуля {} (сайт {}) назначен прокси {}:{}, страна {}",
                                    userUuid, moduleId, site, assignedProxy.getHost(), assignedProxy.getPort(), assignedProxy.getCountry());
                        }
                    }

                    userModuleProxyMap.clear();
                    userModuleProxyMap.putAll(newMap);
                    log.info("Перераспределение завершено. Назначено прокси для {} пользователей", userModuleProxyMap.size());
                    return null;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    /**
     * Возвращает прокси для конкретного пользователя и модуля.
     */
    public ProxyCredentials getProxyForUserAndModule(String userUuid, Long moduleId) {
        Map<Long, ProxyCredentials> userMap = userModuleProxyMap.get(userUuid);
        if (userMap != null) {
            ProxyCredentials proxy = userMap.get(moduleId);
            if (proxy != null && (proxy.getState() == ProxyState.ACTIVE || proxy.getState() == ProxyState.WARMING_UP)) {
                return proxy;
            }
        }
        // Если прокси нет или нерабочий – перераспределяем на лету
        log.warn("Прокси для пользователя {} модуля {} не найден или нерабочий, выполняем перераспределение", userUuid, moduleId);
        reassignProxies();
        Map<Long, ProxyCredentials> newUserMap = userModuleProxyMap.get(userUuid);
        if (newUserMap != null) {
            return newUserMap.get(moduleId);
        }
        return null;
    }

    /**
     * Старый метод для обратной совместимости (использует первый модуль).
     * @deprecated используйте {@link #getProxyForUserAndModule(String, Long)}
     */
    @Deprecated
    public ProxyCredentials getProxyForUser(String userUuid) {
        Map<Long, ProxyCredentials> userMap = userModuleProxyMap.get(userUuid);
        if (userMap != null && !userMap.isEmpty()) {
            return userMap.values().iterator().next();
        }
        return null;
    }

    /**
     * Возвращает страну сайта по его домену (с кэшированием).
     */
    private String getSiteCountry(SiteName site) {
        return siteCountryCache.computeIfAbsent(site, s -> {
            try {
                String domain = s.getDomain();
                InetAddress address = InetAddress.getByName(domain);
                String ip = address.getHostAddress();
                return ipGeoService.getCountryByIp(ip);
            } catch (Exception e) {
                log.warn("Ошибка определения страны для сайта {}: {}", s, e.getMessage());
                return null;
            }
        });
    }
}