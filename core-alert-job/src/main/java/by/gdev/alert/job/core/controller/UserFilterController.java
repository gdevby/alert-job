package by.gdev.alert.job.core.controller;

import java.util.Objects;

import by.gdev.alert.job.core.service.ai.AiFilterService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
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
import by.gdev.alert.job.core.model.WordDTO;
import by.gdev.alert.job.core.service.UserFilterService;
import by.gdev.common.model.HeaderName;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/api/")
@RequiredArgsConstructor
@Validated
@Tag(name = "User Filters", description = "Управление фильтрами")
public class UserFilterController {

	private final UserFilterService filterService;
    private final AiFilterService aiFilterService;

	@Hidden
	@GetMapping("user/title-word")
	public ResponseEntity<Mono<Page<WordDTO>>> getTitleWords(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
			@RequestParam("module_id") Long moduleId, @RequestParam(name = "name", required = false) String name,
			@RequestParam("page") Integer page) {
		return ResponseEntity.ok(filterService.showTitleWords(moduleId, uuid, name, page));
	}

	@Hidden
	@PostMapping("user/title-word")
	public Mono<ResponseEntity<WordDTO>> createTitleWord(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
			@RequestBody @Valid KeyWord keyWord) {
		return filterService.addTitleWord(keyWord, uuid);
	}

	@Hidden
	@GetMapping("user/description-word")
	public ResponseEntity<Mono<Page<WordDTO>>> getDescriptionWords(
			@RequestParam(name = "name", required = false) String name, @RequestParam("page") Integer page) {
		return ResponseEntity.ok(filterService.showDescriptionWords(name, page));
	}

	@Hidden
	@PostMapping("user/description-word")
	public Mono<ResponseEntity<WordDTO>> createDescriptionWord(@RequestBody @Valid KeyWord keyWord) {
		return filterService.addDescriptionWord(keyWord);
	}

	@Hidden
	@GetMapping("user/description-word-price")
	public ResponseEntity<Mono<Page<WordDTO>>> getDescriptionWordPrice(
			@RequestParam(name = "name", required = false) String name, @RequestParam("page") Integer page) {
		return ResponseEntity.ok(filterService.showDescriptionWordPrice(name, page));
	}

	@Hidden
	@PostMapping("user/description-word-price")
	public Mono<ResponseEntity<WordDTO>> createDescriptionWordPrice(
			@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid, @RequestBody @Valid KeyWord keyWord) {
		return filterService.addDescriptionWordPrice(keyWord, uuid);
	}

	@Hidden
	@GetMapping("user/module/{module_id}/filter")
	public ResponseEntity<Flux<FilterDTO>> getUserFilter(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
			@PathVariable("module_id") Long moduleId) {
		return ResponseEntity.ok(filterService.showUserFilters(uuid, moduleId));
	}

	@Hidden
	@PostMapping("user/module/{module_id}/filter")
	public ResponseEntity<Mono<FilterDTO>> addUserFilter(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
			@PathVariable("module_id") Long id, @RequestBody Filter filter) {
		return ResponseEntity.ok(filterService.createUserFilter(uuid, id, filter));
	}

	@Hidden
	@DeleteMapping("user/module/{id}/filter/{filter_id}")
	public Mono<ResponseEntity<Void>> deleteFilter(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
			@PathVariable("id") Long moduleId, @PathVariable("filter_id") Long filterId) {
		return filterService.removeUserFilter(uuid, moduleId, filterId);
	}

	@Hidden
	@PostMapping("user/module/{id}/current-filter/{filter_id}")
	public ResponseEntity<Mono<FilterDTO>> setCurrentFilter(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
			@PathVariable("id") Long moduleId, @PathVariable("filter_id") Long filterId) {
		return ResponseEntity.ok(filterService.replaceCurrentFilter(uuid, moduleId, filterId));
	}

	@Hidden
	@Transactional(readOnly = true)
	@GetMapping("user/module/{id}/current-filter")
	public ResponseEntity<FilterDTO> getCurrentFilter(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
			@PathVariable("id") Long moduleId) {
		FilterDTO f = filterService.showUserCurrentFilter(uuid, moduleId);
		return Objects.isNull(f) ? ResponseEntity.notFound().build() : ResponseEntity.ok(f);
	}

	@Hidden
	@PatchMapping("user/module/{id}/filter/{filter_id}")
	public ResponseEntity<Mono<FilterDTO>> changeFilter(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
			@PathVariable("id") Long moduleId, @PathVariable("filter_id") Long filterId, @RequestBody Filter filter) {
		return ResponseEntity.ok(filterService.updateFilter(uuid, moduleId, filterId, filter));
	}

	@Hidden
	@PatchMapping("user/filter/{filter_id}/title-word/{word_id}")
	public ResponseEntity<Mono<FilterDTO>> addTitleWordToFilter(@PathVariable("filter_id") Long filterId,
			@PathVariable("word_id") Long wordId) {
		return ResponseEntity.ok(filterService.createTitleWordToFilter(filterId, wordId));
	}

	@Hidden
	@DeleteMapping("user/filter/{filter_id}/title-word/{word_id}")
	public Mono<ResponseEntity<Void>> deleteTitleWordFromFilter(@PathVariable("filter_id") Long filterId,
			@PathVariable("word_id") Long wordId) {
		return filterService.removeTitleWordFromFilter(filterId, wordId);
	}

