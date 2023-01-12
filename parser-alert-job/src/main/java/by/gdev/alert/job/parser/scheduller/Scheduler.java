package by.gdev.alert.job.parser.scheduller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import by.gdev.alert.job.parser.repository.OrderLinksRepository;
import by.gdev.alert.job.parser.service.ParserCategories;
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
	private final ParserCategories parserCategories;

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		try {
			if (parseCategories)
				parserCategories.parse();
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
			log.info("removed parser link" + e.getLinks());
		});
	}

}
