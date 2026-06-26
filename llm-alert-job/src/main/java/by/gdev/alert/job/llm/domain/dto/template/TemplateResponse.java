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

    /** Название шаблона */
    @Schema(description = "Название шаблона", example = "Default Reply Template")
    private String name;

    /** ID шаблона */
    @Schema(description = "Уникальный идентификатор шаблона", example = "42")
    private Long id;

    /** HTML‑контент шаблона */
    @Schema(description = "HTML содержимое шаблона")
    private String htmlTemplate;

    /** Дата создания шаблона */
    @Schema(description = "Дата создания шаблона", example = "2026-06-19T08:44:32")
    private String createdAt;
}
