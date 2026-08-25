package vn.gpg.unionportal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.gpg.unionportal.model.DomainEnums.ReportStatus;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "monthly_reports", uniqueConstraints = @UniqueConstraint(columnNames = {"union_unit_id", "report_month"}))
@Getter
@Setter
@NoArgsConstructor
public class MonthlyReport extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "union_unit_id", nullable = false)
    private UnionUnit unionUnit;

    @Column(name = "report_month", nullable = false)
    private LocalDate reportMonth;

    @Column(name = "prepared_by", nullable = false, length = 150)
    private String preparedBy;

    @Column(name = "plan_next_month", length = 2000)
    private String planNextMonth;

    @Column(name = "support_request", length = 2000)
    private String supportRequest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReportStatus status;

    @Column(name = "submitted_at")
    private Instant submittedAt;
}
