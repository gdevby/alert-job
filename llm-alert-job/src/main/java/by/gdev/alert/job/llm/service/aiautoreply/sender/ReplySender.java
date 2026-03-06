package by.gdev.alert.job.llm.service.aiautoreply.sender;

import by.gdev.alert.job.llm.domain.dto.AiDecision;
import by.gdev.alert.job.llm.domain.dto.order.OrderDTO;

public interface ReplySender {
    void send(OrderDTO order, String replyText, AiDecision decision);
}
