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

/**
 * The per-KPI numbers of a locked run. This is the snapshot the history reads: KPIs whose truth is
 * "state at the end of the period" cannot be recomputed later, so the stored row is the only record.
 */
@Entity
@Table(name = "kpi_result_details")
@Getter
@Setter
@NoArgsConstructor
public class KpiResultDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", nullable = false)
    private Long runId;

    @Column(name = "kpi_code", nullable = false, length = 20)
    private String kpiCode;

    @Column(precision = 18, scale = 6)
    private BigDecimal numerator;

    @Column(precision = 18, scale = 6)
    private BigDecimal denominator;

    @Column(name = "target_value", precision = 12, scale = 6)
    private BigDecimal targetValue;

    @Column(name = "normalized_score", precision = 12, scale = 8)
    private BigDecimal normalizedScore;

    @Column(name = "eligible_weight", nullable = false, precision = 8, scale = 4)
    private BigDecimal eligibleWeight;

    @Column(name = "earned_points", nullable = false, precision = 12, scale = 8)
    private BigDecimal earnedPoints;

    @Column(name = "result_status", nullable = false, length = 30)
    private String resultStatus;

    @Column(nullable = false, length = 2000)
    private String explanation;
}
