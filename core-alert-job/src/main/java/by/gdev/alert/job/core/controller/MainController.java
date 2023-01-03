package by.gdev.alert.job.core.controller;

import java.util.Objects;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import by.gdev.alert.job.core.service.CoreService;
import by.gdev.common.model.HeaderName;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/api/")
@RequiredArgsConstructor
public class MainController {

	private final CoreService coreService;

	@GetMapping("user/test")
	public Mono<ResponseEntity<String>> testAuth(
			@RequestHeader(required = false, name = HeaderName.UUID_USER_HEADER) String uuid) {
		if (Objects.isNull(uuid))
			return Mono.just(ResponseEntity.status(403).build());
		else
			return Mono.just(ResponseEntity.ok("ok"));
	}

	@PostMapping("/test-message")
	public ResponseEntity<Mono<Void>> testMailMessage(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid) {
		return ResponseEntity.ok(coreService.sendTestMessageOnMai(uuid));
	}

	@PatchMapping("user/alerts")
	public ResponseEntity<Mono<Boolean>> diactivateAlerts(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
			@RequestParam("status") boolean status) {
		return ResponseEntity.ok(coreService.changeAlertStatus(uuid, status));
	}

	@GetMapping("user/alerts")
	public ResponseEntity<Mono<Boolean>> getAlertStatus(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid) {
		return ResponseEntity.ok(coreService.showAlertStatus(uuid));
	}

	@PatchMapping("")
	public ResponseEntity<Mono<Boolean>> alertType(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
			@RequestParam("default_send") boolean defaultSend) {
		return ResponseEntity.ok(coreService.changeDefaultSendType(uuid, defaultSend));
	}
}