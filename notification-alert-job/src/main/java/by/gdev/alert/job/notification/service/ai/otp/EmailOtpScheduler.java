package by.gdev.alert.job.notification.service.ai.otp;

import by.gdev.alert.job.notification.service.ai.otp.email.EmailReaderService;
import by.gdev.alert.job.notification.service.ai.otp.email.MailDto;
import by.gdev.common.model.SiteName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailOtpScheduler {
    private final EmailReaderService emailReaderService;
    private final OtpService otpService;

    private static final DateTimeFormatter DATE_TIME_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    @Scheduled(fixedDelay = 30000)
    public void checkMailbox() {
        log.debug("Запуск шедулера проверки почты: {} ...", LocalDateTime.now().format(DATE_TIME_FMT));
        try {
            List<MailDto> newMessages = emailReaderService.readUnreadMessages();
            for (MailDto mail : newMessages) {
                log.debug("Новое письмо UID={}", mail.uid());
                String userId = detectUser(mail);
                if (userId == null) {
                    log.warn("Не удалось определить пользователя для письма UID={}", mail.uid());
                }
                // Определяем сайт по отправителю/теме/телу
                SiteName site = detectSite(mail);
                if (site == null) {
                    log.warn("Не удалось определить сайт для письма UID={}", mail.uid());
                }
                String otp = extractOtp(mail.body());
                if (otp != null && site != null && userId != null) {
                    otpService.saveOtp(site.name(), userId, otp);
                    log.debug("OTP={} для userId={} (site={})", otp, userId, site);
                } else {
                    log.warn("Не найден OTP в письме UID={}", mail.uid());
                }
            }
        } catch (Exception e) {
            log.error("Ошибка чтения почтового ящика", e);
        }
    }

    private String detectUser(MailDto mail) {
        return mail.to();
    }

    private SiteName detectSite(MailDto mail) {
        String from = mail.from().toLowerCase();
        String subject = mail.subject().toLowerCase();
        String body = mail.body().toLowerCase();
        for (SiteName site : SiteName.values()) {
            String lexeme = site.name().toLowerCase();
            if (from.contains(lexeme) || subject.contains(lexeme) || body.contains(lexeme)) {
                return site;
            }
        }
        return null;
    }

    private String extractOtp(String raw) {
        if (raw == null) return null;
        String text = raw
                .replaceAll("<[^>]*>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
        Pattern p = Pattern.compile("Код для подтверждения\\s*[:\\-]?\\s*(\\d{4,6})",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

        Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }





}
