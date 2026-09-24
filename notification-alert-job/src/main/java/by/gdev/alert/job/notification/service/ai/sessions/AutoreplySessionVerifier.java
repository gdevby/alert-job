package by.gdev.alert.job.notification.service.ai.sessions;

import by.gdev.alert.job.notification.model.dto.AiNotificationPayload;
import by.gdev.alert.job.notification.model.dto.DecryptedCredential;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepResult;
import by.gdev.common.model.SiteName;
import com.microsoft.playwright.Page;

public interface AutoreplySessionVerifier {

    SiteName getSiteName();

    StepResult<Void> verifySessionOnPage(Page page, DecryptedCredential creds);

    StepResult<Void> verifyStoredSession(String userUuid, DecryptedCredential creds, AiNotificationPayload payload);
}
