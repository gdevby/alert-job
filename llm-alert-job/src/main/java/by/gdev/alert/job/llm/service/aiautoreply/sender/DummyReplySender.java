package by.gdev.alert.job.llm.service.aiautoreply.sender;

import by.gdev.alert.job.llm.domain.dto.AiDecision;
import by.gdev.alert.job.llm.domain.dto.order.AiAppUserDTO;
import by.gdev.alert.job.llm.domain.dto.order.AiOrderModulesDTO;
import by.gdev.alert.job.llm.domain.dto.order.OrderDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class DummyReplySender implements ReplySender {

    private static final AtomicInteger COUNTER = new AtomicInteger(1);

    @Override
    public void send(OrderDTO order, String replyText, AiDecision decision) {

        int num = COUNTER.getAndIncrement();

        StringBuilder sb = new StringBuilder();

        sb.append("\n===================================================\n");
        sb.append("=============== ЗАКАЗ #").append(num).append(" ===============\n\n");

        // Краткая информация о заказе
        appendIfNotNull(sb, "Заголовок", order.getTitle());
        appendIfNotNull(sb, "Ссылка", order.getLink());
        appendIfNotNull(sb, "Описание", order.getMessage());
        if (order.getPrice() != null) {
            appendIfNotNull(sb, "Цена", order.getPrice().getPrice());
        }

        sb.append("\n--- Решение AI ---\n");
        appendIfNotNull(sb, "shouldReply", decision.shouldReply());
        appendIfNotNull(sb, "confidence", decision.confidence());
        appendIfNotNull(sb, "reason", decision.reason());
        appendIfNotNull(sb, "categoryMatchReason", decision.categoryMatchReason());
        appendIfNotNull(sb, "subcategoryMatchReason", decision.subcategoryMatchReason());

        if (decision.matchedKeywords() != null && !decision.matchedKeywords().isEmpty()) {
            appendIfNotNull(sb, "matchedKeywords", decision.matchedKeywords());
        }
        if (decision.missedKeywords() != null && !decision.missedKeywords().isEmpty()) {
            appendIfNotNull(sb, "missedKeywords", decision.missedKeywords());
        }

        sb.append("\n--- Автоответ ---\n");
        sb.append(replyText).append("\n");

        sb.append("===================================================\n\n\n");

        log.info(sb.toString());
    }

    @Override
    public void sendToNotificationService(OrderDTO order, AiAppUserDTO user, AiOrderModulesDTO module, AiDecision decision) {

    }

    private void appendIfNotNull(StringBuilder sb, String label, Object value) {
        if (value == null) return;
        sb.append(label).append(": ").append(value).append("\n");
    }
}

