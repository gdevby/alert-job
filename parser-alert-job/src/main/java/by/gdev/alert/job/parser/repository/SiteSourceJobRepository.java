package by.gdev.alert.job.parser.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import by.gdev.alert.job.parser.domain.db.SiteSourceJob;

public interface SiteSourceJobRepository extends CrudRepository<SiteSourceJob, Long> {

	SiteSourceJob findByName(String name);

	List<SiteSourceJob> findAllByActiveTrue();

    @Query("""
    SELECT ssj FROM SiteSourceJob ssj
    LEFT JOIN FETCH ssj.categories c
    LEFT JOIN FETCH c.subCategories
    WHERE ssj.name = :name
""")
    SiteSourceJob findWithCategories(String name);


}