package by.gdev.alert.job.llm.domain.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Расширенный запрос для AI, содержащий пользователя, модуль и список заказов")
public class AiOrderRequest {

    @Schema(
            description = "Информация о пользователе, от имени которого отправлены заказы",
            implementation = AiAppUserDTO.class
    )
    private AiAppUserDTO user;

    @Schema(
            description = "Информация о модуле, который инициировал отправку заказов",
            implementation = AiOrderModulesDTO.class
    )
    private AiOrderModulesDTO module;

    @Schema(
            description = "ID учётных данных пользователя (credential), связанных с модулем",
            example = "101"
    )
    private Long credentialId;

    @Schema(
            description = "ID шаблона, который должен использоваться для генерации ответа",
            example = "55"
    )
    private Long templateId;

    @Schema(
            description = "Список заказов, которые необходимо обработать",
            implementation = OrderDTO.class
    )
    private List<OrderDTO> orders;
}
