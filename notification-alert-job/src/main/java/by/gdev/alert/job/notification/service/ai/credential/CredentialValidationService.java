package by.gdev.alert.job.notification.service.ai.credential;

import by.gdev.alert.job.notification.model.AutoreplyMode;
import by.gdev.alert.job.notification.model.dto.AiNotificationPayload;
import by.gdev.alert.job.notification.model.dto.AiOrderModulesDTO;
import by.gdev.alert.job.notification.model.dto.DecryptedCredential;
import by.gdev.alert.job.notification.model.dto.AiAppUserDTO;
import by.gdev.alert.job.notification.model.dto.credential.CredentialValidationResult;
import by.gdev.alert.job.notification.service.ai.parser.AutoreplyParserFactory;
import by.gdev.alert.job.notification.service.ai.parser.AutoreplyPlaywrightParser;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepResult;
import by.gdev.common.model.SiteName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CredentialValidationService {

    private final AutoreplyParserFactory parserFactory;
    private final UserCredentialService userCredentialService;

    public CredentialValidationResult validate(String uuid, Long siteId, String login, String password) {
        SiteName siteName;
        try {
            siteName = SiteName.fromId(siteId);
        } catch (IllegalArgumentException e) {
            log.warn("Неизвестный siteId: {}", siteId);
            return CredentialValidationResult.fail("Неизвестный siteId: " + siteId);
        }

        AutoreplyPlaywrightParser parser;
        try {
            parser = parserFactory.getParser(siteName);
        } catch (IllegalArgumentException e) {
            log.warn("Парсер для сайта {} не найден", siteName);
            return CredentialValidationResult.fail("Парсер для сайта " + siteName + " не найден");
        }

        // Создаём минимальный payload для логина
        AiNotificationPayload payload = new AiNotificationPayload();
        //модуль заглушка
        AiOrderModulesDTO module = new AiOrderModulesDTO();
        module.setId(1L);
        payload.setModule(module);

        //Пользователь, который сделал запрос
        AiAppUserDTO user = new AiAppUserDTO();
        user.setUuid(uuid);
        payload.setUser(user);

        DecryptedCredential decryptedCred =  userCredentialService.getUserCredentials(login, password);
        StepResult<Void> result = parser.sendAutoreply(decryptedCred, payload, AutoreplyMode.LOGIN_ONLY);
        if (result.success()) {
            return CredentialValidationResult.success();
        } else {
            return CredentialValidationResult.fail(result.getErrorMessage());
        }
    }
}