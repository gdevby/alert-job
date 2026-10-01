package by.gdev.alert.job.notification.service.ai.otp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class OtpService {

    private static class OtpEntry {
        final String otp;
        /** Время из заголовка письма; если нет — момент сохранения в память. */
        final Instant codeTime;

        OtpEntry(String otp, Instant codeTime) {
            this.otp = otp;
            this.codeTime = codeTime != null ? codeTime : Instant.now();
        }
    }

    private final Map<String, OtpEntry> storage = new ConcurrentHashMap<>();
    private final Map<String, Instant> notBeforeByKey = new ConcurrentHashMap<>();
    private final Map<String, Object> locks = new ConcurrentHashMap<>();

    // Флаг доступности почты
    private volatile boolean mailAvailable = true;

    public void setMailAvailable(boolean available) {
        this.mailAvailable = available;
        if (!available) {
            synchronized (this) {
                this.notifyAll();
            }
        }
    }

    private Object lockFor(String key) {
        return locks.computeIfAbsent(key, k -> new Object());
    }

    /**
     * Ключ хранилища. Адрес приводим к единому виду: из почтового ящика он приходит как
     * «Имя &lt;user@mail.ru&gt;» и в произвольном регистре, а парсер ищет код по голому логину.
     */
    private static String key(String site, String userEmail) {
        String email = userEmail == null ? "" : userEmail.trim();
        int open = email.lastIndexOf('<');
        int close = email.lastIndexOf('>');
        if (open >= 0 && close > open) {
            email = email.substring(open + 1, close).trim();
        }
        return site + ":" + email.toLowerCase(Locale.ROOT);
    }

    public void saveOtp(String site, String userEmail, String otp) {
        saveOtp(site, userEmail, otp, null);
    }

    /**
     * @param messageSentAt время отправки письма с кодом (для отсечения старых непрочитанных писем)
     */
    public void saveOtp(String site, String userEmail, String otp, Instant messageSentAt) {
        String key = key(site, userEmail);
        OtpEntry entry = new OtpEntry(otp, messageSentAt);

        synchronized (lockFor(key)) {
            if (!isAcceptable(key, entry)) {
                log.info("OTP ignored as stale for {} (codeTime={}, notBefore={})",
                        key, entry.codeTime, notBeforeByKey.get(key));
                return;
            }
            storage.put(key, entry);
            lockFor(key).notifyAll();
        }
    }

    /**
     * Сбрасывает код в памяти и запоминает момент, после которого принимаются только новые письма с кодом.
     * Вызывать непосредственно перед действием на сайте, которое инициирует отправку OTP.
     */
    public void beginOtpWait(String site, String userEmail) {
        String key = key(site, userEmail);
        synchronized (lockFor(key)) {
            notBeforeByKey.put(key, Instant.now());
            storage.remove(key);
        }
    }

    public void invalidateOtp(String site, String userEmail) {
        String key = key(site, userEmail);
        storage.remove(key);
        notBeforeByKey.remove(key);
    }

    public String waitForOtp(String site, String userEmail, long timeoutMs) {
        String key = key(site, userEmail);
        long deadline = System.currentTimeMillis() + timeoutMs;

        synchronized (lockFor(key)) {
            while (true) {
                OtpEntry entry = storage.get(key);
                if (entry != null) {
                    if (isAcceptable(key, entry)) {
                        return entry.otp;
                    }
                    storage.remove(key);
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

    /** Допуск рассинхрона часов между IMAP и приложением. */
    private boolean isAcceptable(String key, OtpEntry entry) {
        Instant notBefore = notBeforeByKey.get(key);
        if (notBefore == null) {
            return true;
        }
        return !entry.codeTime.isBefore(notBefore.minusSeconds(30));
    }
}

