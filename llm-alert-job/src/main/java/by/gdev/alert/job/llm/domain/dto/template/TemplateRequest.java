package by.gdev.alert.job.llm.domain.dto.template;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Запрос на создание или обновление шаблона")
public class TemplateRequest {

    @Schema(description = "Название шаблона", required = true, example = "Order Template #1")
    private String name;

    @Schema(description = "Содержимое шаблона (HTML)", required = true, example = "<p>Здравствуйте! Я готов выполнить вашу задачу.</p>")
    private String text;
}