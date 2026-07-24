package by.gdev.alert.job.notification.service.ai.otp.email;

import jakarta.mail.*;
import jakarta.mail.internet.MimeMultipart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailReaderService {

    private final GdevEmailConfig gdevEmailConfig;

    public List<MailDto> readUnreadMessages() {
        List<MailDto> newMessages = new ArrayList<>();
        try {
            Properties props = new Properties();
            props.put("mail.store.protocol", "imaps");
            props.put("mail.imaps.ssl.trust", "*");
            props.put("mail.imaps.connectiontimeout", "5000");
            props.put("mail.imaps.timeout", "5000");

            Session session = Session.getInstance(props);
            Store store = session.getStore("imaps");
            store.connect(
                    gdevEmailConfig.getHost(),
                    gdevEmailConfig.getUsername(),
                    gdevEmailConfig.getPassword()
            );

            Folder inbox = store.getFolder(gdevEmailConfig.getFolder());
            inbox.open(Folder.READ_WRITE); // чтобы мы могли менять флаги у писем
            Message[] messages = inbox.getMessages();
            for (Message msg : messages) {
                // Берём только НЕпрочитанные письма
                if (msg.isSet(Flags.Flag.SEEN)) {
                    continue;
                }
                long uid = ((UIDFolder) inbox).getUID(msg);
                String subject = msg.getSubject();
                String from = msg.getFrom() != null ? msg.getFrom()[0].toString() : "";
                Address[] recipients = msg.getRecipients(Message.RecipientType.TO);
                String to = recipients != null ? recipients[0].toString() : "";
                Date sentDate = msg.getSentDate();
                String body = extractBody(msg);

                newMessages.add(new MailDto(uid, subject, from, to, sentDate, body));
                // Помечаем это письмо как прочитанное
                msg.setFlag(Flags.Flag.SEEN, true);
                log.debug("Обработано новое письмо UID={} (SEEN=true)", uid);
            }
            inbox.close(true);
            store.close();
        } catch (Exception e) {
            log.error("Ошибка чтения почты", e);
        }
        return newMessages;
    }

    private String extractBody(Message message) throws Exception {
        Object content = message.getContent();
        if (content instanceof String) {
            return (String) content;
        }
        if (content instanceof MimeMultipart) {
            return getTextFromMimeMultipart((MimeMultipart) content);
        }
        if (content instanceof java.io.InputStream) {
            try (java.io.InputStream is = (java.io.InputStream) content) {
                return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        }
        if (message.isMimeType("multipart/*")) {
            try {
                MimeMultipart multipart = (MimeMultipart) message.getContent();
                return getTextFromMimeMultipart(multipart);
            } catch (ClassCastException e) {
                Object raw = message.getContent();
                if (raw instanceof java.io.InputStream) {
                    try (java.io.InputStream is = (java.io.InputStream) raw) {
                        return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    }
                }
            }
        }

        return "";
    }

    private String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws Exception {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < mimeMultipart.getCount(); i++) {
            BodyPart part = mimeMultipart.getBodyPart(i);

            if (part.isMimeType("text/plain")) {
                result.append(part.getContent());
            } else if (part.isMimeType("text/html")) {
                result.append(part.getContent());
            } else if (part.getContent() instanceof MimeMultipart) {
                result.append(getTextFromMimeMultipart((MimeMultipart) part.getContent()));
            }
        }
        return result.toString();
    }
}


