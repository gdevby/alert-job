package by.gdev.common.model;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
	private String moduleName; //field for internal processing, not for receiving
	private boolean openForAll;
	private boolean validOrder;
}
