package by.gdev.alert.job.llm.service.aiautoreply;

import by.gdev.alert.job.llm.domain.dto.AiDecision;
import org.springframework.stereotype.Service;

@Service
public class AiAutoReplyService {

    public String finalizeReply(AiDecision decision) {
        if (decision.reply() == null) return null;

        return decision.reply().trim();
    }
}
