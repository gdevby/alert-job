package by.gdev.alert.job.notification.service.ai.queue;

import by.gdev.alert.job.notification.model.dto.AiNotificationPayload;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class AiDecisionQueue {

    private final BlockingQueue<AiNotificationPayload> queue =
            new LinkedBlockingQueue<>();

    public void submit(AiNotificationPayload payload) {
        queue.offer(payload);
    }

    public AiNotificationPayload take() throws InterruptedException {
        return queue.take();
    }

    public int size() {
        return queue.size();
    }
}

