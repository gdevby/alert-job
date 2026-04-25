package by.gdev.alert.job.llm.domain.dto;

import by.gdev.alert.job.llm.domain.dto.order.AiAppUserDTO;
import by.gdev.alert.job.llm.domain.dto.order.AiOrderModulesDTO;
import by.gdev.alert.job.llm.domain.dto.order.OrderDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationPayload {

    private AiAppUserDTO user;
    private AiOrderModulesDTO module;
    private OrderDTO order;
    private AiDecision decision;
}
