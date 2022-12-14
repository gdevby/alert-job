package by.gdev.alert.job.parser.repository;

import org.springframework.data.repository.CrudRepository;

import by.gdev.alert.job.parser.domain.Category;
import by.gdev.alert.job.parser.domain.OrderLinks;
import by.gdev.alert.job.parser.domain.SubCategory;

public interface OrderLinksRepository extends CrudRepository<OrderLinks, Long> {
	
	boolean existsByCategoryAndSubCategoryAndLinks(Category category, SubCategory subCategory, String links);
}
