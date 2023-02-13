package by.gdev.alert.job.core.controller;

import javax.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
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
import by.gdev.alert.job.core.model.OrderModulesDTO;
import by.gdev.alert.job.core.model.Source;
import by.gdev.alert.job.core.model.SourceDTO;
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
@Validated
public class MainController {

	private final CoreService coreService;

	@PostMapping("user/authentication")
	public Mono<ResponseEntity<String>> userAuthentication(
			@RequestHeader(required = false, name = HeaderName.UUID_USER_HEADER) String uuid,
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
	public ResponseEntity<Mono<NotificationAlertType>> getAlearType(
			@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid) {
		return ResponseEntity.ok(coreService.notificationUserAlertType(uuid));
	}

	@GetMapping("user/title-word")
	public ResponseEntity<Mono<Page<WordDTO>>> getTitleWords(@RequestParam(name = "name", required = false) String name,
			@RequestParam("page") Integer page) {
		return ResponseEntity.ok(coreService.showTitleWords(name, page));
	}

	@PostMapping("user/title-word")
	public Mono<ResponseEntity<WordDTO>> createTitleWord(@RequestBody @Valid KeyWord keyWord) {
		return coreService.addTitleWord(keyWord);
	}

	@GetMapping("user/technology-word")
	public ResponseEntity<Mono<Page<WordDTO>>> getTechnologyWords(
			@RequestParam(name = "name", required = false) String name, @RequestParam("page") Integer page) {
		return ResponseEntity.ok(coreService.showTechnologyWords(name, page));
	}

	@PostMapping("user/technology-word")
	public Mono<ResponseEntity<WordDTO>> createTechnologyWord(@RequestBody @Valid KeyWord keyWord) {
		return coreService.addTechnologyWord(keyWord);
	}

	@GetMapping("user/description-word")
	public ResponseEntity<Mono<Page<WordDTO>>> getDescriptionWords(
			@RequestParam(name = "name", required = false) String name, @RequestParam("page") Integer page) {
		return ResponseEntity.ok(coreService.showDescriptionWords(name, page));
	}

	@PostMapping("user/description-word")
	public Mono<ResponseEntity<WordDTO>> createDescriptionWord(@RequestBody @Valid KeyWord keyWord) {
		return coreService.addDescriptionWord(keyWord);
	}

	@PatchMapping("user/telegram")
	public ResponseEntity<Mono<Void>> addUserTelegram(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
			@RequestParam("telegram_id") Long telegramId) {
		return ResponseEntity.ok(coreService.changeUserTelegram(uuid, telegramId));
	}

	@GetMapping("user/order-module")
	public ResponseEntity<Flux<OrderModulesDTO>> getOrderModules(
			@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid) {
		return ResponseEntity.ok(coreService.showOrderModules(uuid));
	}

	@PostMapping("user/order-module")
	public Mono<ResponseEntity<OrderModulesDTO>> addOrderModule(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
			@RequestParam("name") String name) {
		return coreService.createOrderModules(uuid, name);
	}

	@DeleteMapping("user/order-module/{id}")
	public Mono<ResponseEntity<Void>> deleteOrderModule(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
			@PathVariable("id") Long id) {
		return coreService.removeOrderModules(uuid, id);
	}

	// TODO изменил
	@GetMapping("user/module/{module_id}/filter")
	public ResponseEntity<Flux<FilterDTO>> getUserFilter(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
			@PathVariable("module_id") Long moduleId) {
		return ResponseEntity.ok(coreService.showUserFilters(uuid, moduleId));

	}

	// TODO изменил
	@PostMapping("user/module/{module_id}/filter")
	public Mono<ResponseEntity<FilterDTO>> addUserFilter(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
			@PathVariable("module_id") Long id, @RequestBody Filter filter) {
		return coreService.createUserFilter(uuid, id, filter);
	}

	@DeleteMapping("user/module/{id}/filter/{filter_id}")
	public Mono<ResponseEntity<Void>> deleteFilter(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
			@PathVariable("id") Long moduleId, @PathVariable("filter_id") Long filterId) {
		return coreService.removeUserFilter(uuid, moduleId, filterId);
	}

	@PostMapping("user/module/{id}/current-filter/{filter_id}")
	public ResponseEntity<Mono<Void>> setCurrentFilter(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
			@PathVariable("id") Long moduleId, @PathVariable("filter_id") Long filterId) {
		return ResponseEntity.ok(coreService.replaceCurrentFilter(uuid, moduleId, filterId));
	}

	@GetMapping("user/module/{id}/current-filter")
	public ResponseEntity<Mono<FilterDTO>> getCurrentFilter(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
			@PathVariable("id") Long moduleId) {
		return ResponseEntity.ok(coreService.showUserCurrentFilter(uuid, moduleId));
	}

	@PatchMapping("user/module/{id}/current-filter/{filter_id}")
	public ResponseEntity<Mono<FilterDTO>> changeCurrrentFilter(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
			@PathVariable("id") Long moduleId, @PathVariable("filter_id") Long filterId, @RequestBody Filter filter) {
		return ResponseEntity.ok(coreService.updateCurrentFilter(uuid, moduleId, filterId, filter));
	}

	@PatchMapping("user/filter/{filter_id}/title-word/{word_id}")
	public ResponseEntity<Mono<Void>> addTitleWordToFilter(@PathVariable("filter_id") Long filterId,
			@PathVariable("word_id") Long wordId) {
		return ResponseEntity.ok(coreService.createTitleWordToFilter(filterId, wordId));
	}

	@DeleteMapping("user/filter/{filter_id}/title-word/{word_id}")
	public Mono<ResponseEntity<Void>> deleteTitleWordFromFilter(@PathVariable("filter_id") Long filterId,
			@PathVariable("word_id") Long wordId) {
		return coreService.removeTitleWordFromFilter(filterId, wordId);
	}

	@PatchMapping("user/filter/{filter_id}/technology-word/{word_id}")
	public ResponseEntity<Mono<Void>> addTechnologyWordToFilter(@PathVariable("filter_id") Long filterId,
			@PathVariable("word_id") Long wordId) {
		return ResponseEntity.ok(coreService.createTechnologyWordToFilter(filterId, wordId));
	}

	@DeleteMapping("user/filter/{filter_id}/technology-word/{word_id}")
	public Mono<ResponseEntity<Void>> deleteTechnologyWordFromFilter(@PathVariable("filter_id") Long filterId,
			@PathVariable("word_id") Long wordId) {
		return coreService.removeTechnologyWordFromFilter(filterId, wordId);
	}

	@PatchMapping("user/filter/{filter_id}/description-word/{word_id}")
	public ResponseEntity<Mono<Void>> addDescriptionWordToFilter(@PathVariable("filter_id") Long filterId,
			@PathVariable("word_id") Long wordId) {
		return ResponseEntity.ok(coreService.createDescriptionWordToFilter(filterId, wordId));
	}

	@DeleteMapping("user/filter/{filter_id}/description-word/{word_id}")
	public Mono<ResponseEntity<Void>> deleteDescriptionWordFromFilter(@PathVariable("filter_id") Long filterId,
			@PathVariable("word_id") Long wordId) {
		return coreService.removeDescriptionWordFromFilter(filterId, wordId);
	}

	@GetMapping("user/module/{id}/source")
	public ResponseEntity<Flux<SourceDTO>> getSouceSite(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
			@PathVariable("id") Long id) {
		return ResponseEntity.ok(coreService.showSourceSite(uuid, id));
	}

	@PostMapping("user/module/{id}/source")
	public Mono<ResponseEntity<SourceSiteDTO>> addSouceSite(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
			@PathVariable("id") Long id, @RequestBody @Valid Source source) {
		return coreService.createSourceSite(uuid, id, source);
	}

	@DeleteMapping("user/module/{id}/source/{source_id}")
	public Mono<ResponseEntity<Void>> deleteSourceSite(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
			@PathVariable("id") Long id, @PathVariable("source_id") Long sourceId) {
		return coreService.removeSourceSite(uuid, id, sourceId);
	}
}