package by.gdev.alert.job.parser.scheduller;

import by.gdev.alert.job.parser.domain.currency.CurrencyRoot;
import by.gdev.alert.job.parser.domain.currency.Valute;
import by.gdev.alert.job.parser.domain.db.CurrencyEntity;
import by.gdev.alert.job.parser.repository.CurrencyRepository;
import by.gdev.alert.job.parser.repository.OrderLinksRepository;
import by.gdev.alert.job.parser.repository.OrderRepository;
import by.gdev.alert.job.parser.service.category.ParserCategories;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
@Slf4j
public class Scheduler implements ApplicationListener<ContextRefreshedEvent> {

	private final OrderLinksRepository linkRepository;
	@Value("${parser.update.links.after.days}")
	private long parserUpdateLinksAfterDay;
	@Value("${parser.insert.categories.active}")
	boolean parseCategories;
	@Value("${currency.site}")
	private String currencySiteUrl;

	private final ParserCategories parserCategories;
	private final OrderRepository orderRepository;
	private final CurrencyRepository currencyRepository;

	private final ApplicationContext context;
	private final RestTemplate restTemplate;

	@Override
	@Transactional
	public void onApplicationEvent(ContextRefreshedEvent event) {
		try {
			currencyParser();
			if (parseCategories) {
				parserCategories.parse();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Scheduled(cron = "0 0 1 * * *")
	public void removeParsedLinks() {
		Lists.newArrayList(linkRepository.findAll()).stream().filter(f -> {
			LocalDateTime ldt = f.getCreatedDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			LocalDateTime plusLdt = ldt.plusDays(parserUpdateLinksAfterDay);
			LocalDateTime now = LocalDateTime.now();
			return now.isAfter(plusLdt);
		}).forEach(e -> {
			linkRepository.delete(e);
			log.debug("removed parser link" + e.getLinks());
		});
	}

	@Scheduled(cron = "0 0 1 * * *")
	public void removeOrders() {
		Lists.newArrayList(orderRepository.findAll()).stream().filter(f -> {
			LocalDateTime ldt = f.getDateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			LocalDateTime plusLdt = ldt.plusDays(parserUpdateLinksAfterDay);
			LocalDateTime now = LocalDateTime.now();
			return now.isAfter(plusLdt);
		}).forEach(e -> {
			orderRepository.delete(e);
			log.debug("removed order with title" + e.getTitle());
		});
	}

	@Scheduled(cron = "0 0 12 * * *")
	@SneakyThrows
	public void currencyParser() {
		String request = restTemplate.getForObject(currencySiteUrl, String.class);
		ObjectMapper mapper = new ObjectMapper();
		CurrencyRoot currency = mapper.readValue(request, CurrencyRoot.class);
		currency.getValute().entrySet().forEach(e -> {
			Valute valute = e.getValue();
			CurrencyEntity entity = currencyRepository
					.findByCurrencyCode(valute.getCharCode()).orElseGet(() -> {
						CurrencyEntity ent = new CurrencyEntity();
						ent.setNominal(valute.getNominal());
						ent.setCurrencyCode(valute.getCharCode());
						ent.setCurrencyName(valute.getName());
						return ent;
					});
			entity.setCurrencyValue(valute.getValue());
			currencyRepository.save(entity);
		});

	}
}