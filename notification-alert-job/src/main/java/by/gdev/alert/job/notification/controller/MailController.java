package by.gdev.alert.job.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import by.gdev.alert.job.notification.service.MailService;
import by.gdev.common.model.UserNotification;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Mail & Telegram", description = "Отправка уведомлений по email и Telegram")
public class MailController {
	private final MailService service;

	@Operation(
			summary = "Отправить email-уведомление",
			description = "Отправляет уведомление пользователю на электронную почту."
	)
	@ApiResponse(
			responseCode = "200",
			description = "Запрос на отправку принят",
			content = @Content
	)
	@PostMapping("mail")
	public ResponseEntity<Mono<Void>> mailMessage(@RequestBody UserNotification userMail) {
		return ResponseEntity.ok(service.sendMessage(userMail));
	}


	@Operation(
			summary = "Отправить Telegram-уведомление",
			description = "Отправляет уведомление пользователю через Telegram (бот)."
	)
	@ApiResponse(
			responseCode = "200",
			description = "Запрос на отправку принят",
			content = @Content
	)
	@PostMapping("telegram")
	public ResponseEntity<Mono<Void>> sendTelegram(@RequestBody UserNotification userMail) {
		return ResponseEntity.ok(service.sendMessageToTelegram(userMail));
	}
}
