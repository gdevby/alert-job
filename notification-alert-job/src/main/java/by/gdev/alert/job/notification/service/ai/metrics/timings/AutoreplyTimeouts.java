package by.gdev.alert.job.notification.service.ai.metrics.timings;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class AutoreplyTimeouts {

    @Value("${parser.autoreply.timeouts.order-element-wait-ms:60000}")
    private long orderElementWaitMs;

    @Value("${parser.autoreply.timeouts.order-element-poll-ms:10000}")
    private long orderElementPollMs;

    @Value("${parser.autoreply.timeouts.login-field-ms:8000}")
    private long loginFieldMs;

    @Value("${parser.autoreply.timeouts.otp-timeout-ms:120000}")
    private long otpTimeoutMs;

    @Value("${parser.autoreply.timeouts.navigate-retry-pause-ms:1500}")
    private long navigateRetryPauseMs;

    @Value("${parser.autoreply.timeouts.before-process-pause-ms:1000}")
    private long beforeProcessPauseMs;

    @Value("${parser.autoreply.timeouts.session-verify-headful-pause-ms:5000}")
    private long sessionVerifyHeadfulPauseMs;

    @Value("${parser.autoreply.timeouts.element-wait-ms:60000}")
    private long elementWaitMs;

    @Value("${parser.autoreply.timeouts.element-poll-ms:100}")
    private long elementPollMs;
}