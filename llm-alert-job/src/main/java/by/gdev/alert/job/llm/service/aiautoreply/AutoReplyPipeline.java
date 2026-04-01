package by.gdev.alert.job.llm.service.aiautoreply;

import by.gdev.alert.job.llm.domain.dto.AiDecision;
import by.gdev.alert.job.llm.domain.dto.order.AiAppUserDTO;
import by.gdev.alert.job.llm.domain.dto.order.AiOrderModulesDTO;
import by.gdev.alert.job.llm.domain.dto.order.AiOrderRequest;
import by.gdev.alert.job.llm.domain.dto.order.OrderDTO;
import by.gdev.alert.job.llm.service.aiautoreply.sender.DummyReplySender;
import by.gdev.alert.job.llm.service.aiautoreply.sender.NotificationReplySender;
import by.gdev.alert.job.llm.service.aiautoreply.sender.ReplySender;
import by.gdev.alert.job.llm.service.template.LlmUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AutoReplyPipeline {

    private final LlmUserService llmUserService;
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

        AiDecision decision = analysisService.analyze(orderDTO, null, null);
        if (!decision.shouldReply()) {
            return;
        }

        String reply = finalizeReply(decision);
        if (reply != null && !reply.trim().isEmpty()) {
            getDummyReplySender().send(orderDTO, reply, decision);
        } else {
            log.warn("Reply is empty, skipping send to notification");
        }
    }

    public void process(AiOrderRequest request) {
        llmUserService.saveUser(request.getUser());
        for (OrderDTO order : request.getOrders()) {
            AiDecision decision = processItem(order, request.getUser(), request.getModule());
            String reply = finalizeReply(decision);
            if (reply != null){
                if (!reply.trim().isEmpty()) {
                    getDummyReplySender().send(order, reply, decision);
                    getNotificationyReplySender().sendToNotificationService(order, request.getUser(), request.getModule(), decision);
                }
                else {
                    log.warn("Reply is empty, skipping send to notification");
                }
            }
        }
    }

    private AiDecision processItem(OrderDTO order, AiAppUserDTO user, AiOrderModulesDTO orderModule) {
        return analysisService.analyze(order, user, orderModule);
    }

    private String finalizeReply(AiDecision decision) {
        if (decision.reply() == null) return null;
        return decision.reply().trim();
    }

}

