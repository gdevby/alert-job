package by.gdev.alert.job.notification.service.ai.otp.email;

import jakarta.mail.*;
import jakarta.mail.internet.MimeMultipart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailReaderService {

    private final GdevEmailConfig gdevEmailConfig;

    /** Последний успешный IP почтового хоста — запасной вариант при сбое DNS в JVM */
    private volatile String lastResolvedMailHostIp;

    public MailReadResult readUnreadMessages() {
        Store store;
        try {
            store = connectStore();
        } catch (MessagingException e) {
            log.error("Ошибка подключения к IMAP", e);
            return MailReadResult.connectFailed("подключение к IMAP: " + rootCauseMessage(e));
        }

        List<MailDto> newMessages = new ArrayList<>();
        Folder inbox = null;
        try {
            inbox = store.getFolder(gdevEmailConfig.getFolder());
            inbox.open(Folder.READ_WRITE);
            Message[] messages = inbox.getMessages();
            for (Message msg : messages) {
                if (msg.isSet(Flags.Flag.SEEN)) {
                    continue;
                }
                try {
                    MailDto mail = readOneUnreadMessage(inbox, msg);
                    newMessages.add(mail);
                    msg.setFlag(Flags.Flag.SEEN, true);
                } catch (Exception e) {
                    log.warn("Не удалось обработать непрочитанное письмо, оставляем без флага SEEN: {}",
                            e.getMessage(), e);
                }
            }
            return MailReadResult.success(newMessages);
        } catch (Exception e) {
            log.error("Ошибка чтения почты после подключения к IMAP", e);
            return MailReadResult.partialAfterConnect(newMessages, "чтение писем: " + rootCauseMessage(e));
        } finally {
            closeQuietly(inbox);
            closeQuietly(store);
        }
    }

    private MailDto readOneUnreadMessage(Folder inbox, Message msg) throws Exception {
        long uid = ((UIDFolder) inbox).getUID(msg);
        String subject = msg.getSubject();
        String from = msg.getFrom() != null ? msg.getFrom()[0].toString() : "";
        Address[] recipients = msg.getRecipients(Message.RecipientType.TO);
        String to = recipients != null ? recipients[0].toString() : "";
        Date sentDate = msg.getSentDate();
        String body = extractBody(msg);
        log.info("АВТООТВЕТ: EMAIL -> найдено непрочитанное письмо UID={}, от: {}, тема: {}", uid, from, subject);
        return new MailDto(uid, subject, from, to, sentDate, body);
    }

    private Store connectStore() throws MessagingException {
        int attempts = Math.max(1, gdevEmailConfig.getConnectRetries());
        long delayMs = Math.max(0, gdevEmailConfig.getConnectRetryDelayMs());
        MessagingException last = null;

        for (int attempt = 1; attempt <= attempts; attempt++) {
            String connectHost = gdevEmailConfig.getHost();
            try {
                connectHost = resolveConnectHost(attempt > 1);
                Properties props = imapProperties();
                Session session = Session.getInstance(props);
                Store store = session.getStore("imaps");
                store.connect(
                        connectHost,
                        gdevEmailConfig.getUsername(),
                        gdevEmailConfig.getPassword()
                );
                rememberResolvedIp(connectHost);
                if (attempt > 1) {
                    log.info("IMAP: подключение к {} успешно с попытки {}", connectHost, attempt);
                }
                return store;
            } catch (MessagingException e) {
                last = e;
                boolean retryable = isRetryableConnectFailure(e);
                if (!retryable || attempt == attempts) {
                    throw e;
                }
                log.warn("IMAP: попытка {}/{} к {} не удалась ({}), повтор через {} мс",
                        attempt, attempts, connectHost, rootCauseMessage(e), delayMs);
                sleep(delayMs);
            }
        }
        throw last != null ? last : new MessagingException("IMAP connect failed");
    }

    private Properties imapProperties() {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.ssl.trust", "*");
        props.put("mail.imaps.ssl.checkserveridentity", "false");
        props.put("mail.imaps.connectiontimeout", String.valueOf(gdevEmailConfig.getConnectionTimeoutMs()));
        props.put("mail.imaps.timeout", String.valueOf(gdevEmailConfig.getReadTimeoutMs()));
        return props;
    }

    private String resolveConnectHost(boolean allowCachedIp) throws MessagingException {
        String host = gdevEmailConfig.getHost();
        try {
            InetAddress address = InetAddress.getByName(host);
            lastResolvedMailHostIp = address.getHostAddress();
            return host;
        } catch (UnknownHostException e) {
            if (allowCachedIp && lastResolvedMailHostIp != null) {
                log.warn("DNS не резолвит {}, используем последний известный IP {}", host, lastResolvedMailHostIp);
                return lastResolvedMailHostIp;
            }
            throw new MessagingException("Не удалось разрешить хост почты: " + host, e);
        }
    }

    private void rememberResolvedIp(String connectHost) {
        String configuredHost = gdevEmailConfig.getHost();
        if (connectHost.equals(configuredHost)) {
            try {
                lastResolvedMailHostIp = InetAddress.getByName(configuredHost).getHostAddress();
            } catch (UnknownHostException ignored) {
                // уже подключились по имени — IP необязателен
            }
        }
    }

    private static boolean isRetryableConnectFailure(MessagingException e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof UnknownHostException
                    || cause instanceof java.net.SocketTimeoutException) {
                return true;
            }
            String name = cause.getClass().getSimpleName();
            if (name.contains("MailConnectException")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private static String rootCauseMessage(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getClass().getSimpleName() + ": " + root.getMessage();
    }

    private static void sleep(long delayMs) {
        if (delayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private static void closeQuietly(Folder folder) {
        if (folder == null || !folder.isOpen()) {
            return;
        }
        try {
            folder.close(true);
        } catch (MessagingException e) {
            log.debug("Не удалось закрыть папку IMAP: {}", e.getMessage());
        }
    }

    private static void closeQuietly(Store store) {
        if (store == null || !store.isConnected()) {
            return;
        }
        try {
            store.close();
        } catch (MessagingException e) {
            log.debug("Не удалось закрыть IMAP store: {}", e.getMessage());
        }
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
