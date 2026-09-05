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

import java.time.LocalDate;

/** Quality warnings as they stood when the period was locked. */
@Entity
@Table(name = "kpi_run_warnings")
@Getter
@Setter
@NoArgsConstructor
public class KpiRunWarning {
    @Column(name = "due_at")
    private LocalDate dueAt;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", nullable = false)
    private Long runId;

    @Column(name = "warning_code", nullable = false, length = 60)
    private String warningCode;

    @Column(nullable = false, length = 20)
    private String severity;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(name = "recommended_action", length = 1000)
    private String recommendedAction;

    @Column(name = "source_module", length = 50)
    private String sourceModule;

    @Column(name = "source_record_id", length = 100)
    private String sourceRecordId;

    @Column(nullable = false)
    private boolean redacted;
}
