package by.gdev.alert.job.core.model.db;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@Table(uniqueConstraints = { @UniqueConstraint(name = "site_source_q_key", columnNames = { "siteSource", "siteCategory",
	"siteSubCategory" }) })
public class SourceSite extends BasicId {
    private Long siteSource;
    private Long siteCategory;
    private Long siteSubCategory;
    private boolean active = true;
}
