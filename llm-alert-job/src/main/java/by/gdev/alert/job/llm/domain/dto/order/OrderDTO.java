package by.gdev.alert.job.llm.domain.dto.order;

import by.gdev.alert.job.llm.domain.dto.SourceSiteDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderDTO {
    private String title;
    private String message;
    private String link;
    private Date dateTime;
    private PriceDTO price;
    private SourceSiteDTO sourceSite;
    private String moduleName;
    private boolean openForAll;
    private boolean validOrder;
}
