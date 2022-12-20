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
import by.gdev.alert.job.parser.domain.model.Order;
import by.gdev.alert.job.parser.domain.model.SiteCategoryDTO;
import by.gdev.alert.job.parser.domain.model.SiteSubCategoryDTO;
import by.gdev.alert.job.parser.service.AlertService;
import by.gdev.alert.job.parser.service.FLOrderParser;
import by.gdev.alert.job.parser.service.HabrOrderParser;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/")
@RequiredArgsConstructor
public class OrderParserController {

	public final HabrOrderParser hubr;
	public final FLOrderParser fl;
	public final AlertService service;
	
	@GetMapping(value = "/stream-sse/flru", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<List<Order>>> streamFlruEvents() {
		return Flux.interval(Duration.ofSeconds(60)).map(sequence -> ServerSentEvent.<List<Order>>builder()
				.id(String.valueOf(sequence)).event("periodic-parse-event").data(fl.flruParser()).build());
	}
	
	@GetMapping(value = "/stream-sse/hubr", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<List<Order>>> streamHubrEvents() {
		return Flux.interval(Duration.ofSeconds(60)).map(sequence -> ServerSentEvent.<List<Order>>builder()
				.id(String.valueOf(sequence)).event("periodic-parse-event").data(hubr.hubrParser()).build());
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
