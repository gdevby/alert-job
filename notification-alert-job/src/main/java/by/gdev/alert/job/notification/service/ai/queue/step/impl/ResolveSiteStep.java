package by.gdev.alert.job.notification.service.ai.queue.step.impl;

import by.gdev.alert.job.notification.model.dto.AiNotificationPayload;
import by.gdev.alert.job.notification.service.ai.queue.step.AiStep;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepResult;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepType;
import by.gdev.common.model.SiteName;
import org.springframework.stereotype.Component;

@Component
public class ResolveSiteStep implements AiStep<AiNotificationPayload, SiteName> {

    @Override
    public StepType type() {
        return StepType.RESOLVE_SITE;
    }

    @Override
    public StepResult<SiteName> execute(AiNotificationPayload payload) {
        try {
            SiteName siteEnum = SiteName.valueOf(
                    payload.getOrder().getSourceSite().getSourceName().toUpperCase()
            );
            return StepResult.ok(siteEnum);
        } catch (Exception e) {
            return StepResult.fail();
        }
    }
}


