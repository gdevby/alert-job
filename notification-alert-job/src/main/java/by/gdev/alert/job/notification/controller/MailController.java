package by.gdev.alert.job.notification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import by.gdev.alert.job.notification.model.UserMail;
import by.gdev.alert.job.notification.service.MailService;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class MailController {

	private final MailService service;

	@PostMapping("mail")
	public ResponseEntity<Mono<Void>> mailMessage(@RequestBody UserMail userMail) {
		return ResponseEntity.ok(service.sendMessage(userMail));
	}

	@PostMapping("telegram")
	public ResponseEntity<Mono<Void>> sendTelegram(@RequestBody UserMail userMail) {
		return ResponseEntity.ok(service.sendMessageToTelegram(userMail));
	}
}
