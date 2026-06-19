package by.gdev.alert.job.llm.domain.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Информация о цене заказа")
public class PriceDTO {

    @Schema(
            description = "Цена в текстовом виде (как на сайте)",
            example = "1000 RUB"
    )
    private String price;

    @Schema(
            description = "Цена в числовом виде (в минимальных единицах, например копейки/центах)",
            example = "1000"
    )
    private int value;
}
