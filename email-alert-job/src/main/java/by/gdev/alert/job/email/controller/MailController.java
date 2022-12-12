package by.gdev.alert.job.email.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import by.gdev.alert.job.email.model.UserMail;
import by.gdev.alert.job.email.service.MailService;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class MailController {
	
	private final MailService service;
	
	
	@GetMapping("mail")
	public ResponseEntity<Mono<Void>> mailMessage(@RequestBody UserMail userMail) {
		return ResponseEntity.ok(service.sendMessage(userMail));
	}
}
