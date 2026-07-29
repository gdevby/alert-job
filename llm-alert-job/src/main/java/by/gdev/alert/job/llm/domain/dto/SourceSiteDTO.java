package by.gdev.alert.job.llm.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * DTO, описывающее источник заказа:
 *  - содержит ID сайта, категории и подкатегории;
 *  - включает человекочитаемые названия;
 *  - используется для отображения информации о происхождении заказа.
 */
@Data
@Schema(description = "Информация об источнике заказа: сайт, категория и подкатегория")
public class SourceSiteDTO {

    /** ID записи источника */
    @Schema(description = "ID записи источника", example = "101")
    private Long id;

    /** ID сайта-источника */
    @Schema(description = "ID сайта, откуда пришёл заказ", example = "1")
    private Long source;

    /** Название сайта */
    @Schema(description = "Название сайта-источника", example = "FreelanceHunt")
    private String sourceName;

    /** ID категории */
    @Schema(description = "ID категории заказа", example = "12")
    private Long category;

    /** Название категории */
    @Schema(description = "Название категории", example = "Разработка ПО")
    private String categoryName;

    /** ID подкатегории */
    @Schema(description = "ID подкатегории заказа", example = "34")
    private Long subCategory;

    /** Название подкатегории */
    @Schema(description = "Название подкатегории", example = "Java Backend")
    private String subCategoryName;
}
