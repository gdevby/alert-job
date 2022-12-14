package by.gdev.alert.job.parser.service;

import org.springframework.stereotype.Component;

import by.gdev.alert.job.parser.domain.Category;
import by.gdev.alert.job.parser.domain.OrderLinks;
import by.gdev.alert.job.parser.domain.SubCategory;
import by.gdev.alert.job.parser.repository.OrderLinksRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ParserUtil {
	
	private final OrderLinksRepository linkRepository;
	
	public boolean orderFilter(Category category, SubCategory subCategory, String link){
			if (linkRepository.existsByCategoryAndSubCategoryAndLinks(category, subCategory, link)) {
				return false;
			}
			else {
				OrderLinks ol = new OrderLinks();
				ol.setCategory(category);
				ol.setSubCategory(subCategory);
				ol.setLinks(link);
				linkRepository.save(ol);
				return true;
			}
	}
}