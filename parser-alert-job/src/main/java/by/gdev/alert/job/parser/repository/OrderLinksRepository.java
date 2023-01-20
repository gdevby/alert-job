package by.gdev.alert.job.parser.repository;

import org.springframework.data.repository.CrudRepository;

import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.OrderLinks;
import by.gdev.alert.job.parser.domain.db.Subcategory;

public interface OrderLinksRepository extends CrudRepository<OrderLinks, Long> {
	
	boolean existsByCategoryAndSubCategoryAndLinks(Category category, Subcategory subCategory, String links);
}
