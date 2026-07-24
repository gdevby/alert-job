package by.gdev.alert.job.core.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Зашифрованные учётные данные пользователя")
public class UserCredentialEncrypted {

    @Schema(description = "Название учётной записи", required = true, example = "Аккаунт для Kwork")
    private String name;

    @Schema(description = "Логин", required = true, example = "user@example.com")
    private String login;

    @Schema(description = "Зашифрованный пароль", required = true, example = "6HR06ABSE3SSiHMr3n3/ig==")
    private String passwordEncrypted;
}