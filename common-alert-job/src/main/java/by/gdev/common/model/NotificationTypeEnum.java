package by.gdev.common.model;

public enum NotificationTypeEnum {
    NONE("Не отправлять уведомления"),
    EMAIL("Только Email"),
    TELEGRAM("Только Telegram");

    private final String description;

    NotificationTypeEnum(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}