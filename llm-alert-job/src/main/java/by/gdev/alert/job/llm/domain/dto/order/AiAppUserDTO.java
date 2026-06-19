package by.gdev.alert.job.llm.domain.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Информация о пользователе, связанная с заказами")
public class AiAppUserDTO {

    @Schema(
            description = "UUID пользователя",
            example = "123e4567-e89b-12d3-a456-426614174000"
    )
    private String uuid;

    @Schema(
            description = "Email пользователя",
            example = "user@example.com"
    )
    private String email;

    @Schema(
            description = "Telegram ID пользователя",
            example = "123456789"
    )
    private Long telegram;

    @Schema(
            description = "Флаг отключения уведомлений",
            example = "false"
    )
    private boolean switchOffAlerts;

    @Schema(
            description = "Тип отправки по умолчанию: true = email, false = telegram",
            example = "true"
    )
    private boolean defaultSendType;
}
