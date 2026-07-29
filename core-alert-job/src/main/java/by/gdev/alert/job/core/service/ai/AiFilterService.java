package by.gdev.alert.job.core.service.ai;

import by.gdev.alert.job.core.model.db.OrderModules;
import by.gdev.alert.job.core.repository.OrderModulesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AiFilterService {
    private final OrderModulesRepository orderModulesRepository;

    public boolean getAutoReplyStatus(String uuid, Long moduleId){
        Optional<OrderModules> orderModule = orderModulesRepository.findById(moduleId);
        return orderModule.isPresent() && Boolean.TRUE.equals(orderModule.get().getAutoReplyEnabled());
    }

    public void setAutoReplyStatus(String uuid, Long moduleId, boolean status){
        Optional<OrderModules> orderModuleOptional = orderModulesRepository.findById(moduleId);
        if (orderModuleOptional.isPresent()){
            OrderModules orderModule = orderModuleOptional.get();
            if (orderModule.isAvailable()){
                orderModule.setAutoReplyEnabled(status);
                orderModulesRepository.save(orderModule);
            }
        }
    }

}
