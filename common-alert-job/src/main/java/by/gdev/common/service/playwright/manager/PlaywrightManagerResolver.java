package by.gdev.common.service.playwright.manager;

import by.gdev.common.model.SiteName;
import by.gdev.common.service.playwright.manager.impl.PlaywrightCamoufoxManager;
import by.gdev.common.service.playwright.manager.impl.PlaywrightManager;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlaywrightManagerResolver {

    private final PlaywrightCamoufoxManager camoufoxManager;
    @Getter
    private final PlaywrightManager localManager;

    @Value("${camoufox.enabled:false}")
    private boolean enabled;

    @Value("${camoufox.sites:}")
    private String camoufoxSitesRaw;

    private Set<String> camoufoxSites;

    @PostConstruct
    void init() {
        camoufoxSites = Arrays.stream(camoufoxSitesRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        log.info("Camoufox включён: {}, сайты: {}", enabled, camoufoxSites);
    }

    public PlaywrightBrowserManager resolve(SiteName site) {
        if (isEnabledCamoufox() && camoufoxSites.contains(site.name())) {
            log.info("[{}] Используем Camoufox", site);
            return camoufoxManager;
        }
        log.debug("[{}] Используем локальный Playwright", site);
        return localManager;
    }

    public boolean isEnabledCamoufox() {
        return enabled;
    }

}