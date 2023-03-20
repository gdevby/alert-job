package by.gdev.alert.job.core.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import by.gdev.alert.job.core.model.db.SourceSite;

public interface SourceSiteRepository extends CrudRepository<SourceSite, Long> {

	boolean existsBySiteCategory(Long siteCategory);

	boolean existsBySiteCategoryAndSiteSubCategory(Long siteCategory, Long siteSubCategory);

	@Query("select s from SourceSite s where s.siteSource = :siteSource and s.siteCategory = :siteCategory and (:siteSubCategory is null or s.siteSubCategory = :siteSubCategory)")
	Optional<SourceSite> findBySource(Long siteSource, Long siteCategory, Long siteSubCategory);
}
