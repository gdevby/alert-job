package by.gdev.alert.job.llm.domain.dto.promt;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Запрос на создание или обновление промта")
public class PromptRequest {

    @Schema(description = "Название промта", required = true, example = "Мой промт для аналитики")
    private String name;

    @Schema(description = "Текст промта", required = true, example = "Ты — экспертный аналитик фриланс-заказов...")
    private String text;
}