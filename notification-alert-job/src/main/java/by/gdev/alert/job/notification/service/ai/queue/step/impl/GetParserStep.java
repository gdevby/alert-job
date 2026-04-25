package by.gdev.alert.job.notification.service.ai.queue.step.impl;

import by.gdev.alert.job.notification.service.ai.parser.AutoreplyParserFactory;
import by.gdev.alert.job.notification.service.ai.parser.AutoreplyPlaywrightParser;
import by.gdev.alert.job.notification.service.ai.queue.step.AiStep;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepResult;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepType;
import by.gdev.common.model.SiteName;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetParserStep implements AiStep<SiteName, AutoreplyPlaywrightParser> {

    private final AutoreplyParserFactory parserFactory;

    @Override
    public StepType type() {
        return StepType.GET_PARSER;
    }

    @Override
    public StepResult<AutoreplyPlaywrightParser> execute(SiteName site) {
        try {
            return StepResult.ok(parserFactory.getParser(site));
        } catch (Exception e) {
            return StepResult.fail();
        }
    }
}

