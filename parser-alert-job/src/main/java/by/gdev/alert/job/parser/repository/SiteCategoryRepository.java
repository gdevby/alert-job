package by.gdev.alert.job.parser.repository;

import java.util.Set;

import org.springframework.data.repository.CrudRepository;

import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.SiteCategory;

public interface SiteCategoryRepository extends CrudRepository<SiteCategory, Long>{
	
	Set<SiteCategory> findByCategory(Category category);

}
