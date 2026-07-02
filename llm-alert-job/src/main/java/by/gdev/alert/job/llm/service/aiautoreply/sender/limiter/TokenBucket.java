package by.gdev.alert.job.llm.service.aiautoreply.sender.limiter;

import org.springframework.stereotype.Component;

@Component
public class TokenBucket {

    private static final int LIMIT_PER_MINUTE = 6000;

    private int used = 0;
    private long windowStart = System.currentTimeMillis();

    public synchronized void consume(int tokens) {
        long now = System.currentTimeMillis();

        // новая минутная "окошко"
        if (now - windowStart >= 60_000) {
            used = 0;
            windowStart = now;
        }

        // если не влезаем — ждём до следующей минуты
        if (used + tokens > LIMIT_PER_MINUTE) {
            long wait = 60_000 - (now - windowStart);
            if (wait > 0) {
                try {
                    Thread.sleep(wait);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            used = 0;
            windowStart = System.currentTimeMillis();
        }

        used += tokens;
    }
}
