package vn.gpg.unionportal.model.kpi;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "kpi_adjustments")
@Getter
@Setter
@NoArgsConstructor
public class KpiAdjustment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "union_unit_id", nullable = false)
    private Long unionUnitId;

    @Column(name = "period_type", nullable = false, length = 30)
    private String periodType;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "version_id", nullable = false, length = 60)
    private String versionId;

    @Column(name = "adjustment_type", nullable = false, length = 30)
    private String adjustmentType;

    @Column(name = "penalty_code", length = 20)
    private String penaltyCode;

    @Column(nullable = false, precision = 8, scale = 4)
    private BigDecimal points;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Column(name = "evidence_module", length = 50)
    private String evidenceModule;

    @Column(name = "evidence_record_id", length = 100)
    private String evidenceRecordId;

    @Column(name = "effectiveness_verified", nullable = false)
    private boolean effectivenessVerified;

    @Column(name = "non_duplicate_verified", nullable = false)
    private boolean nonDuplicateVerified;

    @Column(name = "requested_by", nullable = false, length = 150)
    private String requestedBy;

    @Column(name = "approved_by", length = 150)
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;
}
