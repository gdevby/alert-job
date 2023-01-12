package by.gdev.alert.job.parser.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import by.gdev.alert.job.parser.domain.db.FlCategory;

public interface FlCategoryRepository extends CrudRepository<FlCategory, Long> {
	Optional<FlCategory> findByNativeLocName(String nativeLocName);
}
