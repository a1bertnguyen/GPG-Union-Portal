package vn.gpg.unionportal.model.kpi;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "kpi_classification_gates")
@Getter
@Setter
@NoArgsConstructor
public class KpiClassificationGate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "version_id", nullable = false, length = 60)
    private String versionId;

    @Column(name = "gate_code", nullable = false, length = 60)
    private String gateCode;

    @Column(name = "classification_cap", nullable = false, length = 40)
    private String classificationCap;

    @Column(name = "detection_rule", nullable = false, length = 1000)
    private String detectionRule;
}
