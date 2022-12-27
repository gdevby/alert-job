package by.gdev.alert.job.core.controller;

import java.util.Objects;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import by.gdev.common.model.HeaderName;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/api/")
@RequiredArgsConstructor
public class MainController {
	@GetMapping("user/test")
	public Mono<ResponseEntity<String>> testAuth(
			@RequestHeader(required = false, name = HeaderName.UUID_USER_HEADER) String uuid) {
		if (Objects.isNull(uuid))
			return Mono.just(ResponseEntity.status(403).build());
		else
			return Mono.just(ResponseEntity.ok("ok"));
	}
}
