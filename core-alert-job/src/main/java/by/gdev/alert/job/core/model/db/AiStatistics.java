package by.gdev.alert.job.core.model.db;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
public class AiStatistics extends BasicId {
    private String siteName;
    private Long orderCount;
}
