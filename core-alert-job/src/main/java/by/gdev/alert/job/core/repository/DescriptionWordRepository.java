package by.gdev.alert.job.core.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import by.gdev.alert.job.core.model.db.key.DescriptionWord;

public interface DescriptionWordRepository extends CrudRepository<DescriptionWord, Long>{
	
	Optional<DescriptionWord> findByName(String name);
	
	Page<DescriptionWord> findAll(Pageable p);
	
	Page<DescriptionWord> findByNameIsStartingWith(String name, Pageable p);
}
