package by.gdev.alert.job.core.service.ai;

import by.gdev.alert.job.core.model.ai.AiAppUserDTO;
import by.gdev.alert.job.core.model.ai.AiOrderModulesDTO;
import by.gdev.alert.job.core.model.ai.AiOrderRequest;
import by.gdev.alert.job.core.model.db.AppUser;
import by.gdev.alert.job.core.model.db.OrderModules;
import by.gdev.common.model.OrderDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AiOrderRequestMapper {

    public AiOrderRequest build(AppUser user, OrderModules orderModule, List<OrderDTO> orders) {

        AiOrderRequest req = new AiOrderRequest();

        // Пользователь
        AiAppUserDTO userDto = new AiAppUserDTO();
        userDto.setUuid(user.getUuid());
        userDto.setEmail(user.getEmail());
        userDto.setTelegram(user.getTelegram());
        userDto.setSwitchOffAlerts(user.isSwitchOffAlerts());
        userDto.setDefaultSendType(user.isDefaultSendType());
        req.setUser(userDto);

        // Модуль
        AiOrderModulesDTO moduleDto = new AiOrderModulesDTO();
        moduleDto.setId(orderModule.getId());
        moduleDto.setName(orderModule.getName());
        req.setModule(moduleDto);

        // Заказы
        req.setOrders(orders);

        return req;
    }
}
