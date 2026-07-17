package by.gdev.alert.job.llm.domain.dto.template;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * DTO ответа с информацией о шаблоне:
 *  - содержит имя, ID, HTML‑контент и дату создания;
 *  - используется при отображении списка шаблонов и загрузке конкретного шаблона.
 */
@Data
@Schema(description = "Ответ с информацией о шаблоне HTML для автоответов")
public class TemplateResponse {

    @Schema(description = "Название шаблона", required = true, example = "Default Reply Template")
    private String name;

    @Schema(description = "Уникальный идентификатор шаблона", required = true, example = "42")
    private Long id;

    @Schema(description = "Cодержимое шаблона (HTML)", required = true, example = "<p>Здравствуйте!</p>")
    private String text;

    @Schema(description = "Дата создания шаблона", required = true, example = "2026-06-19T08:44:32")
    private String createdAt;
}