package by.gdev.alert.job.parser.repository;

import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.SiteCategory;

public interface SiteCategoryRepository extends CrudRepository<SiteCategory, Long>{
	
	Set<SiteCategory> findByCategory(Category category);
	
	@Query("select c from SiteCategory c left join fetch c.siteSubCategories s where c.id= :id and s.id = :sId")
	SiteCategory test(@Param("id") Long id, @Param("sId") Long sId);
	

}
