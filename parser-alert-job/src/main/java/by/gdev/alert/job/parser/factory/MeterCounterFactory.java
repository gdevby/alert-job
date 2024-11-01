package by.gdev.alert.job.parser.factory;

import by.gdev.alert.job.parser.util.SiteName;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class MeterCounterFactory {

    public static final String PROXY_CLIENT = "proxy_client";

    private final MeterRegistry meterRegistry;

    public MeterCounterFactory(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public Counter createCounter(SiteName siteName) {
        return meterRegistry.counter(PROXY_CLIENT, PROXY_CLIENT, siteName.name().toLowerCase());
    }
}
