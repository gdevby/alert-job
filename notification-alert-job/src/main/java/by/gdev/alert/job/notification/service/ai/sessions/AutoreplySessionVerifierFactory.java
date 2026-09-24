package by.gdev.alert.job.notification.service.ai.sessions;

import by.gdev.common.model.SiteName;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AutoreplySessionVerifierFactory {

    private final Map<SiteName, AutoreplySessionVerifier> registry = new HashMap<>();

    public AutoreplySessionVerifierFactory(List<AutoreplySessionVerifier> verifiers) {
        for (AutoreplySessionVerifier verifier : verifiers) {
            registry.put(verifier.getSiteName(), verifier);
        }
    }

    public AutoreplySessionVerifier get(SiteName site) {
        AutoreplySessionVerifier verifier = registry.get(site);
        if (verifier == null) {
            throw new IllegalArgumentException("Session verifier not found for site: " + site);
        }
        return verifier;
    }

    public Optional<AutoreplySessionVerifier> find(SiteName site) {
        return Optional.ofNullable(registry.get(site));
    }
}
