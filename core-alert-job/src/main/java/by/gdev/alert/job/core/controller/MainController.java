package by.gdev.alert.job.core.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/api/")
@RequiredArgsConstructor
public class MainController {
	@GetMapping("user/test")
	public Mono<ResponseEntity<String>> testAuth() {
		return Mono.just(ResponseEntity.ok("ok")); 
	}
}
