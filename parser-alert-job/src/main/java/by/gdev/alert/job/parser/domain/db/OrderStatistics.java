package by.gdev.alert.job.parser.domain.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity(name = "order_statistics")
@Getter
@Setter
@NoArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(name = "unique_date_and_source_site", columnNames = {"date", "sourceSite"}))
public class OrderStatistics extends BasicId{
    @Column(nullable = false)
    private int numberOfOrders;
    @Column(nullable = false)
    private LocalDate date;
    @ManyToOne
    @JoinColumn(name = "site_source_job_id", nullable = false)
    private SiteSourceJob sourceSite;
}
