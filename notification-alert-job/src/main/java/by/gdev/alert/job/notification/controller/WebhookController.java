package by.gdev.alert.job.notification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import by.gdev.alert.job.notification.service.WebhookService;
import by.gdev.common.model.WebhookNotification;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class WebhookController {

	private final WebhookService service;

	@PostMapping("webhook")
	public ResponseEntity<Mono<Void>> sendWebhook(@RequestBody WebhookNotification notification) {
		return ResponseEntity.ok(service.send(notification));
	}
}
