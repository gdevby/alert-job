package by.gdev.alert.job.llm.service.aiautoreply.sender;

import by.gdev.alert.job.llm.domain.dto.AiDecision;
import by.gdev.alert.job.llm.domain.dto.order.AiAppUserDTO;
import by.gdev.alert.job.llm.domain.dto.order.AiOrderModulesDTO;
import by.gdev.alert.job.llm.domain.dto.order.OrderDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Тестовый отправщик автоответов.
 * <p>
 * Вместо реальной отправки сообщений выводит подробную информацию
 * о заказе, решении AI и сгенерированном ответе в лог.
 * Используется для отладки и локальной разработки.
 */
@Slf4j
@Component
public class DummyReplySender implements ReplySender {

    /**
     * Счётчик отправленных сообщений.
     * Используется для нумерации блоков в логах.
     */
    private static final AtomicInteger COUNTER = new AtomicInteger(1);

    /**
     * Добавляет строку в лог‑блок, если значение не null.
     */
    private void appendIfNotNull(StringBuilder sb, String label, Object value) {
        if (value == null) return;
        sb.append(label).append(": ").append(value).append("\n");
    }

    /**
     * Формирует подробный лог‑блок с информацией о заказе,
     * решении AI и итоговом автоответе.
     *
     * @param order      заказ
     * @param replyText  текст автоответа
     * @param decision   решение AI
     */
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
        appendIfNotNull(sb, "shouldReply", decision.isShouldReply());
        appendIfNotNull(sb, "confidence", decision.getConfidence());
        appendIfNotNull(sb, "reason", decision.getReason());
        appendIfNotNull(sb, "categoryMatchReason", decision.getCategoryMatchReason());
        appendIfNotNull(sb, "subcategoryMatchReason", decision.getSubcategoryMatchReason());

        if (decision.getMatchedKeywords() != null && !decision.getMatchedKeywords().isEmpty()) {
            appendIfNotNull(sb, "matchedKeywords", decision.getMatchedKeywords());
        }
        if (decision.getMissedKeywords() != null && !decision.getMissedKeywords().isEmpty()) {
            appendIfNotNull(sb, "missedKeywords", decision.getMissedKeywords());
        }

        sb.append("\n--- Автоответ ---\n");
        sb.append(replyText).append("\n");

        sb.append("===================================================\n\n\n");

        log.debug(sb.toString());
    }

    /**
     * Тестовый отправщик не взаимодействует с Notification‑сервисом.
     * Метод оставлен пустым намеренно.
     */
    @Override
    public void sendToNotificationService(
            OrderDTO order,
            AiAppUserDTO user,
            AiOrderModulesDTO module,
            AiDecision decision,
            Long credentialId
    ) {
        // no-op
    }
}
