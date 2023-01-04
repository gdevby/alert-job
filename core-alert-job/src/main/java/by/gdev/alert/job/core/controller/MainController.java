package by.gdev.alert.job.core.controller;

import java.util.Objects;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import by.gdev.alert.job.core.service.CoreService;
import by.gdev.common.model.HeaderName;
import by.gdev.common.model.KeyWord;
import by.gdev.common.model.WordDTO;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
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
	
	@PostMapping("user/authentication")
	public Mono<ResponseEntity<String>> userAuthentication(@RequestHeader(required = false, name = HeaderName.UUID_USER_HEADER) String uuid,
			@RequestHeader(required = false, name = HeaderName.EMAIL_USER_HEADER) String mail) {
		return coreService.authentication(uuid, mail);
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

	@PatchMapping("user/alerts/type")
	public ResponseEntity<Mono<Boolean>> alertType(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
			@RequestParam("default_send") boolean defaultSend) {
		return ResponseEntity.ok(coreService.changeDefaultSendType(uuid, defaultSend));
	}
	
	@GetMapping("user/title-word")
	public ResponseEntity<Flux<WordDTO>> getTitleWords(){
		return ResponseEntity.ok(coreService.showTitleWords());
	} 
	
	@PostMapping("user/title-word")
	public Mono<ResponseEntity<WordDTO>> createTitleWord(@RequestBody KeyWord keyWord){
		return coreService.addTitleWord(keyWord);
	}
	
	@GetMapping("user/technology-word")
	public ResponseEntity<Flux<WordDTO>> getTechnologyWords(){
		return ResponseEntity.ok(coreService.showTechnologyWords());
	} 
	
	@PostMapping("user/technology-word")
	public Mono<ResponseEntity<WordDTO>> createTechnologyWord(@RequestBody KeyWord keyWord){
		return coreService.addTechnologyWord(keyWord);
	}
	
	@GetMapping("user/description-word")
	public ResponseEntity<Flux<WordDTO>> getDescriptionWords(){
		return ResponseEntity.ok(coreService.showDescriptionWords());
	} 
	
	@PostMapping("user/description-word")
	public Mono<ResponseEntity<WordDTO>> createDescriptionWord(@RequestBody KeyWord keyWord){
		return coreService.addDescriptionWord(keyWord);
	}
}