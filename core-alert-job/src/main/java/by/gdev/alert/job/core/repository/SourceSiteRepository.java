package by.gdev.alert.job.core.repository;

import org.springframework.data.repository.CrudRepository;

import by.gdev.alert.job.core.model.db.SourceSite;

public interface SourceSiteRepository extends CrudRepository<SourceSite, Long>{
	
	boolean existsBySiteCategory(Long siteCategory);
	
	boolean existsBySiteCategoryAndSiteSubCategory(Long siteCategory, Long siteSubCategory);
}
