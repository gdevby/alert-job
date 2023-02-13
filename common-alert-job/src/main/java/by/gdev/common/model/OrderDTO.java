package by.gdev.common.model;

import java.util.Date;
import java.util.List;

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
	private List<String> technologies;
	private SourceSiteDTO sourceSite;
	private boolean flRuForAll;
	private boolean validOrder;
}
