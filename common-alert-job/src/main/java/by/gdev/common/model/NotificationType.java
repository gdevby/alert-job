package by.gdev.common.model;

public enum NotificationType {
    ORDER,          // новый заказ
    AUTO_REPLY, // автоответ
    AUTO_REPLY_ERROR, //ошибка автоответа
    OTP_MAIL_ERROR,  // ошибка получения OTP (недоступность почтового сервера) – для администратора
    TEST,           //тестовое сообщение
    CLEANUP,        //очистка сайта
    CATEGORY_CHANGE_ADMIN, //обновление категорий для администратора
    CATEGORY_CHANGE_USER //обновление категорий для пользователя
}
