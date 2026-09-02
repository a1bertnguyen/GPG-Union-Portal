package vn.gpg.unionportal.model.kpi;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;

@Entity
@Table(name = "kpi_versions")
@Getter
@Setter
@NoArgsConstructor
public class KpiVersion {
    @Id
    @Column(name = "version_id", length = 60)
    private String versionId;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "score_scale", nullable = false, precision = 8, scale = 4)
    private BigDecimal scoreScale;

    @Column(name = "round_display", nullable = false)
    private int roundDisplay;

    @Column(name = "bonus_cap", nullable = false, precision = 8, scale = 4)
    private BigDecimal bonusCap;

    @Column(name = "data_quality_final_threshold", nullable = false, precision = 8, scale = 6)
    private BigDecimal dataQualityFinalThreshold;

    @Column(name = "created_by", nullable = false, length = 150)
    private String createdBy;

    @Column(name = "approved_by", length = 150)
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;
}
