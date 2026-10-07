package by.gdev.alert.job.notification.service.ai.metrics.timings;

/**
 * Тег {@code operation} для {@link AutoreplyDurationMetrics}.
 */
public final class AutoreplyMetricOperations {

    /** Вход / проверка сессии и пост-гейты до сохранения сессии. */
    public static final String LOGIN = "login";

    /** Заполнение и отправка отклика на заказ. */
    public static final String PROCESS_AUTOREPLY = "process_autoreply";

    private AutoreplyMetricOperations() {
    }
}
