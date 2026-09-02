package vn.gpg.unionportal.model.kpi;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "kpi_no_occurrence_confirmations")
@Getter
@Setter
@NoArgsConstructor
public class KpiNoOccurrenceConfirmation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "union_unit_id", nullable = false)
    private Long unionUnitId;

    @Column(name = "version_id", nullable = false, length = 60)
    private String versionId;

    @Column(name = "kpi_code", nullable = false, length = 20)
    private String kpiCode;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "source_module", nullable = false, length = 50)
    private String sourceModule;

    @Column(name = "reconciliation_source_module", nullable = false, length = 50)
    private String reconciliationSourceModule;

    @Column(nullable = false)
    private boolean reconciled;

    @Column(name = "confirmed_by", nullable = false, length = 150)
    private String confirmedBy;

    @Column(name = "approved_by", length = 150)
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;
}
