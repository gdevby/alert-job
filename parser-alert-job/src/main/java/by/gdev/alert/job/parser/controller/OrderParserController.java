package by.gdev.alert.job.parser.controller;

import static by.gdev.alert.job.parser.util.ParserStringUtils.COUNTER_FLRU;
import static by.gdev.alert.job.parser.util.ParserStringUtils.COUNTER_FREELANCEHUNT;
import static by.gdev.alert.job.parser.util.ParserStringUtils.COUNTER_FREELANCER;
import static by.gdev.alert.job.parser.util.ParserStringUtils.COUNTER_FREELANCERU;
import static by.gdev.alert.job.parser.util.ParserStringUtils.COUNTER_HUBR;
import static by.gdev.alert.job.parser.util.ParserStringUtils.COUNTER_WEBLANCER;
import static by.gdev.alert.job.parser.util.ParserStringUtils.COUNTER_YOUDO;
import static by.gdev.alert.job.parser.util.ParserStringUtils.PROXY_CLIENT;

import java.time.Duration;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import by.gdev.alert.job.parser.service.FLOrderParser;
import by.gdev.alert.job.parser.service.FreelanceRuOrderParser;
import by.gdev.alert.job.parser.service.FreelancehuntOrderParcer;
import by.gdev.alert.job.parser.service.FreelancerOrderParser;
import by.gdev.alert.job.parser.service.HabrOrderParser;
import by.gdev.alert.job.parser.service.ParserService;
import by.gdev.alert.job.parser.service.WeblancerOrderParcer;
import by.gdev.alert.job.parser.service.YouDoOrderParcer;
import by.gdev.common.model.CategoryDTO;
import by.gdev.common.model.OrderDTO;
import by.gdev.common.model.SiteSourceDTO;
import by.gdev.common.model.SubCategoryDTO;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
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
	public final FreelanceRuOrderParser freelanceRuOrderParser;
	public final WeblancerOrderParcer weblancerOrderParcer;
	public final FreelancehuntOrderParcer freelancehuntOrderParcer;
	public final YouDoOrderParcer youDoOrderParcer;
	public final FreelancerOrderParser freelancerOrderParcer;
	public final ParserService service;

	@Value("${parser.interval}")
	private long parserInterval;

	private final ApplicationContext context;
	private final MeterRegistry meterRegistry;

	@GetMapping(value = "/stream-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<List<OrderDTO>>> streamFlruEvents() {
		log.trace("subscribed on orders");
		Flux<ServerSentEvent<List<OrderDTO>>> flruFlux = Flux
				.interval(Duration.ofSeconds(parserInterval)).map(sequence -> ServerSentEvent.<List<OrderDTO>>builder()
						.id(String.valueOf(sequence)).event("periodic-flru-parse-event").data(fl.flruParser()).build())
				.doOnNext(s -> {
					int size = s.data().size();
					context.getBean(COUNTER_FLRU, Counter.class).increment(size);
				});
		Flux<ServerSentEvent<List<OrderDTO>>> hubrFlux = Flux.interval(Duration.ofSeconds(parserInterval))
				.map(sequence -> ServerSentEvent.<List<OrderDTO>>builder().id(String.valueOf(sequence))
						.event("periodic-hubr-parse-event").data(hubr.hubrParser()).build())
				.doOnNext(s -> {
					int size = s.data().size();
					context.getBean(COUNTER_HUBR, Counter.class).increment(size);
				});

		Flux<ServerSentEvent<List<OrderDTO>>> freelanceRuFlux = Flux.interval(Duration.ofSeconds(parserInterval))
				.map(sequence -> ServerSentEvent.<List<OrderDTO>>builder().id(String.valueOf(sequence))
						.event("periodic-freelanceru-parse-event").data(freelanceRuOrderParser.getOrders()).build())
				.doOnNext(s -> {
					int size = s.data().size();
					context.getBean(COUNTER_FREELANCERU, Counter.class).increment(size);
				});
		Flux<ServerSentEvent<List<OrderDTO>>> weblancerFlux = Flux.interval(Duration.ofSeconds(parserInterval))
				.map(sequence -> ServerSentEvent.<List<OrderDTO>>builder().id(String.valueOf(sequence))
						.event("periodic-weblancer-parse-event").data(weblancerOrderParcer.weblancerParser()).build())
				.doOnNext(s -> {
					int size = s.data().size();
					context.getBean(COUNTER_WEBLANCER, Counter.class).increment(size);
				});
		Flux<ServerSentEvent<List<OrderDTO>>> freelancehuntOrderParcerFlux = Flux
				.interval(Duration.ofSeconds(parserInterval))
				.map(sequence -> ServerSentEvent.<List<OrderDTO>>builder().id(String.valueOf(sequence))
						.event("periodic-freelancehunt-parse-event")
						.data(freelancehuntOrderParcer.freelancehuntParser()).build())
				.doOnNext(s -> {
					int size = s.data().size();
					context.getBean(COUNTER_FREELANCEHUNT, Counter.class).increment(size);
				});

		Flux<ServerSentEvent<List<OrderDTO>>> youDoOrderParcerFlux = Flux.interval(Duration.ofSeconds(parserInterval))
				.map(sequence -> ServerSentEvent.<List<OrderDTO>>builder().id(String.valueOf(sequence))
						.event("periodic-youdo-parse-event").data(youDoOrderParcer.youDoParser()).build())
				.doOnNext(s -> {
					int size = s.data().size();
					context.getBean(COUNTER_YOUDO, Counter.class).increment(size);
				});

		Flux<ServerSentEvent<List<OrderDTO>>> freelancerOrderParcerFlux = Flux
				.interval(Duration.ofSeconds(parserInterval))
				.map(sequence -> ServerSentEvent.<List<OrderDTO>>builder().id(String.valueOf(sequence))
						.event("periodic-freelancer-parse-event").data(freelancerOrderParcer.freelancerParser())
						.build())
				.doOnNext(s -> {
					int size = s.data().size();
					context.getBean(COUNTER_FREELANCER, Counter.class).increment(size);
				});

		return Flux.merge(flruFlux, hubrFlux, freelanceRuFlux, weblancerFlux, freelancehuntOrderParcerFlux,
				youDoOrderParcerFlux, freelancerOrderParcerFlux);
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
	public Flux<OrderDTO> showOrdersBySource(@RequestParam("site_id") Long site,
			@RequestParam("category_id") Long category, @RequestParam(name = "sub_id", required = false) Long subId,
			@RequestParam("period") Long period) {
		return service.getOrdersBySource(site, category, subId, period);
	}

	@PostConstruct
	void init() {
		ConfigurableListableBeanFactory beanFactory = ((ConfigurableApplicationContext) context).getBeanFactory();
		beanFactory.registerSingleton(COUNTER_HUBR, meterRegistry.counter(PROXY_CLIENT, PROXY_CLIENT, "hubr"));
		beanFactory.registerSingleton(COUNTER_FLRU, meterRegistry.counter(PROXY_CLIENT, PROXY_CLIENT, "flru"));
		beanFactory.registerSingleton(COUNTER_FREELANCERU,
				meterRegistry.counter(PROXY_CLIENT, PROXY_CLIENT, "freelanceru"));
		beanFactory.registerSingleton(COUNTER_WEBLANCER,
				meterRegistry.counter(PROXY_CLIENT, PROXY_CLIENT, "weblancer"));
		beanFactory.registerSingleton(COUNTER_FREELANCEHUNT,
				meterRegistry.counter(PROXY_CLIENT, PROXY_CLIENT, "freelancehun"));
		beanFactory.registerSingleton(COUNTER_YOUDO, meterRegistry.counter(PROXY_CLIENT, PROXY_CLIENT, "youdo"));
		beanFactory.registerSingleton(COUNTER_FREELANCER,
				meterRegistry.counter(PROXY_CLIENT, PROXY_CLIENT, "freelancer"));
	}
}