package by.gdev.alert.job.core.service;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import by.gdev.alert.job.core.model.db.AppUser;
import by.gdev.alert.job.core.repository.AppUserRepository;
import by.gdev.common.util.WebhookUrlValidator;
import by.gdev.common.model.NotificationType;
import by.gdev.common.model.OrderDTO;
import by.gdev.common.model.WebhookNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

/**
 * Доставка заказов на пользовательский webhook.
 *
 * Канал независимый: он не заменяет почту и телеграм, а работает рядом с ними,
 * поэтому пользователь может получать письма и одновременно отдавать заказы
 * в свою систему. Расписание тишины (UserAlertTime) на webhook не влияет —
 * оно про людей, а не про интеграции.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookSenderService {

	private static final String SEND_WEBHOOK_URL = "http://notification:8019/webhook";

	private final WebClient webClient;
	private final AppUserRepository userRepository;

	@Value("${webhook.max.failures:10}")
	private int maxWebhookFailures;

	public void sendOrders(AppUser user, List<OrderDTO> orders) {
		send(user, orders, NotificationType.ORDER);
	}

	public void send(AppUser user, List<OrderDTO> orders, NotificationType type) {
		String url = user.getWebhookUrl();
		if (StringUtils.isEmpty(url) || orders == null || orders.isEmpty()) {
			return;
		}
		if (!user.isSwitchOffAlerts()) {
			log.debug("Webhook skipped, alerts are off for user {}", user.getUuid());
			return;
		}
		Integer failCount = user.getWebhookFailCount();
		if (failCount != null && failCount >= maxWebhookFailures) {
			log.debug("Webhook disabled for user {} after {} failures", user.getUuid(), failCount);
			return;
		}
		if (!WebhookUrlValidator.isValid(url)) {
			// Адрес мог стать невалидным уже после сохранения — например, домен
			// начал резолвиться во внутренний адрес.
			log.warn("Webhook url of user {} is no longer valid, skipping", user.getUuid());
			return;
		}

		WebhookNotification notification = new WebhookNotification(url, user.getUuid(), type,
				Instant.now().toString(), orders);

		webClient.post().uri(SEND_WEBHOOK_URL).bodyValue(notification).retrieve().bodyToMono(Void.class)
				.subscribe(success -> resetFailures(user), error -> registerFailure(user, error));
	}

	private void resetFailures(AppUser user) {
		if (user.getWebhookFailCount() != null && user.getWebhookFailCount() > 0) {
			user.setWebhookFailCount(0);
			userRepository.save(user);
		}
		log.debug("Webhook delivered for user {}", user.getUuid());
	}

	private void registerFailure(AppUser user, Throwable error) {
		int newCount = user.getWebhookFailCount() == null ? 1 : user.getWebhookFailCount() + 1;
		user.setWebhookFailCount(newCount);
		userRepository.save(user);
		log.warn("Webhook failed for user {}, fail count {}: {}", user.getUuid(), newCount, error.getMessage());
	}
}
