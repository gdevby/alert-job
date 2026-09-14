package by.gdev.alert.job.notification.config;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    public static final String COUNTER_TELEGRAM_POSITIVE = "counter_telegram_positive";
    public static final String COUNTER_TELEGRAM_NEGATIVE = "counter_telegram_negative";
    public static final String COUNTER_MAIL_POSITIVE = "counter_mail_positive";
    public static final String COUNTER_MAIL_NEGATIVE = "counter_mail_negative";
    public static final String COUNTER_WEBHOOK_POSITIVE = "counter_webhook_positive";
    public static final String COUNTER_WEBHOOK_NEGATIVE = "counter_webhook_negative";
    private static final String TELEGRAM_COUNTER_NAME = "notification_telegram";
    private static final String MAIL_COUNTER_NAME = "notification_mail";
    private static final String WEBHOOK_COUNTER_NAME = "notification_webhook";
    private static final String POSITIVE_TAG = "positive";
    private static final String NEGATIVE_TAG = "negative";
    private static final String TELEGRAM_TAG = "telegram";
    private static final String MAIL_TAG = "mail";
    private static final String WEBHOOK_TAG = "webhook";

    @Autowired
    private ApplicationContext context;
    @Autowired
    private MeterRegistry meterRegistry;

    @PostConstruct
    void init() {
        ConfigurableListableBeanFactory beanFactory = ((ConfigurableApplicationContext) context).getBeanFactory();
        beanFactory.registerSingleton(COUNTER_TELEGRAM_POSITIVE, meterRegistry.counter(TELEGRAM_COUNTER_NAME, TELEGRAM_TAG, POSITIVE_TAG));
        beanFactory.registerSingleton(COUNTER_TELEGRAM_NEGATIVE, meterRegistry.counter(TELEGRAM_COUNTER_NAME, TELEGRAM_TAG, NEGATIVE_TAG));

        beanFactory.registerSingleton(COUNTER_MAIL_POSITIVE, meterRegistry.counter(MAIL_COUNTER_NAME, MAIL_TAG, POSITIVE_TAG));
        beanFactory.registerSingleton(COUNTER_MAIL_NEGATIVE, meterRegistry.counter(MAIL_COUNTER_NAME, MAIL_TAG, NEGATIVE_TAG));

        beanFactory.registerSingleton(COUNTER_WEBHOOK_POSITIVE, meterRegistry.counter(WEBHOOK_COUNTER_NAME, WEBHOOK_TAG, POSITIVE_TAG));
        beanFactory.registerSingleton(COUNTER_WEBHOOK_NEGATIVE, meterRegistry.counter(WEBHOOK_COUNTER_NAME, WEBHOOK_TAG, NEGATIVE_TAG));
    }
}
