package by.gdev.alert.job.parser.scheduller;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import by.gdev.alert.job.parser.repository.OrderLinksRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class Scheduler implements ApplicationListener<ContextRefreshedEvent>{
	
	private final OrderLinksRepository linkRepository;
	
	@Value("${parser.update.links.after.days}")
	private long parserUpdateLinksAfterDay;
	
	
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
	}
	
	
	@Scheduled(cron = "0 0 6 * * *")
	public void removeEnaibleTokens() {
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
