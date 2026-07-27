package by.gdev.alert.job.core.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Информация о поддерживаемом сайте")
public class SiteDTO {

    @Schema(description = "ID сайта", required = true, example = "1")
    private Long id;

    @Schema(description = "Название сайта", required = true, example = "FLRU")
    private String name;
}