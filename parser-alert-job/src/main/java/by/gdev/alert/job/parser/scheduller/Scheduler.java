package by.gdev.alert.job.parser.scheduller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
	@Value("${parser.categories.file.path}")
	private String updateFilePath;
	private final ParserCategories parserCategories;

	@Override
	@Transactional
	public void onApplicationEvent(ContextRefreshedEvent event) {
		try {
			if (parseCategories) {
				parserCategories.parse();
				if (Files.notExists(Paths.get(updateFilePath)))
					throw new RuntimeException(
							"warn can't find links for habr rss you need to create file and set parser.categories.file.path "
							+ "for parser service param, the example you can find here link src/test/resources/hubr.txt");
				else
					parserCategories.updateHubrLink(updateFilePath);
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
			log.info("removed parser link" + e.getLinks());
		});
	}
	
}
