package by.gdev.alert.job.core.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import by.gdev.alert.job.core.model.key.DescriptionWord;

public interface DescriptionWordRepository extends CrudRepository<DescriptionWord, Long>{
	
	Optional<DescriptionWord> findByName(String name);
}
