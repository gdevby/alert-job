package by.gdev.alert.job.core.service.credential;

import by.gdev.alert.job.core.client.NotificationClient;
import by.gdev.alert.job.core.exeption.ai.AccessDeniedException;
import by.gdev.alert.job.core.exeption.ai.credential.CredentialNotFoundException;
import by.gdev.alert.job.core.exeption.ai.credential.CredentialValidationJobNotFoundException;
import by.gdev.alert.job.core.model.credential.dto.CredentialValidationJobResponse;
import by.gdev.alert.job.core.model.credential.dto.CredentialValidationJobStatus;
import by.gdev.alert.job.core.model.credential.dto.CredentialValidationResult;
import by.gdev.alert.job.core.model.db.AppUser;
import by.gdev.alert.job.core.model.db.ai.UserSiteCredential;
import by.gdev.alert.job.core.repository.ai.UserSiteCredentialRepository;
import by.gdev.alert.job.core.service.AppUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * Асинхронная проверка сохранённых учётных данных: core отдаёт jobId сразу (202),
 * длительный вызов notification выполняется в {@code credentialValidationExecutor}.
 * Фронт опрашивает {@link #getStatus}. Задачи хранятся в памяти до {@link #purgeExpiredJobs()}.
 */
@Slf4j
@Service
public class CredentialValidationJobService {

    /** Время жизни записи о задаче в памяти (для опроса статуса после завершения). */
    private static final long JOB_TTL_MS = 30 * 60 * 1000L;

    private final UserSiteCredentialRepository userSiteCredentialRepository;
    private final NotificationClient notificationClient;
    private final AppUserService appUserService;
    private final Executor credentialValidationExecutor;

    /** jobId → состояние; не переживает рестарт core. */
    private final Map<String, JobEntry> jobs = new ConcurrentHashMap<>();

    public CredentialValidationJobService(
            UserSiteCredentialRepository userSiteCredentialRepository,
            NotificationClient notificationClient,
            AppUserService appUserService,
            @Qualifier("credentialValidationExecutor") Executor credentialValidationExecutor) {
        this.userSiteCredentialRepository = userSiteCredentialRepository;
        this.notificationClient = notificationClient;
        this.appUserService = appUserService;
        this.credentialValidationExecutor = credentialValidationExecutor;
    }

    /** Создаёт задачу и запускает проверку в фоне; ответ сразу со статусом PENDING. */
    public CredentialValidationJobResponse start(Long credentialId, String userUuid) {
        UserSiteCredential cred = userSiteCredentialRepository.findById(credentialId)
                .orElseThrow(() -> new CredentialNotFoundException("Учетные данные не найдены: " + credentialId));
        if (!cred.getUserUuid().equals(userUuid)) {
            throw new AccessDeniedException("У вас нет доступа к этим учётным данным");
        }

        String jobId = UUID.randomUUID().toString();
        JobEntry entry = new JobEntry(userUuid, credentialId, CredentialValidationJobStatus.PENDING, null, Instant.now());
        jobs.put(jobId, entry);

        credentialValidationExecutor.execute(() -> runValidation(jobId, cred));
        log.info("CREDENTIAL_VALIDATE_JOB: started jobId={} credentialId={} user={}", jobId, credentialId, userUuid);
        return toResponse(jobId, entry);
    }

    public CredentialValidationJobResponse getStatus(String jobId, String userUuid) {
        JobEntry entry = jobs.get(jobId);
        if (entry == null) {
            throw new CredentialValidationJobNotFoundException("Задача проверки не найдена: " + jobId);
        }
        if (!entry.userUuid.equals(userUuid)) {
            throw new AccessDeniedException("Нет доступа к этой задаче проверки");
        }
        return toResponse(jobId, entry);
    }

    private void runValidation(String jobId, UserSiteCredential cred) {
        JobEntry entry = jobs.get(jobId);
        if (entry == null) {
            return;
        }
        try {
            CredentialValidationResult result = notificationClient.validateCredentials(
                    cred.getUserUuid(),
                    resolveUserEmail(cred.getUserUuid()),
                    cred.getSiteId(),
                    cred.getLogin(),
                    cred.getPasswordEncrypted()
            );
            entry.result = result;
            entry.status = result.isSuccess()
                    ? CredentialValidationJobStatus.COMPLETED
                    : CredentialValidationJobStatus.FAILED;
            entry.finishedAt = Instant.now();
            log.info("CREDENTIAL_VALIDATE_JOB: finished jobId={} credentialId={} success={}",
                    jobId, cred.getId(), result.isSuccess());
        } catch (Exception e) {
            log.error("CREDENTIAL_VALIDATE_JOB: error jobId={} credentialId={}", jobId, cred.getId(), e);
            entry.result = CredentialValidationResult.fail("Ошибка проверки: " + e.getMessage());
            entry.status = CredentialValidationJobStatus.FAILED;
            entry.finishedAt = Instant.now();
        }
    }

    /** Удаляет устаревшие jobId, чтобы не раздувать память. */
    @Scheduled(fixedRate = 300_000)
    void purgeExpiredJobs() {
        Instant cutoff = Instant.now().minusMillis(JOB_TTL_MS);
        Iterator<Map.Entry<String, JobEntry>> it = jobs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, JobEntry> e = it.next();
            Instant ref = e.getValue().finishedAt != null ? e.getValue().finishedAt : e.getValue().createdAt;
            if (ref.isBefore(cutoff)) {
                it.remove();
            }
        }
    }

    private CredentialValidationJobResponse toResponse(String jobId, JobEntry entry) {
        CredentialValidationResult result = entry.status == CredentialValidationJobStatus.PENDING ? null : entry.result;
        return new CredentialValidationJobResponse(jobId, entry.status, result);
    }

    private String resolveUserEmail(String userUuid) {
        return appUserService.findByUuid(userUuid)
                .map(AppUser::getEmail)
                .filter(email -> !email.isBlank())
                .orElse(null);
    }

    private static final class JobEntry {
        final String userUuid;
        final Long credentialId;
        volatile CredentialValidationJobStatus status;
        volatile CredentialValidationResult result;
        final Instant createdAt;
        volatile Instant finishedAt;

        JobEntry(String userUuid, Long credentialId, CredentialValidationJobStatus status,
                 CredentialValidationResult result, Instant createdAt) {
            this.userUuid = userUuid;
            this.credentialId = credentialId;
            this.status = status;
            this.result = result;
            this.createdAt = createdAt;
        }
    }
}
