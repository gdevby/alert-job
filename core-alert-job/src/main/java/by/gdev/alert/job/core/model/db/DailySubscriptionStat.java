package by.gdev.alert.job.core.model.db;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "daily_subscription_stats")
@Data
public class DailySubscriptionStat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stat_date", nullable = false, unique = true)
    private LocalDate statDate;

    @Column(name = "total_users", nullable = false)
    private int totalUsers;
}