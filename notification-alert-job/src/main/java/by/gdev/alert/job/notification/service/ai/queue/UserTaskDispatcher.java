package by.gdev.alert.job.notification.service.ai.queue;

import by.gdev.alert.job.notification.model.dto.AiNotificationPayload;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserTaskDispatcher {
    private final UserQueueManager queueManager;
    private final AiDecisionWorker worker;
    //сколько пользователей могут работать одновременно
    private final ExecutorService executor = Executors.newFixedThreadPool(1);

    @PostConstruct
    public void start() {
        log.debug("UserTaskDispatcher started!");
        for (int i = 0; i < 1; i++) {
            executor.submit(this::dispatchLoop);
        }
    }

    private void dispatchLoop() {
        while (true) {
            boolean anyWork = false;
            for (String user : queueManager.getUsers()) {
                BlockingQueue<AiNotificationPayload> q = queueManager.getQueue(user);
                if (q == null) continue;
                AiNotificationPayload task = q.poll();
                if (task != null) {
                    anyWork = true;
                    log.debug(">> Dispatching task for user: {}", user);
                    worker.process(task);
                }
            }

            try {
                if (anyWork) {
                    // Были задачи — спим мало, чтобы быстро продолжить
                    Thread.sleep(10);
                } else {
                    // Ничего не делали — спим дольше, чтобы не жрать CPU
                    Thread.sleep(200);
                }
            } catch (InterruptedException ignored) {}
        }
    }
}