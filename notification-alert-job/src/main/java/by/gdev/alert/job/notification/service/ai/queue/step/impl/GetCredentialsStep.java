package by.gdev.alert.job.notification.service.ai.queue.step.impl;

import by.gdev.alert.job.notification.model.dto.AiNotificationPayload;
import by.gdev.alert.job.notification.model.dto.DecryptedCredential;
import by.gdev.alert.job.notification.service.ai.credential.UserCredentialService;
import by.gdev.alert.job.notification.service.ai.queue.step.AiStep;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepResult;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetCredentialsStep implements AiStep<AiNotificationPayload, DecryptedCredential> {

    private final UserCredentialService userCredentialService;

    @Override
    public StepType type() {
        return StepType.GET_CREDENTIALS;
    }

    @Override
    public StepResult<DecryptedCredential> execute(AiNotificationPayload payload) {
        try {
            var creds = userCredentialService.getUserCredentialsBlocking(payload);
            return StepResult.ok(creds);
        } catch (Exception e) {
            return StepResult.fail();
        }
    }
}
