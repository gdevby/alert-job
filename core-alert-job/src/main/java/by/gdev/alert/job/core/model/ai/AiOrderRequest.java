package by.gdev.alert.job.core.model.ai;

import by.gdev.common.model.OrderDTO;
import lombok.Data;

import java.util.List;

@Data
public class AiOrderRequest {
    private AiAppUserDTO user;
    private AiOrderModulesDTO module;
    private List<OrderDTO> orders;
}

