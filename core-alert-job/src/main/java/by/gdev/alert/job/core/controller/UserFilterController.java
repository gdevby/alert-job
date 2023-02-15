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
public class UserFilterController {
	
	private final UserFilterService filterService;
	
	
	@GetMapping("user/title-word")
	public ResponseEntity<Mono<Page<WordDTO>>> getTitleWords(@RequestParam(name = "name", required = false) String name,
			@RequestParam("page") Integer page) {
		return ResponseEntity.ok(filterService.showTitleWords(name, page));
	}

	@PostMapping("user/title-word")
	public Mono<ResponseEntity<WordDTO>> createTitleWord(@RequestBody @Valid KeyWord keyWord) {
		return filterService.addTitleWord(keyWord);
	}

	@GetMapping("user/technology-word")
	public ResponseEntity<Mono<Page<WordDTO>>> getTechnologyWords(
			@RequestParam(name = "name", required = false) String name, @RequestParam("page") Integer page) {
		return ResponseEntity.ok(filterService.showTechnologyWords(name, page));
	}

	@PostMapping("user/technology-word")
	public Mono<ResponseEntity<WordDTO>> createTechnologyWord(@RequestBody @Valid KeyWord keyWord) {
		return filterService.addTechnologyWord(keyWord);
	}

	@GetMapping("user/description-word")
	public ResponseEntity<Mono<Page<WordDTO>>> getDescriptionWords(
			@RequestParam(name = "name", required = false) String name, @RequestParam("page") Integer page) {
		return ResponseEntity.ok(filterService.showDescriptionWords(name, page));
	}

	@PostMapping("user/description-word")
	public Mono<ResponseEntity<WordDTO>> createDescriptionWord(@RequestBody @Valid KeyWord keyWord) {
		return filterService.addDescriptionWord(keyWord);
	}
	
	@GetMapping("user/module/{module_id}/filter")
	public ResponseEntity<Flux<FilterDTO>> getUserFilter(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
			@PathVariable("module_id") Long moduleId) {
		return ResponseEntity.ok(filterService.showUserFilters(uuid, moduleId));
	}

	@PostMapping("user/module/{module_id}/filter")
	public Mono<ResponseEntity<FilterDTO>> addUserFilter(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
			@PathVariable("module_id") Long id, @RequestBody Filter filter) {
		return filterService.createUserFilter(uuid, id, filter);
	}

	@DeleteMapping("user/module/{id}/filter/{filter_id}")
	public Mono<ResponseEntity<Void>> deleteFilter(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
			@PathVariable("id") Long moduleId, @PathVariable("filter_id") Long filterId) {
		return filterService.removeUserFilter(uuid, moduleId, filterId);
	}

	@PostMapping("user/module/{id}/current-filter/{filter_id}")
	public ResponseEntity<Mono<Void>> setCurrentFilter(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
			@PathVariable("id") Long moduleId, @PathVariable("filter_id") Long filterId) {
		return ResponseEntity.ok(filterService.replaceCurrentFilter(uuid, moduleId, filterId));
	}

	@GetMapping("user/module/{id}/current-filter")
	public ResponseEntity<Mono<FilterDTO>> getCurrentFilter(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
			@PathVariable("id") Long moduleId) {
		return ResponseEntity.ok(filterService.showUserCurrentFilter(uuid, moduleId));
	}

	@PatchMapping("user/module/{id}/current-filter/{filter_id}")
	public ResponseEntity<Mono<FilterDTO>> changeCurrrentFilter(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
			@PathVariable("id") Long moduleId, @PathVariable("filter_id") Long filterId, @RequestBody Filter filter) {
		return ResponseEntity.ok(filterService.updateCurrentFilter(uuid, moduleId, filterId, filter));
	}

	@PatchMapping("user/filter/{filter_id}/title-word/{word_id}")
	public ResponseEntity<Mono<Void>> addTitleWordToFilter(@PathVariable("filter_id") Long filterId,
			@PathVariable("word_id") Long wordId) {
		return ResponseEntity.ok(filterService.createTitleWordToFilter(filterId, wordId));
	}

