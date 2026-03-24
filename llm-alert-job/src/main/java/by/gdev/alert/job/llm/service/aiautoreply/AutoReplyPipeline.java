package by.gdev.alert.job.llm.service.aiautoreply;

import by.gdev.alert.job.llm.domain.dto.AiDecision;
import by.gdev.alert.job.llm.domain.dto.order.OrderDTO;
import by.gdev.alert.job.llm.service.aiautoreply.sender.ReplySender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AutoReplyPipeline {

    private final AiOrderAnalysisService analysisService;
    private final AiAutoReplyService replyService;
    private final ReplySender replySender;

    public void process(OrderDTO orderDTO) {

        AiDecision decision = analysisService.analyze(orderDTO);

        /*if (!decision.shouldReply()) {
            return;
        }*/

        String reply = replyService.finalizeReply(decision);
        replySender.send(orderDTO, reply, decision);
    }
}

