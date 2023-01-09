package by.gdev.alert.job.core.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import by.gdev.alert.job.core.model.db.key.TitleWord;

public interface TitleWordRepository extends CrudRepository<TitleWord, Long>{
	
	Optional<TitleWord> findByName(String name);
	
	Page<TitleWord> findAll(Pageable p);
	
	Page<TitleWord> findByNameIsStartingWith(String name, Pageable p);
}
