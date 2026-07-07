package by.gdev.alert.job.core.model.db.ai;

import by.gdev.alert.job.core.model.db.BasicId;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "account_template_binding")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountTemplateBinding extends BasicId {

    @Column(name = "module_id", nullable = false)
    private Long moduleId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    //промт
    @Column(name = "promt_id", nullable = false)
    private Long promtId;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "user_uuid", nullable = false, columnDefinition = "CHAR(36)")
    private String userUuid;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}