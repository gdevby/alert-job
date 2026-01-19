package by.gdev.alert.job.parser.domain.db;

import java.util.Date;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity(name = "parser_order")
@Table(
        name = "parser_order",
        indexes = {
                @Index(name = "idx_order_link", columnList = "link"),
                @Index(name = "idx_order_link_source", columnList = "link, source_site_id")
        }
)
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

    private Date dateTime;
    @AttributeOverrides({ @AttributeOverride(name = "price", column = @Column(name = "order_price", nullable = true)),
	    @AttributeOverride(name = "value", column = @Column(name = "order_value", nullable = true)) })
    private Price price;

    @ManyToOne(optional = false)
    private ParserSource sourceSite;
    private boolean openForAll;
    private boolean validOrder = true;
}