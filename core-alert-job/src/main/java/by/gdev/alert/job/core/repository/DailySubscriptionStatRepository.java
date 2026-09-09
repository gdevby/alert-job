package by.gdev.alert.job.core.repository;

import by.gdev.alert.job.core.model.db.DailySubscriptionStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailySubscriptionStatRepository extends JpaRepository<DailySubscriptionStat, Long> {
    Optional<DailySubscriptionStat> findByStatDate(LocalDate date);
    List<DailySubscriptionStat> findByStatDateBetweenOrderByStatDateAsc(LocalDate from, LocalDate to);
}