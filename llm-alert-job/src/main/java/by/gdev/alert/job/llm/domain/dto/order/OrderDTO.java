package by.gdev.alert.job.llm.domain.dto.order;

import by.gdev.alert.job.llm.domain.dto.SourceSiteDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Информация о заказе, полученном с внешнего сайта")
public class OrderDTO {

    @Schema(
            description = "Заголовок заказа",
            example = "Нужен Java разработчик для парсинга API"
    )
    private String title;

    @Schema(
            description = "Полное описание заказа",
            example = "Требуется написать парсер для REST API, опыт работы с Spring Boot обязателен."
    )
    private String message;

    @Schema(
            description = "Ссылка на заказ",
            example = "https://freelancehunt.com/project/parser-api/123456.html"
    )
    private String link;

    @Schema(
            description = "Дата и время публикации заказа",
            example = "2025-06-19T10:15:30Z"
    )
    private Date dateTime;

    @Schema(
            description = "Информация о цене заказа",
            implementation = PriceDTO.class
    )
    private PriceDTO price;

    @Schema(
            description = "Источник заказа: сайт, категория, подкатегория",
            implementation = SourceSiteDTO.class
    )
    private SourceSiteDTO sourceSite;

    @Schema(
            description = "Название модуля, который обработал заказ",
            example = "FreelanceParser"
    )
    private String moduleName;

    @Schema(
            description = "Флаг: заказ открыт для всех пользователей",
            example = "true"
    )
    private boolean openForAll;

    @Schema(
            description = "Флаг валидности заказа (например, не удалён, не скрыт)",
            example = "true"
    )
    private boolean validOrder;
}
