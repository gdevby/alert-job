package by.gdev.common.model;

public enum NotificationType {
    ORDER,          // новый заказ
    AUTO_REPLY,     // автоответ
    TEST,           //тестовое сообщение
    CLEANUP,         //очистка сайта
    CATEGORY_CHANGE, //обновление категорий для администратора
    CATEGORY_CHANGE_USER //обновление категорий для пользователя
}