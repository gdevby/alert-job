package by.gdev.alert.job.parser.repository;

import org.springframework.data.repository.CrudRepository;

import by.gdev.alert.job.parser.domain.Category;

public interface CategoryRepository extends CrudRepository<Category, Long> {

}
