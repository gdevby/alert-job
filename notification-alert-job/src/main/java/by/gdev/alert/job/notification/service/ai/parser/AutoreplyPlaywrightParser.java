package by.gdev.alert.job.notification.service.ai.parser;

import by.gdev.alert.job.notification.model.dto.DecryptedCredential;
import by.gdev.alert.job.notification.model.dto.AiNotificationPayload;
import by.gdev.common.model.SiteName;

public interface AutoreplyPlaywrightParser {

    SiteName getSiteName();
    /**
     * Логинится на сайт, переходит на страницу заказа и отправляет автоответ.
     *
     * @param creds   — логин + расшифрованный пароль пользователя
     * @param payload — данные заказа, модуля, ссылки, текста автоответа
     * @return true если автоответ успешно отправлен
     */
    boolean sendAutoreply(DecryptedCredential creds, AiNotificationPayload payload);
}

