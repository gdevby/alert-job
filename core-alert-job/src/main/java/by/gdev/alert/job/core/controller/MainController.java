package by.gdev.alert.job.core.controller;

import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import by.gdev.alert.job.core.model.Filter;
import by.gdev.alert.job.core.model.FilterDTO;
import by.gdev.alert.job.core.model.KeyWord;
import by.gdev.alert.job.core.model.Source;
import by.gdev.alert.job.core.model.WordDTO;
import by.gdev.alert.job.core.service.CoreService;
import by.gdev.common.model.HeaderName;
import by.gdev.common.model.NotificationAlertType;
import by.gdev.common.model.SourceSiteDTO;
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
	
	@GetMapping("user/alerts/type")
	public ResponseEntity<Mono<NotificationAlertType>> getAlearType(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid){
		return ResponseEntity.ok(coreService.notificationUserAlertType(uuid));
	}
	
	@GetMapping("user/title-word")
	public ResponseEntity<Mono<Page<WordDTO>>> getTitleWords(@RequestParam(name = "name", required = false) String name, @RequestParam("page") Integer page){
		return ResponseEntity.ok(coreService.showTitleWords(name, page));
	} 
	
	@PostMapping("user/title-word")
	public Mono<ResponseEntity<WordDTO>> createTitleWord(@RequestBody KeyWord keyWord){
		return coreService.addTitleWord(keyWord);
	}
	
	@GetMapping("user/technology-word")
	public ResponseEntity<Mono<Page<WordDTO>>> getTechnologyWords(@RequestParam(name = "name", required = false) String name, @RequestParam("page") Integer page){
		return ResponseEntity.ok(coreService.showTechnologyWords(name, page));
	} 
	
	@PostMapping("user/technology-word")
	public Mono<ResponseEntity<WordDTO>> createTechnologyWord(@RequestBody KeyWord keyWord){
		return coreService.addTechnologyWord(keyWord);
	}
	
	@GetMapping("user/description-word")
	public ResponseEntity<Mono<Page<WordDTO>>> getDescriptionWords(@RequestParam(name = "name", required = false) String name, @RequestParam("page") Integer page){
		return ResponseEntity.ok(coreService.showDescriptionWords(name, page));
	} 
	
	@PostMapping("user/description-word")
	public Mono<ResponseEntity<WordDTO>> createDescriptionWord(@RequestBody KeyWord keyWord){
		return coreService.addDescriptionWord(keyWord);
	}
	
	@PatchMapping("user/telegram")
	public ResponseEntity<Mono<Void>> addUserTelegram(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
			@RequestParam("telegram_id") Long telegramId) {
		return ResponseEntity.ok(coreService.changeUserTelegram(uuid, telegramId));
	}
	
	@GetMapping("user/filter")
	public ResponseEntity<Flux<FilterDTO>> getUserFilter(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid){
		return ResponseEntity.ok(coreService.showUserFilters(uuid));
		
	}
	
	@PostMapping("user/filter")
	public ResponseEntity<Mono<FilterDTO>> addUserFilter(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
			@RequestBody Filter filter) {
		return ResponseEntity.ok(coreService.createUserFilter(uuid, filter));
	}
	
	@PatchMapping("user/filter/{id}")
	public Mono<ResponseEntity<FilterDTO>> changeUserFilter(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
			@PathVariable("id") Long filterId, @RequestBody Filter filter) {
		return coreService.updateUserFilter(uuid, filterId, filter);
	}
	
	@DeleteMapping("user/filter/{id}")
	public Mono<ResponseEntity<Void>> deleteFilter(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
			@PathVariable("id") Long filterId){
		return coreService.removeUserFilter(uuid, filterId);
	}
	
	@PatchMapping("user/filter/{id}/current")
	public ResponseEntity<Mono<Void>> setCurrentFilter(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
			@PathVariable("id") Long filterId){
		return ResponseEntity.ok(coreService.currentFilter(uuid, filterId));
	}
	
	@PatchMapping("user/filter/{filter_id}/title-word/{word_id}")
	public ResponseEntity<Mono<Void>> addTitleWordToFilter(@PathVariable("filter_id") Long filterId, @PathVariable("word_id") Long wordId){
		return ResponseEntity.ok(coreService.createTitleWordToFilter(filterId, wordId));
	}
	
	@PatchMapping("user/filter/{filter_id}/technology-word/{word_id}")
	public ResponseEntity<Mono<Void>> addTechnologyWordToFilter(@PathVariable("filter_id") Long filterId, @PathVariable("word_id") Long wordId){
		return ResponseEntity.ok(coreService.createTechnologyWordToFilter(filterId, wordId));
	}
	
	@PatchMapping("user/filter/{filter_id}/description-word/{word_id}")
	public ResponseEntity<Mono<Void>> addDescriptionWordToFilter(@PathVariable("filter_id") Long filterId, @PathVariable("word_id") Long wordId){
		return ResponseEntity.ok(coreService.createDescriptionWordToFilter(filterId, wordId));
	}
	
	@GetMapping("user/source")
	public ResponseEntity<Flux<SourceSiteDTO>> getSouceSite(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid){
		return ResponseEntity.ok(coreService.showSourceSite(uuid));
	}
	
	@PostMapping("user/source")
	public ResponseEntity<Mono<SourceSiteDTO>> addSouceSite(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid, @RequestBody Source source){
		return ResponseEntity.ok(coreService.createSourceSite(uuid, source));
	}
	
	@DeleteMapping("user/source/{source_id}")
	public Mono<ResponseEntity<Void>> deleteSourceSite(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid, @PathVariable("source_id") Long sourceId){
		return coreService.removeSourceSite(uuid, sourceId);
	}
}