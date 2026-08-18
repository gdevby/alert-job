package by.gdev.alert.job.llm.domain.dto.test;

import by.gdev.alert.job.llm.domain.dto.order.OrderDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ответ тестирования автоответа")
public class TestAutoreplyResponse {

    @Schema(description = "Успешно ли выполнен запрос", example = "true")
    private boolean success;

    @Schema(description = "Сообщение об ошибке (если success=false)")
    private String error;

    @Schema(description = "Сгенерированный текст ответа")
    private String reply;

    @Schema(description = "Уверенность AI (0.0-1.0)", example = "0.95")
    private Double confidence;

    @Schema(description = "Причина принятия решения")
    private String reason;

    @Schema(description = "Тестовый заказ, на основе которого был сгенерирован ответ")
    private OrderDTO testOrder;
}