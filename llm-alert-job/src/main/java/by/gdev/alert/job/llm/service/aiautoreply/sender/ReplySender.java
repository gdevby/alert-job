package by.gdev.alert.job.llm.service.aiautoreply.sender;

import by.gdev.alert.job.llm.domain.dto.AiDecision;
import by.gdev.alert.job.llm.domain.dto.order.AiAppUserDTO;
import by.gdev.alert.job.llm.domain.dto.order.AiOrderModulesDTO;
import by.gdev.alert.job.llm.domain.dto.order.OrderDTO;

public interface ReplySender {
    void send(OrderDTO order, String replyText, AiDecision decision);
    void sendToNotificationService(OrderDTO order, AiAppUserDTO user, AiOrderModulesDTO module, AiDecision decision);
}
