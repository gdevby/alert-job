package by.gdev.alert.job.core.repository;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import by.gdev.alert.job.core.model.db.key.TechnologyWord;

public interface TechnologyWordRepository extends CrudRepository<TechnologyWord, Long> {
	
	Optional<TechnologyWord> findByName(String name);
	
	@Query("select t from TechnologyWord t where t.sourceSite.id IN :sourceSite or t.uuid =:uuid order by t.counter desc")
	Page<TechnologyWord> findByNameAndSourceSiteInOrUuid(String uuid, Set<Long> sourceSite, Pageable p);
	
	@Query("select t from TechnologyWord t where (t.name = :name and t.sourceSite.id IN :sourceSite) or (t.name = :name and t.uuid =:uuid) order by t.counter desc")
	Page<TechnologyWord> findByNameAndSourceSiteInOrNameAndUuid(String name, String uuid, Set<Long> sourceSite, Pageable p);
}
