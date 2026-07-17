package by.gdev.alert.job.core.model.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Сокращённая информация об учётных данных пользователя")
public class UserSiteCredentialShortResponse {

    @Schema(description = "ID учётной записи", required = true, example = "1")
    private Long id;

    @Schema(description = "Название учётной записи (например, 'Аккаунт для Kwork')", required = true, example = "Аккаунт для Kwork")
    private String name;

    @Schema(description = "Логин", required = true, example = "user@example.com")
    private String login;

    @Schema(description = "Дата создания учётной записи", required = true, example = "2026-07-02T18:50:12.340107")
    private String createdAt;
}