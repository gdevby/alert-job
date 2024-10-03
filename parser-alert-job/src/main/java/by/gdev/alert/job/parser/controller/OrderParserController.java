package by.gdev.alert.job.parser.controller;

import by.gdev.alert.job.parser.service.FLOrderParser;
import by.gdev.alert.job.parser.service.FreelanceRuOrderParser;
import by.gdev.alert.job.parser.service.FreelancehuntOrderParcer;
import by.gdev.alert.job.parser.service.FreelancerOrderParser;
import by.gdev.alert.job.parser.service.HabrOrderParser;
import by.gdev.alert.job.parser.service.KworkOrderParcer;
import by.gdev.alert.job.parser.service.ParserService;
import by.gdev.alert.job.parser.service.WeblancerOrderParcer;
import by.gdev.alert.job.parser.service.YouDoOrderParcer;
import by.gdev.common.model.CategoryDTO;
import by.gdev.common.model.OrderDTO;
import by.gdev.common.model.SiteSourceDTO;
import by.gdev.common.model.SubCategoryDTO;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static by.gdev.alert.job.parser.util.ParserStringUtils.COUNTER_FLRU;
import static by.gdev.alert.job.parser.util.ParserStringUtils.COUNTER_FREELANCEHUNT;
import static by.gdev.alert.job.parser.util.ParserStringUtils.COUNTER_FREELANCER;
import static by.gdev.alert.job.parser.util.ParserStringUtils.COUNTER_FREELANCERU;
import static by.gdev.alert.job.parser.util.ParserStringUtils.COUNTER_HUBR;
import static by.gdev.alert.job.parser.util.ParserStringUtils.COUNTER_KWORK;
import static by.gdev.alert.job.parser.util.ParserStringUtils.COUNTER_WEBLANCER;
import static by.gdev.alert.job.parser.util.ParserStringUtils.COUNTER_YOUDO;

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
	public final KworkOrderParcer kworkOrderParcer;
	public final ParserService service;

	private final ApplicationContext context;

	@GetMapping("/stream-orders")
	public Flux<List<OrderDTO>> flruEvents() {
		log.trace("subscribed on orders");
		Flux<List<OrderDTO>> flruFlux = Flux.just(fl.flruParser()).doOnNext(s -> {
			int size = s.size();
			context.getBean(COUNTER_FLRU, Counter.class).increment(size);
		});

		Flux<List<OrderDTO>> hubrFlux = Flux.just(hubr.hubrParser()).doOnNext(s -> {
			int size = s.size();
			context.getBean(COUNTER_HUBR, Counter.class).increment(size);
		});

		Flux<List<OrderDTO>> freelanceRuFlux = Flux.just(freelanceRuOrderParser.getOrders()).doOnNext(s -> {
			int size = s.size();
			context.getBean(COUNTER_FREELANCERU, Counter.class).increment(size);
		});

		Flux<List<OrderDTO>> weblancerFlux = Flux.just(weblancerOrderParcer.weblancerParser()).doOnNext(s -> {
			int size = s.size();
			context.getBean(COUNTER_WEBLANCER, Counter.class).increment(size);
		});
		Flux<List<OrderDTO>> freelancehuntOrderParcerFlux = Flux.just(freelancehuntOrderParcer.freelancehuntParser())
				.doOnNext(s -> {
					int size = s.size();
					context.getBean(COUNTER_FREELANCEHUNT, Counter.class).increment(size);
				});

		Flux<List<OrderDTO>> youDoOrderParcerFlux = Flux.just(youDoOrderParcer.youDoParser()).doOnNext(s -> {
			int size = s.size();
			context.getBean(COUNTER_YOUDO, Counter.class).increment(size);
		});

		Flux<List<OrderDTO>> kworkOrderParcerFlux = Flux.just(kworkOrderParcer.kworkParser()).doOnNext(s -> {
			int size = s.size();
			context.getBean(COUNTER_KWORK, Counter.class).increment(size);
		});

		Flux<List<OrderDTO>> freelancerOrderParcerFlux = Flux.just(freelancerOrderParcer.freelancerParser())
				.doOnNext(s -> {
					int size = s.size();
					context.getBean(COUNTER_FREELANCER, Counter.class).increment(size);
				});
		return Flux.merge(flruFlux, hubrFlux, freelanceRuFlux, weblancerFlux, freelancehuntOrderParcerFlux,
				youDoOrderParcerFlux, kworkOrderParcerFlux, freelancerOrderParcerFlux);
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
}