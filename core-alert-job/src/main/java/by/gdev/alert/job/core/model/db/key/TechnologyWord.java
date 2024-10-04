package by.gdev.alert.job.core.model.db.key;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import by.gdev.alert.job.core.model.db.SourceSite;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(indexes = {@Index(columnList = "uuid", name = "TECHNOLOGY_WORD_UUID")})
public class TechnologyWord extends Word {
	@ManyToOne
	private SourceSite sourceSite;
	private String uuid;
}
