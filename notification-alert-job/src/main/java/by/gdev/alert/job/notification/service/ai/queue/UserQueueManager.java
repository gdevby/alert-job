package by.gdev.alert.job.notification.service.ai.queue;

import by.gdev.alert.job.notification.model.dto.AiNotificationPayload;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class UserQueueManager {
    private final Map<String, BlockingQueue<AiNotificationPayload>> queues = new ConcurrentHashMap<>();
    public void submit(AiNotificationPayload payload) {
        String user = payload.getUser().getUuid();
        queues.computeIfAbsent(user, u -> new LinkedBlockingQueue<>()).offer(payload);
    }

    public BlockingQueue<AiNotificationPayload> getQueue(String user) {
        return queues.get(user);
    }

    public int size(String user) {
        BlockingQueue<AiNotificationPayload> q = queues.get(user);
        return q == null ? 0 : q.size();
    }

    public Set<String> getUsers() {
        return queues.keySet();
    }
}