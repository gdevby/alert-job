package by.gdev.alert.job.core.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import by.gdev.alert.job.core.model.db.key.TechnologyWord;

public interface TechnologyWordRepository extends CrudRepository<TechnologyWord, Long> {
	
	Optional<TechnologyWord> findByName(String name);
}
