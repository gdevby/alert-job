package by.gdev.alert.job.llm.domain.dto.order;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PriceDTO {
    private String price;
    private int value;
}