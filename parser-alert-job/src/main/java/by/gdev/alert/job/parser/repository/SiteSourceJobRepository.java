package by.gdev.alert.job.parser.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import by.gdev.alert.job.parser.domain.db.SiteSourceJob;

public interface SiteSourceJobRepository extends CrudRepository<SiteSourceJob, Long> {
	
	SiteSourceJob findByName(String name);
	
	@Query("select s from SiteSourceJob s left join fetch s.siteCategories c where s.id = :id and c.id = :cId")
	SiteSourceJob test(@Param("id") Long id, @Param("cId") Long cId);
	
}
