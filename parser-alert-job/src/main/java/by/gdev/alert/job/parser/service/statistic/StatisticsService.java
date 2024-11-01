package by.gdev.alert.job.parser.service.statistic;

import by.gdev.alert.job.parser.domain.db.OrderStatistics;
import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.repository.OrderStatisticsRepository;
import by.gdev.alert.job.parser.repository.SiteSourceJobRepository;
import by.gdev.alert.job.parser.util.SiteName;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final OrderStatisticsRepository orderStatisticsRepository;
    private final SiteSourceJobRepository siteSourceJobRepository;

    public void save(SiteName siteName, int count) {
        SiteSourceJob site = siteSourceJobRepository.findById(siteName.getId()).get();
        LocalDate currentDate = LocalDate.now();
        Optional<OrderStatistics> counter = orderStatisticsRepository.findBySourceSiteAndDate(site, currentDate);
        if (counter.isPresent()) {
            OrderStatistics entityFromDb = counter.get();
            entityFromDb.setNumberOfOrders(entityFromDb.getNumberOfOrders() + count);
            orderStatisticsRepository.save(entityFromDb);
        } else {
            OrderStatistics entity = new OrderStatistics();
            entity.setNumberOfOrders(count);
            entity.setDate(currentDate);
            entity.setSourceSite(site);
            orderStatisticsRepository.save(entity);
        }
    }

    public List<OrderStatistics> getStatistics(int numberOfDays) {
        LocalDate date = LocalDate.now().minusDays(numberOfDays);
        return orderStatisticsRepository.findAllByDateGreaterThanEqual(date);
    }

}
