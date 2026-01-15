package by.gdev.alert.job.parser.domain.db;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;

@Entity
@Table(name = "order_links",
		uniqueConstraints = {
				@UniqueConstraint(
						name = "UK_order_links_unique",
						columnNames = {"category_id", "sub_category_id", "links"}
				)
		},
		indexes = {
				@Index(name = "idx_order_links_links", columnList = "links"),
				@Index(name = "idx_order_links_created", columnList = "createdDate")
		})
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderLinks extends BasicId {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id", nullable = false,
			foreignKey = @ForeignKey(name = "fk_order_links_category"))
	private Category category;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sub_category_id", nullable = true,
			foreignKey = @ForeignKey(name = "fk_order_links_subcategory"))
	private Subcategory subCategory;

	@Column(name = "links", nullable = false, length = 500)
	private String links;

	@CreationTimestamp
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "created_date", updatable = false)
	private Date createdDate;

	@PrePersist
	@PreUpdate
	private void validate() {
		if (links == null || links.isBlank()) {
			throw new IllegalArgumentException("Links cannot be null or empty");
		}
		if (category == null) {
			throw new IllegalArgumentException("Category cannot be null");
		}
	}

	@Override
	public String toString() {
		return String.format("OrderLinks{id=%d, category=%s, subCategory=%s, links=%s}",
				getId(),
				category != null ? category.getId() : "null",
				subCategory != null ? subCategory.getId() : "null",
				links != null ? (links.length() > 50 ? links.substring(0, 50) + "..." : links) : "null");
	}
}