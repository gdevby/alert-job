package by.gdev.alert.job.core.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import by.gdev.alert.job.core.model.db.SourceSite;

public interface SourceSiteRepository extends CrudRepository<SourceSite, Long> {

    boolean existsBySiteCategoryAndActive(Long siteCategory, boolean active);

    boolean existsBySiteCategoryAndSiteSubCategoryAndActive(Long siteCategory, Long siteSubCategory, boolean active);

    @Query("select s from SourceSite s where s.siteSource = :siteSource and s.siteCategory = :siteCategory and s.siteSubCategory = :siteSubCategory")
    Optional<SourceSite> findBySource(Long siteSource, Long siteCategory, Long siteSubCategory);

    @Query("select s from SourceSite s where s.siteSource = :siteSource and s.siteCategory = :siteCategory and s.siteSubCategory is null")
    Optional<SourceSite> findBySourceSubCategoryIsNull(Long siteSource, Long siteCategory);
}
