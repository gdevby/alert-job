package by.gdev.alert.job.core.model.db.key;

import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import by.gdev.alert.job.core.model.db.SourceSite;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(indexes = {@Index(columnList = "uuid", name = "TITLE_WORD_UUID")})
public class TitleWord extends Word {
	@ManyToOne
	private SourceSite sourceSite;
	private String uuid;
	
	
}
