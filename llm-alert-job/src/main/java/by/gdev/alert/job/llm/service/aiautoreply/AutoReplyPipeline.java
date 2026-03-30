package by.gdev.alert.job.llm.service.aiautoreply;

import by.gdev.alert.job.llm.domain.dto.AiDecision;
import by.gdev.alert.job.llm.domain.dto.order.AiAppUserDTO;
import by.gdev.alert.job.llm.domain.dto.order.AiOrderModulesDTO;
import by.gdev.alert.job.llm.domain.dto.order.AiOrderRequest;
import by.gdev.alert.job.llm.domain.dto.order.OrderDTO;
import by.gdev.alert.job.llm.service.aiautoreply.sender.DummyReplySender;
import by.gdev.alert.job.llm.service.aiautoreply.sender.NotificationReplySender;
import by.gdev.alert.job.llm.service.aiautoreply.sender.ReplySender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AutoReplyPipeline {

    private final AiOrderAnalysisService analysisService;
    private final List<ReplySender> replySenders;

    private ReplySender getDummyReplySender() {
        return replySenders.stream()
                .filter(sender -> sender instanceof DummyReplySender)
                .findFirst()
                .orElse(null);
    }

    private ReplySender getNotificationyReplySender() {
        return replySenders.stream()
                .filter(sender -> sender instanceof NotificationReplySender)
                .findFirst()
                .orElse(null);
    }

    public void process(OrderDTO orderDTO) {

        AiDecision decision = analysisService.analyze(orderDTO);
        if (!decision.shouldReply()) {
            return;
        }

        String reply = finalizeReply(decision);
        getDummyReplySender().send(orderDTO, reply, decision);
    }

    public void process(AiOrderRequest request) {
        for (OrderDTO order : request.getOrders()) {
            AiDecision decision = processItem(order, request.getUser(), request.getModule());
            String reply = finalizeReply(decision);
            if (reply != null){
                getDummyReplySender().send(order, reply, decision);
                getNotificationyReplySender().sendToNotificationService(order, request.getUser(), request.getModule(), decision);
            }
        }
    }

    private AiDecision processItem(OrderDTO order, AiAppUserDTO user, AiOrderModulesDTO module) {
        return analysisService.analyze(order);
    }

    private String finalizeReply(AiDecision decision) {
        if (decision.reply() == null) return null;
        return decision.reply().trim();
    }

}

