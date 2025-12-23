package by.gdev.alert.job.parser.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import by.gdev.alert.job.parser.domain.db.ParserSource;

public interface ParserSourceRepository extends CrudRepository<ParserSource, Long>{
	
	Optional<ParserSource> findBySourceAndCategoryAndSubCategory(Long source, Long category, Long subCategory);

    void deleteBySourceAndCategoryAndSubCategory(Long source, Long category, Long subCategory);
}