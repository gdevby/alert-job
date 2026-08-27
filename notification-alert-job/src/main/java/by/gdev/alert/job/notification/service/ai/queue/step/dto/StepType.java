package by.gdev.alert.job.notification.service.ai.queue.step.dto;

public enum StepType {
    RESOLVE_SITE("Определение сайта"),
    GET_PARSER("Получение парсера"),
    GET_CREDENTIALS("Получение учётных данных"),
    SEND_AUTOREPLY("Отправка автоответа"),
    SEND_NOTIFICATION("Отправка уведомления");

    private final String displayName;

    StepType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}