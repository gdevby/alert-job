package by.gdev.alert.job.notification.service.ai.otp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class OtpService {

    private static class OtpEntry {
        final String otp;
        final Instant createdAt;

        OtpEntry(String otp) {
            this.otp = otp;
            this.createdAt = Instant.now();
        }
    }

    private final Map<String, OtpEntry> storage = new ConcurrentHashMap<>();
    private final Map<String, Object> locks = new ConcurrentHashMap<>();

    private Object lockFor(String key) {
        return locks.computeIfAbsent(key, k -> new Object());
    }

    public void saveOtp(String site, String userEmail, String otp) {
        String key = site + ":" + userEmail;
        storage.put(key, new OtpEntry(otp));
        log.debug("OTP SAVED: {} for {}", otp, key);
        synchronized (lockFor(key)) {
            lockFor(key).notifyAll();
        }
    }

    public String waitForOtp(String site, String userEmail, long timeoutMs) {
        String key = site + ":" + userEmail;
        long deadline = System.currentTimeMillis() + timeoutMs;

        synchronized (lockFor(key)) {
            while (true) {
                OtpEntry entry = storage.get(key);
                if (entry != null) {
                    return entry.otp;
                }

                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    log.warn("OTP WAIT TIMEOUT for {}", key);
                    return null;
                }

                try {
                    lockFor(key).wait(remaining);
                } catch (InterruptedException ignored) {}
            }
        }
    }
}
