package by.gdev.alert.job.parser.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.domain.db.Subcategory;
import by.gdev.alert.job.parser.repository.SiteSourceJobRepository;
import by.gdev.common.model.OrderDTO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbsctractSiteParser {
	@Autowired
	private SiteSourceJobRepository siteSourceJobRepository;

	public List<OrderDTO> getOrders(Long siteId) {
		List<OrderDTO> orders = new ArrayList<>();
		SiteSourceJob siteSourceJob = siteSourceJobRepository.findById(siteId).get();
		log.trace("parsed {}", siteSourceJob.getName());
		siteSourceJob.getCategories().stream()
				// parse only categories that can parse=true
				// iterate over each category from this collection
				.forEach(category -> {
					log.trace("getting order by category {} rss link {}", category.getNativeLocName(),
							category.getLink());
					List<Subcategory> siteSubCategories = category.getSubCategories();
					// checking if a subcategory exists for this category
					// category does't have a subcategory
					if (category.isParse())
						orders.addAll(mapItems(category.getLink(), siteSourceJob.getId(), category, null));
					// category have a subcategory
					siteSubCategories.stream()
							// parse only sub categories that can parse=true
							.filter(subCategoryFilter -> subCategoryFilter.isParse())
							// Iterate all sub category
							.forEach(subCategory -> {
								log.trace("getting order by category {} and subcategory  {} {}",
										category.getNativeLocName(), subCategory.getNativeLocName(),
										subCategory.getLink());
								List<OrderDTO> list1 = mapItems(subCategory.getLink(), siteSourceJob.getId(), category,
										subCategory);
								orders.addAll(list1);
							});
				});
		return orders;
	}

	abstract List<OrderDTO> mapItems(String link, Long siteSourceJobId, Category c, Subcategory sub);
}
