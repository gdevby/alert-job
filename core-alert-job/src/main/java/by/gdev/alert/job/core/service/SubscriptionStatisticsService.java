package by.gdev.alert.job.core.service;

import by.gdev.alert.job.core.model.db.DailySubscriptionStat;
import by.gdev.alert.job.core.repository.AppUserRepository;
import by.gdev.alert.job.core.repository.DailySubscriptionStatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubscriptionStatisticsService {

    private final DailySubscriptionStatRepository statRepository;
    private final AppUserRepository userRepository;

    @Transactional
    public void collectTodayStat() {
        LocalDate today = LocalDate.now();
        long count = userRepository.countBySwitchOffAlertsTrue();

        DailySubscriptionStat stat = statRepository.findByStatDate(today)
                .orElse(new DailySubscriptionStat());
        stat.setStatDate(today);
        stat.setTotalUsers((int) count);
        statRepository.save(stat);

        log.info("Сохранена/обновлена статистика подписок за {}: {} пользователей", today, count);
    }

    public List<DailySubscriptionStat> getStatsForLastDays(int days) {
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(days - 1);
        return statRepository.findByStatDateBetweenOrderByStatDateAsc(from, today);
    }
}