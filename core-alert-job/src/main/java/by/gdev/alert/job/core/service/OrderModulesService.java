package by.gdev.alert.job.core.service;

import by.gdev.alert.job.core.repository.OrderModulesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderModulesService {

    private final OrderModulesRepository orderModulesRepository;

    public List<String> findDistinctUserUuidsWithAutoReplyEnabled() {
        return orderModulesRepository.findDistinctUserUuidsWithAutoReplyEnabled();
    }
}