	@DeleteMapping("user/filter/{filter_id}/title-word/{word_id}")
	public Mono<ResponseEntity<Void>> deleteTitleWordFromFilter(@PathVariable("filter_id") Long filterId,
			@PathVariable("word_id") Long wordId) {
		return filterService.removeTitleWordFromFilter(filterId, wordId);
	}

	@PatchMapping("user/filter/{filter_id}/technology-word/{word_id}")
	public ResponseEntity<Mono<Void>> addTechnologyWordToFilter(@PathVariable("filter_id") Long filterId,
			@PathVariable("word_id") Long wordId) {
		return ResponseEntity.ok(filterService.createTechnologyWordToFilter(filterId, wordId));
	}

	@DeleteMapping("user/filter/{filter_id}/technology-word/{word_id}")
	public Mono<ResponseEntity<Void>> deleteTechnologyWordFromFilter(@PathVariable("filter_id") Long filterId,
			@PathVariable("word_id") Long wordId) {
		return filterService.removeTechnologyWordFromFilter(filterId, wordId);
	}

	@PatchMapping("user/filter/{filter_id}/description-word/{word_id}")
	public ResponseEntity<Mono<Void>> addDescriptionWordToFilter(@PathVariable("filter_id") Long filterId,
			@PathVariable("word_id") Long wordId) {
		return ResponseEntity.ok(filterService.createDescriptionWordToFilter(filterId, wordId));
	}

	@DeleteMapping("user/filter/{filter_id}/description-word/{word_id}")
	public Mono<ResponseEntity<Void>> deleteDescriptionWordFromFilter(@PathVariable("filter_id") Long filterId,
			@PathVariable("word_id") Long wordId) {
		return filterService.removeDescriptionWordFromFilter(filterId, wordId);
	}

	@PatchMapping("user/negative-filter/{filter_id}/title-word/{word_id}")
	public ResponseEntity<Mono<FilterDTO>> addTitleWordToNegativeFilter(@PathVariable("filter_id") Long filterId,
			@PathVariable("word_id") Long wordId) {
		return ResponseEntity.ok(filterService.createTitleWordToNegativeFilter(filterId, wordId));
	}

	@DeleteMapping("user/negative-filter/{filter_id}/title-word/{word_id}")
	public Mono<ResponseEntity<Void>> deleteTitleWordFromNegativeFilter(@PathVariable("filter_id") Long filterId,
			@PathVariable("word_id") Long wordId) {
		return filterService.removeTitleWordFromNegativeFilter(filterId, wordId);
	}

	@PatchMapping("user/negative-filter/{filter_id}/technology-word/{word_id}")
	public ResponseEntity<Mono<FilterDTO>> addTechnologyWordToNegativeFilter(@PathVariable("filter_id") Long filterId,
			@PathVariable("word_id") Long wordId) {
		return ResponseEntity.ok(filterService.createTechnologyWordToNegativeFilter(filterId, wordId));
	}

	@DeleteMapping("user/negative-filter/{filter_id}/technology-word/{word_id}")
	public Mono<ResponseEntity<Void>> deleteTechnologyWordFromNegativeFilter(@PathVariable("filter_id") Long filterId,
			@PathVariable("word_id") Long wordId) {
		return filterService.removeTechnologyWordFromNegativeFilter(filterId, wordId);
	}

	@PatchMapping("user/negative-filter/{filter_id}/description-word/{word_id}")
	public ResponseEntity<Mono<FilterDTO>> addDescriptionWordToNegativeFilter(@PathVariable("filter_id") Long filterId,
			@PathVariable("word_id") Long wordId) {
		return ResponseEntity.ok(filterService.createDescriptionWordToNegativeFilter(filterId, wordId));
	}

	@DeleteMapping("user/negative-filter/{filter_id}/description-word/{word_id}")
	public Mono<ResponseEntity<Void>> deleteDescriptionWordFromNegativeFilter(@PathVariable("filter_id") Long filterId,
			@PathVariable("word_id") Long wordId) {
		return filterService.removeDescriptionWordFromFilter(filterId, wordId);
	}
}