package by.gdev.alert.job.parser.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import by.gdev.alert.job.parser.domain.db.Category;

public interface CategoryRepository extends CrudRepository<Category, Long> {
	
	Optional<Category> findByName(String name);
}
