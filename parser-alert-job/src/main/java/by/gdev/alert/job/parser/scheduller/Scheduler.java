package by.gdev.alert.job.parser.scheduller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;

import by.gdev.alert.job.parser.repository.OrderLinksRepository;
import by.gdev.alert.job.parser.repository.OrderRepository;
import by.gdev.alert.job.parser.service.ParserCategories;
import io.micrometer.core.instrument.util.IOUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class Scheduler implements ApplicationListener<ContextRefreshedEvent> {

	private final OrderLinksRepository linkRepository;
	@Value("${parser.update.links.after.days}")
	private long parserUpdateLinksAfterDay;
	@Value("${parser.insert.categories.active}")
	boolean parseCategories;
	@Value("${parser.categories.file.path}")
	private String updateFilePath;
	private final ParserCategories parserCategories;
	private final OrderRepository orderRepository;
	
	private final ApplicationContext context;
	

	@Override
	@Transactional
	public void onApplicationEvent(ContextRefreshedEvent event) {
		Resource res = context.getResource(updateFilePath);
		try {
			if (parseCategories) {
				parserCategories.parse();
				if (updateFilePath.startsWith("classpath")) {
					log.info(
							"Used one rss link for every categories and subcategory of the habr orders for dev environment."
									+ " For prod or dev needs you can changed parser.categories.file.path with proper rss for parser service."
									+ " The example you can find here src/main/resources/hubr.txt");
					parserCategories.updateHubrLink(Lists.newArrayList(IOUtils.toString(res.getInputStream()).split(System.lineSeparator())));
				}
			}
		} catch (IOException e) {
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
}