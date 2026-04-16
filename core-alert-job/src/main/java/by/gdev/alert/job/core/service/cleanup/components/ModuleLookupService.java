package by.gdev.alert.job.core.service.cleanup.components;

import by.gdev.alert.job.core.model.db.AppUser;
import by.gdev.alert.job.core.model.db.OrderModules;
import by.gdev.alert.job.core.model.db.SourceSite;
import by.gdev.alert.job.core.repository.OrderModulesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ModuleLookupService {
    private final OrderModulesRepository orderModulesRepository;

    public Set<AppUser> getUsersFromSources(List<SourceSite> sources) {
        return sources.stream()
                .flatMap(source -> orderModulesRepository.findAllBySourceId(source.getId()).stream())
                .map(OrderModules::getUser)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

}
