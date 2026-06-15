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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class AutoReplyPipeline {

    private final LlmUserService llmUserService;
    private final AiOrderAnalysisService analysisService;
    private final List<ReplySender> replySenders;

    private final Set<String> sent = ConcurrentHashMap.newKeySet();

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
        AiDecision decision = analysisService.analyze(orderDTO, null, null, null);
        if (!decision.isShouldReply()) {
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
            String key = order.getLink();
            if (!sent.add(key)) {
                log.warn("LLM DUPLICATE DROPPED (already sent to Notification): {}", key);
                continue;
            }

            AiDecision decision = processItem(order, request.getUser(), request.getModule(), request.getTemplateId());
            String reply = finalizeReply(decision);

            if (reply != null && !reply.trim().isEmpty()) {
                getDummyReplySender().send(order, reply, decision);
                getNotificationyReplySender()
                        .sendToNotificationService(order, request.getUser(), request.getModule(),
                                decision, request.getCredentialId());
            }
        }
    }

    private AiDecision processItem(OrderDTO order, AiAppUserDTO user, AiOrderModulesDTO orderModule, Long templateId) {
        return analysisService.analyze(order, user, orderModule, templateId);
    }

    private String finalizeReply(AiDecision decision) {
        if (decision.getReply() == null) return null;
        return decision.getReply().trim();
    }

}

