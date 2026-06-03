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

    // key = site + ":" + userEmail
    private final Map<String, OtpEntry> storage = new ConcurrentHashMap<>();

    public void saveOtp(String site, String userEmail, String otp) {
        String key = site + ":" + userEmail;
        storage.put(key, new OtpEntry(otp));
        log.debug("OTP SAVED: {} for {}", otp, key);
    }

    public String getOtp(String site, String userEmail) {
        String key = site + ":" + userEmail;
        OtpEntry entry = storage.get(key);

        if (entry == null) {
            log.warn("OTP NOT FOUND for {}", key);
            return null;
        }

        // TTL 10 минут
        if (Instant.now().minusSeconds(600).isAfter(entry.createdAt)) {
            log.warn("OTP EXPIRED for {}", key);
            storage.remove(key);
            return null;
        }

        log.debug("OTP RETURNED: {} for {}", entry.otp, key);
        return entry.otp;
    }
}
