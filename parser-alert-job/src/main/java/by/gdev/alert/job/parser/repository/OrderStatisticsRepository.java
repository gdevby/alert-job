package by.gdev.alert.job.parser.repository;

import by.gdev.alert.job.parser.domain.db.OrderStatistics;
import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderStatisticsRepository extends CrudRepository<OrderStatistics, Long> {
     Optional<OrderStatistics> findBySourceSiteAndDate(SiteSourceJob sourceSite, LocalDate date);
     List<OrderStatistics> findAllByDateGreaterThanEqual(LocalDate date);
}
