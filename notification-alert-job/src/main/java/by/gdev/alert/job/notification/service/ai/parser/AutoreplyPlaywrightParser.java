package by.gdev.alert.job.notification.service.ai.parser;

import by.gdev.alert.job.notification.model.dto.DecryptedCredential;
import by.gdev.alert.job.notification.model.dto.AiNotificationPayload;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepResult;
import by.gdev.common.model.SiteName;

public interface AutoreplyPlaywrightParser {

    SiteName getSiteName();

    /**
     * Логинится на сайт, переходит на страницу заказа и отправляет автоответ.
     *
     * @param creds   — логин + расшифрованный пароль пользователя
     * @param payload — данные заказа, модуля, ссылки, текста автоответа
     * @return StepResult с описанием ошибки и скриншотом при неудаче
     */
    StepResult<Void> sendAutoreply(DecryptedCredential creds, AiNotificationPayload payload);
}

