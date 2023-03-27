package by.gdev.alert.job.core.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import by.gdev.alert.job.core.model.db.key.DescriptionWordPrice;

public interface DescriptionWordPriceRepository extends CrudRepository<DescriptionWordPrice, Long> {

    Optional<DescriptionWordPrice> findByNameAndUuid(String name, String uuid);

    Page<DescriptionWordPrice> findAllByOrderByCounterDesc(Pageable p);

    Page<DescriptionWordPrice> findByNameIsStartingWith(String name, Pageable p);
}
