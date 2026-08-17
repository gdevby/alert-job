package by.gdev.alert.job.llm.domain.dto.test;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Запрос на тестирование автоответа")
public class TestAutoreplyRequest {

    @NotNull(message = "ID шаблона обязателен")
    @Schema(description = "ID шаблона письма", required = true, example = "1")
    private Long templateId;

    @NotNull(message = "ID промта обязателен")
    @Schema(description = "ID промта", required = true, example = "1")
    private Long promptId;

    @NotBlank(message = "Описание заказа обязательно")
    @Schema(description = "Описание заказа (текст для анализа)", required = true, example = "Нужно разработать сайт на React...")
    private String orderDescription;
}