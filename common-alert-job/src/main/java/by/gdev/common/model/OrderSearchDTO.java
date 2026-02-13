package by.gdev.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderSearchDTO {
    private String title;
    private String message;
    private String link;
    private Date dateTime;
    private PriceDTO price;
    private String category;
    private String subCategory;
}
