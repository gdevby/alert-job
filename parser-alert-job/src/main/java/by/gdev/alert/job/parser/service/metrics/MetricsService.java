package by.gdev.alert.job.parser.service.metrics;

import by.gdev.alert.job.parser.factory.MeterCounterFactory;
import by.gdev.alert.job.parser.util.SiteName;
import io.micrometer.core.instrument.Counter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;

@Service
public class MetricsService {

    private final Map<SiteName, Counter> counterMap = new EnumMap<>(SiteName.class);
    private final MeterCounterFactory counterFactory;

    @Autowired
    public MetricsService(MeterCounterFactory counterFactory) {
        this.counterFactory = counterFactory;

        for (SiteName siteName : SiteName.values()) {
            counterMap.put(siteName, counterFactory.createCounter(siteName));
        }
    }

    public void save(SiteName siteName, int size) {
        Counter counter = counterMap.get(siteName);
        if (counter != null) {
            counter.increment(size);
        } else {
            throw new IllegalArgumentException("Counter not defined for site: " + siteName);
        }
    }

}
