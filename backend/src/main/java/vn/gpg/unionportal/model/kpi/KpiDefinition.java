package vn.gpg.unionportal.model.kpi;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "kpi_definitions")
@Getter
@Setter
@NoArgsConstructor
public class KpiDefinition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "version_id", nullable = false, length = 60)
    private String versionId;

    @Column(name = "kpi_code", nullable = false, length = 20)
    private String kpiCode;

    @Column(name = "group_code", nullable = false, length = 20)
    private String groupCode;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, precision = 8, scale = 4)
    private BigDecimal weight;

    @Column(nullable = false, length = 30)
    private String direction;

    @Column(name = "target_value", precision = 12, scale = 6)
    private BigDecimal targetValue;

    @Column(name = "max_allowed_value", precision = 12, scale = 6)
    private BigDecimal maxAllowedValue;

    @Column(nullable = false)
    private boolean mandatory;

    @Column(name = "na_allowed", nullable = false)
    private boolean naAllowed;

    @Column(name = "source_module", nullable = false, length = 50)
    private String sourceModule;

    @Column(name = "numerator_rule", nullable = false, length = 1000)
    private String numeratorRule;

    @Column(name = "denominator_rule", nullable = false, length = 1000)
    private String denominatorRule;

    @Column(name = "evidence_rule", nullable = false, length = 1000)
    private String evidenceRule;
}
