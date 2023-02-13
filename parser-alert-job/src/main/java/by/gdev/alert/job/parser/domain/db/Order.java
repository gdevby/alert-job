package by.gdev.alert.job.parser.domain.db;

import java.util.Date;
import java.util.List;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.CreationTimestamp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity(name = "parser_order")
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class Order extends BasicId {
	
	private String title;
	@Column(columnDefinition = "MEDIUMTEXT")
	private String message;
	private String link;
	
	@CreationTimestamp
	@Temporal(TemporalType.TIMESTAMP)
	private Date dateTime;
	@AttributeOverrides({
		@AttributeOverride(name = "price", column = @Column(name = "order_price", nullable = true)),
		@AttributeOverride(name = "value", column = @Column(name = "order_value", nullable = true))
	})
	private Price price;
	
	@ElementCollection
	@CollectionTable(name="parser_order_technologies", joinColumns=@JoinColumn(name="parser_order_id"))
	@Column(name = "technologies", nullable = true)
	private List<String> technologies;
	@ManyToOne(optional = false)
	private ParserSource sourceSite;
	private boolean flRuForAll;
	private boolean validOrder = true;
}