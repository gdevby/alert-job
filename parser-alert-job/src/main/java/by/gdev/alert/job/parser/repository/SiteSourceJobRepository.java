package by.gdev.alert.job.parser.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import by.gdev.alert.job.parser.domain.db.SiteSourceJob;

public interface SiteSourceJobRepository extends CrudRepository<SiteSourceJob, Long> {

	SiteSourceJob findByName(String name);

	List<SiteSourceJob> findAllByActiveTrue();

}