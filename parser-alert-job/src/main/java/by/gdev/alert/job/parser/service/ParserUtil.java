package by.gdev.alert.job.parser.service;

import java.util.function.Predicate;

import org.springframework.stereotype.Component;

import by.gdev.alert.job.parser.domain.OrderLinks;
import by.gdev.alert.job.parser.model.Item;
import by.gdev.alert.job.parser.repository.OrderLinksRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ParserUtil {
	
	private final OrderLinksRepository linkRepository;
	
	public Predicate<Item> orderFilter(){
		return item -> {
			if (linkRepository.existsByLinks(item.getLink())) {
				return false;
			}
			else {
				OrderLinks ol = new OrderLinks();
				ol.setLinks(item.getLink());
				linkRepository.save(ol);
				return true;
			}
		};
	}
}