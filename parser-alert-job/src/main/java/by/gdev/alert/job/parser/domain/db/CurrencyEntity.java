package by.gdev.alert.job.parser.domain.db;

import javax.persistence.Entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity(name = "parser_currency")
@EqualsAndHashCode(callSuper = true)
public class CurrencyEntity extends BasicId {

	private String currencyCode;
	private String currencyName;
	private Integer nominal;
	private Double currencyValue;
}
