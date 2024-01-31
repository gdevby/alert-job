package by.gdev.alert.job.parser.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import by.gdev.alert.job.parser.domain.db.CurrencyEntity;

public interface CurrencyRepository extends CrudRepository<CurrencyEntity, Long> {

	Optional<CurrencyEntity> findByCurrencyCodeAndCurrencyName(String currencyCode, String currencyName);

	Optional<CurrencyEntity> findByCurrencyCode(String currencyCode);

}
