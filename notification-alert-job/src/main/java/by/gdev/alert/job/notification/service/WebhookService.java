package by.gdev.alert.job.notification.service;

import java.net.URI;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import by.gdev.alert.job.notification.config.MetricsConfig;
import by.gdev.common.model.WebhookNotification;
import by.gdev.common.util.WebhookUrlValidator;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Доставка уведомления на пользовательский webhook.
 *
 * Адрес приходит от пользователя, поэтому проверяется здесь ещё раз — модуль
 * стоит внутри docker-сети, и запрос по внутреннему адресу был бы SSRF.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

	private static final String EVENT_HEADER = "X-AlertJob-Event";
	private static final String USER_HEADER = "X-AlertJob-User";

	private final WebClient webClient;
	private final ApplicationContext context;

	@Value("${webhook.timeout.seconds:15}")
	private int timeoutSeconds;

	public Mono<Void> send(WebhookNotification notification) {
		Counter positive = context.getBean(MetricsConfig.COUNTER_WEBHOOK_POSITIVE, Counter.class);
		Counter negative = context.getBean(MetricsConfig.COUNTER_WEBHOOK_NEGATIVE, Counter.class);

		if (notification == null || !WebhookUrlValidator.isValid(notification.getUrl())) {
			log.warn("Webhook rejected: url is not allowed");
			negative.increment();
			return Mono.empty();
		}

		return webClient.post().uri(URI.create(notification.getUrl()))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.header(EVENT_HEADER, String.valueOf(notification.getType()))
				.header(USER_HEADER, String.valueOf(notification.getUserUuid()))
				.bodyValue(notification)
				.retrieve()
				.bodyToMono(Void.class)
				.timeout(Duration.ofSeconds(timeoutSeconds))
				.doOnSuccess(r -> {
					log.info("sent webhook for user {}, orders {}", notification.getUserUuid(),
							notification.getOrders() == null ? 0 : notification.getOrders().size());
					positive.increment();
				})
				.onErrorResume(ex -> {
					log.warn("Could not send webhook for user {}: {}", notification.getUserUuid(), ex.getMessage());
					negative.increment();
					return Mono.error(ex);
				})
				.then();
	}
}
