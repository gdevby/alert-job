package by.gdev.alert.job.parser.controller;

import by.gdev.alert.job.parser.service.FLOrderParser;
import by.gdev.alert.job.parser.service.FreelanceRuOrderParser;
import by.gdev.alert.job.parser.service.FreelancehuntOrderParcer;
import by.gdev.alert.job.parser.service.FreelancerOrderParser;
import by.gdev.alert.job.parser.service.HabrOrderParser;
import by.gdev.alert.job.parser.service.KworkOrderParcer;
import by.gdev.alert.job.parser.service.StatisticsService;
import by.gdev.alert.job.parser.service.ParserService;
import by.gdev.alert.job.parser.service.TruelancerOrderParser;
import by.gdev.alert.job.parser.service.WeblancerOrderParcer;
import by.gdev.alert.job.parser.service.YouDoOrderParcer;
import by.gdev.alert.job.parser.util.SiteName;
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

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static by.gdev.alert.job.parser.util.ParserStringUtils.COUNTER_FLRU;
import static by.gdev.alert.job.parser.util.ParserStringUtils.COUNTER_FREELANCEHUNT;
import static by.gdev.alert.job.parser.util.ParserStringUtils.COUNTER_FREELANCER;
import static by.gdev.alert.job.parser.util.ParserStringUtils.COUNTER_FREELANCERU;
import static by.gdev.alert.job.parser.util.ParserStringUtils.COUNTER_HUBR;
import static by.gdev.alert.job.parser.util.ParserStringUtils.COUNTER_KWORK;
import static by.gdev.alert.job.parser.util.ParserStringUtils.COUNTER_TRUELANCER;
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
	public final TruelancerOrderParser truelancerOrderParser;
	public final ParserService parserService;
	private final StatisticsService statisticsService;

	private final ApplicationContext context;
	private ExecutorService executor = Executors.newCachedThreadPool();

	@GetMapping("/stream-orders")
	public List<OrderDTO> ordersEvents() {
		log.trace("subscribed on orders");

		List<Future<List<OrderDTO>>> futures = List.of(
				executor.submit(() -> {
					List<OrderDTO> list = fl.flruParser();
					int size = list.size();
					context.getBean(COUNTER_FLRU, Counter.class).increment(size);
					statisticsService.save(SiteName.FLRU, size);
					return list;
				}),
				executor.submit(() -> {
					List<OrderDTO> list = hubr.hubrParser();
					int size = list.size();
					context.getBean(COUNTER_HUBR, Counter.class).increment(size);
					statisticsService.save(SiteName.HABR, size);
					return list;
				}),
				executor.submit(() -> {
					List<OrderDTO> list = freelanceRuOrderParser.getOrders();
					int size = list.size();
					context.getBean(COUNTER_FREELANCERU, Counter.class).increment(size);
					statisticsService.save(SiteName.FREELANCERU, size);
					return list;
				}),
				executor.submit(() -> {
					List<OrderDTO> list = weblancerOrderParcer.weblancerParser();
					int size = list.size();
					context.getBean(COUNTER_WEBLANCER, Counter.class).increment(size);
					statisticsService.save(SiteName.WEBLANCER, size);
					return list;
				}),
				executor.submit(() -> {
					List<OrderDTO> list = freelancehuntOrderParcer.freelancehuntParser();
					int size = list.size();
					context.getBean(COUNTER_FREELANCEHUNT, Counter.class).increment(size);
					statisticsService.save(SiteName.FREELANCEHUNT, size);
					return list;
				}),
				executor.submit(() -> {
					List<OrderDTO> list = youDoOrderParcer.youDoParser();
					int size = list.size();
					context.getBean(COUNTER_YOUDO, Counter.class).increment(size);
					statisticsService.save(SiteName.YOUDO, size);
					return list;
				}),
				executor.submit(() -> {
					List<OrderDTO> list = kworkOrderParcer.kworkParser();
					int size = list.size();
					context.getBean(COUNTER_KWORK, Counter.class).increment(size);
					statisticsService.save(SiteName.KWORK, size);
					return list;
				}),
				executor.submit(() -> {
					List<OrderDTO> list = freelancerOrderParcer.freelancerParser();
					int size = list.size();
					context.getBean(COUNTER_FREELANCER, Counter.class).increment(size);
					statisticsService.save(SiteName.FREELANCER, size);
					return list;
				}),

				executor.submit(() -> {
					List<OrderDTO> list = truelancerOrderParser.truelancerParser();
					context.getBean(COUNTER_TRUELANCER, Counter.class).increment(list.size());
					return list;
				})
		);
		return futures.stream()
				.map(future -> {
					try {
						return future.get();
					} catch (InterruptedException | ExecutionException e) {
						log.error(e.getMessage());
						return List.<OrderDTO>of();
					}
				})
				.flatMap(List::stream)
				.collect(Collectors.toList());
	}

	@GetMapping("sites")
	public List<SiteSourceDTO>  sites() {
		return parserService.getSites();
	}

	@GetMapping("categories")
	public List<CategoryDTO> categories(@RequestParam("site_id") Long site) {
		return parserService.getCategories(site);
	}

	@GetMapping("subcategories")
	public List<SubCategoryDTO> subCategories(@RequestParam("category_id") Long category) {
		return parserService.getSubCategories(category);
	}

	@GetMapping("site/{id}")
	public SiteSourceDTO  site(@PathVariable("id") Long id) {
		return parserService.getSite(id);
	}

	@GetMapping("site/{id}/category/{category_id}")
	public CategoryDTO category(@PathVariable("id") Long id, @PathVariable("category_id") Long cId) {
		return parserService.getCategory(id, cId);
	}

	@GetMapping("category/{id}/subcategory/{sub_id}")
	public SubCategoryDTO subCategory(@PathVariable("id") Long id, @PathVariable("sub_id") Long subId) {
		return parserService.getSubCategory(id, subId);
	}

	@PatchMapping("subscribe/sources")
	public void subscribeSources(@RequestParam("category_id") Long categoryId,
			@RequestParam(name = "subcategory_id", required = false) Long subCategoryId,
			@RequestParam("category_value") boolean cValue,
			@RequestParam(name = "subcategory_value", required = false) boolean sValue) {
		parserService.subcribeOnSource(categoryId, subCategoryId, cValue, sValue);
	}

	@GetMapping("orders")
	public List<OrderDTO> showOrdersBySource(@RequestParam("site_id") Long site,
			@RequestParam("category_id") Long category, @RequestParam(name = "sub_id", required = false) Long subId,
			@RequestParam("period") Long period) {
		return parserService.getOrdersBySource(site, category, subId, period);
	}
}