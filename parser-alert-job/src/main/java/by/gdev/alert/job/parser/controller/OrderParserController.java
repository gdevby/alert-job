package by.gdev.alert.job.parser.controller;

import java.time.Duration;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import by.gdev.alert.job.parser.domain.model.EnumSite;
import by.gdev.alert.job.parser.domain.model.SiteCategoryDTO;
import by.gdev.alert.job.parser.domain.model.SiteSubCategoryDTO;
import by.gdev.alert.job.parser.service.ParserService;
import by.gdev.alert.job.parser.service.FLOrderParser;
import by.gdev.alert.job.parser.service.HabrOrderParser;
import by.gdev.common.model.Order;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/")
@RequiredArgsConstructor
public class OrderParserController {

	public final HabrOrderParser hubr;
	public final FLOrderParser fl;
	public final ParserService service;
	
	@GetMapping(value = "/stream-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<List<Order>>> streamFlruEvents() {
		Flux<ServerSentEvent<List<Order>>> flruFlux = Flux.interval(Duration.ofSeconds(60)).map(sequence -> ServerSentEvent.<List<Order>>builder()
				.id(String.valueOf(sequence))
				.event("periodic-flru-parse-event").data(fl.flruParser())
				.build());
		Flux<ServerSentEvent<List<Order>>> hubrFlux = Flux.interval(Duration.ofSeconds(60)).map(sequence -> ServerSentEvent.<List<Order>>builder()
				.id(String.valueOf(sequence))
				.event("periodic-hubr-parse-event").data(hubr.hubrParser())
				.build());
		return Flux.merge(flruFlux, hubrFlux);
	}
	
	@GetMapping("sites")
	public List<EnumSite> sites(){
		return service.getSites();
	}
	
	@GetMapping("categories")
	public List<SiteCategoryDTO> categories(@RequestParam("site_id") Long site){
		return service.getCategories(site);
	}
	
	@GetMapping("subcategories")
	public List<SiteSubCategoryDTO> subCategories(@RequestParam("category_id") Long category){
		return service.getSubCategories(category);
	}
}
