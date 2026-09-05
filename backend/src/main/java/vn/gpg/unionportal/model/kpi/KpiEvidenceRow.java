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

/**
 * Which source records a locked KPI counted. One row per record per role, so a lock writes a lot of rows on
 * a large unit; that is the price of being able to prove a past score against the records it came from.
 */
@Entity
@Table(name = "kpi_evidence")
@Getter
@Setter
@NoArgsConstructor
public class KpiEvidenceRow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "result_id", nullable = false)
    private Long resultId;

    @Column(name = "source_module", nullable = false, length = 50)
    private String sourceModule;

    @Column(name = "source_record_id", nullable = false, length = 100)
    private String sourceRecordId;

    @Column(name = "evidence_role", nullable = false, length = 30)
    private String evidenceRole;

    @Column(name = "evidence_url", length = 1000)
    private String evidenceUrl;

    @Column(name = "validation_status", nullable = false, length = 30)
    private String validationStatus;

    @Column(nullable = false)
    private boolean redacted;
}
