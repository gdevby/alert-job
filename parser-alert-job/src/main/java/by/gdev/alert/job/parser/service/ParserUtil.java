package by.gdev.alert.job.parser.service;

import org.springframework.stereotype.Component;

import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.OrderLinks;
import by.gdev.alert.job.parser.domain.db.SubCategory;
import by.gdev.alert.job.parser.repository.OrderLinksRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ParserUtil {

	private final OrderLinksRepository linkRepository;

	public boolean isExistsOrder(Category category, SubCategory subCategory, String link) {
		return !linkRepository.existsByCategoryAndSubCategoryAndLinks(category, subCategory, link);
	}
	
	public void saveOrderLinks(Category category, SubCategory subCategory, String link) {
		OrderLinks ol = new OrderLinks();
		ol.setCategory(category);
		ol.setSubCategory(subCategory);
		ol.setLinks(link);
		linkRepository.save(ol);
	}
}