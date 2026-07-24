package by.gdev.alert.job.llm.service.aiautoreply.sender.limiter;

import org.springframework.stereotype.Component;

@Component
public class TimeRateLimiter {

    // минимальный интервал между запросами к Groq (мс)
    private static final long MIN_DELAY_MS = 3000; // 3 секунды

    private long lastCall = 0L;

    public synchronized void awaitSlot() {
        long now = System.currentTimeMillis();
        long diff = now - lastCall;

        if (diff < MIN_DELAY_MS) {
            long wait = MIN_DELAY_MS - diff;
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        lastCall = System.currentTimeMillis();
    }
}
