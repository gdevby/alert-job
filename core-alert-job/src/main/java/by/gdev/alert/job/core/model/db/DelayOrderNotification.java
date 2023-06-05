package by.gdev.alert.job.core.model.db;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
public class DelayOrderNotification extends BasicId {

    @ManyToOne
    private AppUser user;
    private String title;
    private String link;
    private String orderName;
    private String categoryName;
    private String subCategoryName;
}
