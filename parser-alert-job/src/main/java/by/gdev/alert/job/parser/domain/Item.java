package by.gdev.alert.job.parser.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Item {
	@JsonProperty("Id")
	private int id;
	@JsonProperty("Name")
	private String name;
	@JsonProperty("IsMarker")
	private boolean isMarker;
	@JsonProperty("PriceAmount")
	private int priceAmount;
	@JsonProperty("StatusText")
	private String statusText;
	@JsonProperty("StatusFlag")
	private String statusFlag;
	@JsonProperty("CategoryFlag")
	private String categoryFlag;
	@JsonProperty("Address")
	private String address;
	@JsonProperty("Url")
	private String url;
	@JsonProperty("DateTimeString")
	private String dateTimeString;
	@JsonProperty("IsMine")
	private boolean isMine;
	@JsonProperty("IsDraft")
	private boolean isDraft;
	@JsonProperty("Viewed")
	private boolean viewed;
	@JsonProperty("IsRegular")
	private boolean isRegular;
	@JsonProperty("PriceRangeId")
	private int priceRangeId;
	@JsonProperty("BudgetDescription")
	private String budgetDescription;
	@JsonProperty("IsSbr")
	private boolean isSbr;
	@JsonProperty("IsB2B")
	private boolean isB2B;
	@JsonProperty("OffersCount")
	private int offersCount;
	@JsonProperty("Distance")
	private double distance;
	@JsonProperty("IsActual")
	private boolean isActual;
	@JsonProperty("CouponsForPaying")
	private double couponsForPaying;
	@JsonProperty("PreferInsuredExecutor")
	private boolean preferInsuredExecutor;
}
