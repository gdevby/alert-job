package by.gdev.alert.job.llm.service.aiautoreply.sender;

import by.gdev.alert.job.llm.domain.dto.AiDecision;
import by.gdev.alert.job.llm.domain.dto.order.AiAppUserDTO;
import by.gdev.alert.job.llm.domain.dto.order.AiOrderModulesDTO;
import by.gdev.alert.job.llm.domain.dto.order.OrderDTO;
import by.gdev.common.model.NotificationTypeEnum;
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

        log.info("""
==================== AUTO-REPLY #{} ====================
ORDER:
  title: {}
  link: {}
  price: {}
  message: {}

AI DECISION:
  valid: {}
  prefix: {}
  reply: {}

FINAL TEXT:
{}
===========================================================
""",
                num,
                order.getTitle(),
                order.getLink(),
                order.getPrice() != null ? order.getPrice().getPrice() : "<нет>",
                order.getMessage(),
                decision.isValid(),
                decision.getPrefix(),
                decision.getReply(),
                replyText
        );
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
            Long credentialId,
            NotificationTypeEnum notificationType
    ) {
    }
}