	@Hidden
	@PatchMapping("user/filter/{filter_id}/description-word/{word_id}")
	public ResponseEntity<Mono<FilterDTO>> addDescriptionWordToFilter(@PathVariable("filter_id") Long filterId,
			@PathVariable("word_id") Long wordId) {
		return ResponseEntity.ok(filterService.createDescriptionWordToFilter(filterId, wordId));
	}

	@Hidden
	@DeleteMapping("user/filter/{filter_id}/description-word/{word_id}")
	public Mono<ResponseEntity<Void>> deleteDescriptionWordFromFilter(@PathVariable("filter_id") Long filterId,
			@PathVariable("word_id") Long wordId) {
		return filterService.removeDescriptionWordFromFilter(filterId, wordId);
	}

	@Hidden
	@PatchMapping("user/negative-filter/{filter_id}/title-word/{word_id}")
	public ResponseEntity<Mono<FilterDTO>> addTitleWordToNegativeFilter(@PathVariable("filter_id") Long filterId,
			@PathVariable("word_id") Long wordId) {
		return ResponseEntity.ok(filterService.createTitleWordToNegativeFilter(filterId, wordId));
	}

	@Hidden
	@DeleteMapping("user/negative-filter/{filter_id}/title-word/{word_id}")
	public Mono<ResponseEntity<Void>> deleteTitleWordFromNegativeFilter(@PathVariable("filter_id") Long filterId,
			@PathVariable("word_id") Long wordId) {
		return filterService.removeTitleWordFromNegativeFilter(filterId, wordId);
	}

	@Hidden
	@PatchMapping("user/negative-filter/{filter_id}/description-word/{word_id}")
	public ResponseEntity<Mono<FilterDTO>> addDescriptionWordToNegativeFilter(@PathVariable("filter_id") Long filterId,
			@PathVariable("word_id") Long wordId) {
		return ResponseEntity.ok(filterService.createDescriptionWordToNegativeFilter(filterId, wordId));
	}

	@Hidden
	@DeleteMapping("user/negative-filter/{filter_id}/description-word/{word_id}")
	public Mono<ResponseEntity<Void>> deleteDescriptionWordFromNegativeFilter(@PathVariable("filter_id") Long filterId,
			@PathVariable("word_id") Long wordId) {
		return filterService.removeDescriptionWordFromNegativeFilter(filterId, wordId);
	}

	@Hidden
	@PatchMapping("user/filter/{filter_id}/description-word-price/{word_id}")
	public ResponseEntity<Mono<FilterDTO>> addDescriptionWordPriceToFilter(@PathVariable("filter_id") Long filterId,
			@PathVariable("word_id") Long wordId) {
		return ResponseEntity.ok(filterService.createDescriptionWordPriceToFilter(filterId, wordId));
	}

	@Hidden
	@DeleteMapping("user/filter/{filter_id}/description-word-price/{word_id}")
	public Mono<ResponseEntity<Void>> deleteDescriptionWordPriceFromFilter(@PathVariable("filter_id") Long filterId,
			@PathVariable("word_id") Long wordId) {
		return filterService.removeDescriptionWordPriceFromFilter(filterId, wordId);
	}

// ======================== ВИДИМЫЕ МЕТОДЫ С ДОКУМЕНТАЦИЕЙ ========================

	@Operation(
			summary = "Получить статус автоответа для модуля",
			description = "Возвращает true, если автоответ включен для указанного модуля, иначе false."
	)
	@ApiResponse(
			responseCode = "200",
			description = "Статус успешно получен",
			content = @Content(schema = @Schema(implementation = Boolean.class))
	)
	@ApiResponse(
			responseCode = "400",
			description = "Ошибка валидации"
	)
	@GetMapping("user/module/{module_id}/autoreply")
	public ResponseEntity<Boolean> getAutoReplyStatus(
			@Parameter(description = "UUID пользователя из заголовка", hidden = true)
			@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
			@Parameter(description = "ID модуля", example = "1")
			@PathVariable("module_id") Long moduleId
	) {
		boolean status = aiFilterService.getAutoReplyStatus(uuid, moduleId);
		return ResponseEntity.ok(status);
	}

	@Operation(
			summary = "Включить/выключить автоответ для модуля",
			description = "Устанавливает статус автоответа для указанного модуля."
	)
	@ApiResponse(
			responseCode = "200",
			description = "Статус успешно обновлён"
	)
	@ApiResponse(
			responseCode = "400",
			description = "Ошибка валидации"
	)
	@PostMapping("user/module/{module_id}/autoreply")
	public ResponseEntity<Void> setAutoReplyStatus(
			@Parameter(description = "UUID пользователя из заголовка", hidden = true)
			@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
			@Parameter(description = "ID модуля", example = "1")
			@PathVariable("module_id") Long moduleId,
			@Parameter(description = "Включить автоответ", example = "true")
			@RequestParam("enabled") boolean enabled
	) {
		aiFilterService.setAutoReplyStatus(uuid, moduleId, enabled);
		return ResponseEntity.ok().build();
	}
}