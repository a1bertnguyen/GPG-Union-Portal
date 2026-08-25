package vn.gpg.unionportal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.gpg.unionportal.model.DomainEnums.IntegrationStatus;
import vn.gpg.unionportal.model.DomainEnums.IntegrationType;

import java.time.Instant;

@Entity
@Table(name = "integration_runs")
@Getter
@Setter
@NoArgsConstructor
public class IntegrationRun extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "integration_type", nullable = false, length = 30)
    private IntegrationType integrationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IntegrationStatus status;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "total_rows", nullable = false)
    private Integer totalRows;

    @Column(name = "successful_rows", nullable = false)
    private Integer successfulRows;

    @Column(name = "failed_rows", nullable = false)
    private Integer failedRows;

    @Column(name = "started_by", nullable = false, length = 80)
    private String startedBy;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    @Column(name = "error_summary", length = 4000)
    private String errorSummary;
}
