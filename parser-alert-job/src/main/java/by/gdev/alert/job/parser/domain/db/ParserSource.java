package by.gdev.alert.job.parser.domain.db;

import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity(name = "parser_order_source")
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, exclude = {"orders"})
@AllArgsConstructor
@NoArgsConstructor
public class ParserSource extends BasicId{
	private Long source;
	private Long category;
	private Long subCategory;
	@OneToMany(mappedBy = "sourceSite")
	private Set<Order> orders;
}
