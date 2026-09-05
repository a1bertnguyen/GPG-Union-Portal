package vn.gpg.unionportal.model.kpi;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * One locked KPI evaluation for one unit and one period. {@code GET /api/kpi} never writes here: only an
 * explicit period lock does, which is what makes a {@code FINAL} ranking and a comparable history possible.
 */
@Entity
@Table(name = "kpi_runs")
@Getter
@Setter
@NoArgsConstructor
public class KpiRun {
    @Column(name = "unit_code_snapshot")
    private String unitCodeSnapshot;
    @Column(name = "unit_name_snapshot")
    private String unitNameSnapshot;
    @Column(name = "population_snapshot_id")
    private Long populationSnapshotId;
    @Column(name = "active_employee_count")
    private Long activeEmployeeCount;
    @Column(name = "active_union_member_count")
    private Long activeUnionMemberCount;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_key", nullable = false, unique = true, length = 160)
    private String runKey;

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

    @Column(nullable = false)
    private int revision;

    @Column(name = "cutoff_at", nullable = false)
    private Instant cutoffAt;

    @Column(name = "run_status", nullable = false, length = 30)
    private String runStatus;

    @Column(name = "data_quality_rate", nullable = false, precision = 12, scale = 8)
    private BigDecimal dataQualityRate;

    @Column(name = "base_score", nullable = false, precision = 12, scale = 8)
    private BigDecimal baseScore;

    @Column(name = "bonus_points", nullable = false, precision = 12, scale = 8)
    private BigDecimal bonusPoints;

    @Column(name = "penalty_points", nullable = false, precision = 12, scale = 8)
    private BigDecimal penaltyPoints;

    @Column(name = "final_score", nullable = false, precision = 12, scale = 8)
    private BigDecimal finalScore;

    @Column(name = "raw_classification", nullable = false, length = 40)
    private String rawClassification;

    @Column(name = "final_classification", nullable = false, length = 40)
    private String finalClassification;

    @Column(name = "ranking_position")
    private Integer rankingPosition;

    /** Fingerprint of the source rows behind the run: an unchanged hash means re-locking is a no-op. */
    @Column(name = "input_hash", nullable = false, length = 64)
    private String inputHash;

    @Column(name = "previous_run_id")
    private Long previousRunId;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    @Column(name = "calculated_by", nullable = false, length = 150)
    private String calculatedBy;
}
