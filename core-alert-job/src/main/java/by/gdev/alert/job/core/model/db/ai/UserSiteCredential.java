package by.gdev.alert.job.core.model.db.ai;

import by.gdev.alert.job.core.model.db.BasicId;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_site_credentials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSiteCredential extends BasicId {

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Column(name = "user_uuid", nullable = false, columnDefinition = "CHAR(36)")
    private String userUuid;

    @Column(nullable = false)
    private String login;

    @Column(name = "password_encrypted", nullable = false)
    private String passwordEncrypted;

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

