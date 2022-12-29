package by.gdev.alert.job.core.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import by.gdev.alert.job.core.model.key.TitleWord;

public interface TitleWordRepository extends CrudRepository<TitleWord, Long>{
	
	Optional<TitleWord> findByName(String name);
}
