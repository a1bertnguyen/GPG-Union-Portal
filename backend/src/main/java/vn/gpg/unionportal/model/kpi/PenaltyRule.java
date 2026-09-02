package vn.gpg.unionportal.model.kpi;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "penalty_rules")
@Getter
@Setter
@NoArgsConstructor
public class PenaltyRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "penalty_code", nullable = false, length = 20)
    private String penaltyCode;

    @Column(name = "version_id", nullable = false, length = 60)
    private String versionId;

    @Column(name = "points_per_case", nullable = false, precision = 8, scale = 4)
    private BigDecimal pointsPerCase;

    @Column(name = "period_cap", precision = 8, scale = 4)
    private BigDecimal periodCap;

    @Column(name = "classification_cap", length = 40)
    private String classificationCap;

    @Column(name = "detection_rule", nullable = false, length = 1000)
    private String detectionRule;
}
