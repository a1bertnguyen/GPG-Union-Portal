package vn.gpg.unionportal.model.kpi;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sla_rules")
@Getter
@Setter
@NoArgsConstructor
public class SlaRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sla_code", nullable = false, length = 60)
    private String slaCode;

    @Column(name = "version_id", nullable = false, length = 60)
    private String versionId;

    @Column(name = "case_type", nullable = false, length = 60)
    private String caseType;

    @Column(length = 40)
    private String priority;

    @Column(name = "duration_value", nullable = false)
    private int durationValue;

    @Column(name = "duration_unit", nullable = false, length = 30)
    private String durationUnit;

    @Column(name = "business_calendar_id", nullable = false, length = 60)
    private String businessCalendarId;
}
