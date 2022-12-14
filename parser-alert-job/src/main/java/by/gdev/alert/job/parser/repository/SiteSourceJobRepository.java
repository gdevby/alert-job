package by.gdev.alert.job.parser.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import by.gdev.alert.job.parser.domain.SiteSourceJob;

public interface SiteSourceJobRepository extends CrudRepository<SiteSourceJob, Long> {
	
	
	List<SiteSourceJob> findAllByName(String name);
	
	
	
	
}
