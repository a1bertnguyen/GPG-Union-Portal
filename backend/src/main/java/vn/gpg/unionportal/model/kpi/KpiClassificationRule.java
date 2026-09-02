package vn.gpg.unionportal.model.kpi;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "kpi_classification_rules")
@Getter
@Setter
@NoArgsConstructor
public class KpiClassificationRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "version_id", nullable = false, length = 60)
    private String versionId;

    @Column(name = "minimum_score", nullable = false, precision = 8, scale = 4)
    private BigDecimal minimumScore;

    @Column(nullable = false, length = 40)
    private String label;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
