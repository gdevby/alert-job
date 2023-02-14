package by.gdev.alert.job.parser.controller;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import by.gdev.alert.job.parser.service.FLOrderParser;
import by.gdev.alert.job.parser.service.HabrOrderParser;
import by.gdev.alert.job.parser.service.ParserService;
import by.gdev.common.model.CategoryDTO;
import by.gdev.common.model.OrderDTO;
import by.gdev.common.model.SiteSourceDTO;
import by.gdev.common.model.SubCategoryDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/")
@RequiredArgsConstructor
@Slf4j
public class OrderParserController {

	public final HabrOrderParser hubr;
	public final FLOrderParser fl;
	public final ParserService service;

	@Value("${parser.interval}")
	private long parserInterval;

	@GetMapping(value = "/stream-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<List<OrderDTO>>> streamFlruEvents() {
		log.trace("subscribed on orders");
		Flux<ServerSentEvent<List<OrderDTO>>> flruFlux = Flux.interval(Duration.ofSeconds(parserInterval))
				.map(sequence -> ServerSentEvent.<List<OrderDTO>>builder().id(String.valueOf(sequence))
						.event("periodic-flru-parse-event").data(fl.flruParser()).build());
		Flux<ServerSentEvent<List<OrderDTO>>> hubrFlux = Flux.interval(Duration.ofSeconds(parserInterval))
				.map(sequence -> ServerSentEvent.<List<OrderDTO>>builder().id(String.valueOf(sequence))
						.event("periodic-hubr-parse-event").data(hubr.hubrParser()).build());
		return Flux.merge(flruFlux, hubrFlux);
	}

	@GetMapping("sites")
	public Flux<SiteSourceDTO> sites() {
		return service.getSites();
	}

	@GetMapping("categories")
	public Flux<CategoryDTO> categories(@RequestParam("site_id") Long site) {
		return service.getCategories(site);
	}

	@GetMapping("subcategories")
	public Flux<SubCategoryDTO> subCategories(@RequestParam("category_id") Long category) {
		return service.getSubCategories(category);
	}

	@GetMapping("site/{id}")
	public Mono<SiteSourceDTO> site(@PathVariable("id") Long id) {
		return service.getSite(id);
	}

	@GetMapping("site/{id}/category/{category_id}")
	public Mono<CategoryDTO> category(@PathVariable("id") Long id, @PathVariable("category_id") Long cId) {
		return service.getCategory(id, cId);
	}

	@GetMapping("category/{id}/subcategory/{sub_id}")
	public Mono<SubCategoryDTO> subCategory(@PathVariable("id") Long id, @PathVariable("sub_id") Long subId) {
		return service.getSubCategory(id, subId);
	}

	@PatchMapping("subscribe/sources")
	public Mono<Void> subscribeSources(@RequestParam("category_id") Long categoryId,
			@RequestParam(name = "subcategory_id", required = false) Long subCategoryId,
			@RequestParam("category_value") boolean cValue,
			@RequestParam(name = "subcategory_value", required = false) boolean sValue) {
		return service.subcribeOnSource(categoryId, subCategoryId, cValue, sValue);
	}
	
	@GetMapping("orders")
	public Flux<OrderDTO> showOrdersBySource(@RequestParam("site_id") Long site, @RequestParam("category_id") Long category, @RequestParam(name = "sub_id", required = false) Long subId){
		return service.getOrdersBySource(site, category, subId);
	}
}
