package by.gdev.common.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Полезная нагрузка доставки уведомления на пользовательский webhook.
 * В отличие от почты и телеграма отправляются не отрендеренные строки,
 * а сами заказы — получатель разбирает их машинно.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WebhookNotification {

	/** Адрес, на который нужно доставить уведомление. */
	private String url;

	/** UUID пользователя-получателя (Keycloak subject). */
	private String userUuid;

	/** Тип события. */
	private NotificationType type = NotificationType.ORDER;

	/** Время формирования доставки, ISO-8601 UTC. */
	private String sentAt;

	/** Заказы, попавшие под фильтры пользователя. */
	private List<OrderDTO> orders;
}
