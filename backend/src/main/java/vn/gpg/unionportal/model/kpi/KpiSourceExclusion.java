package vn.gpg.unionportal.model.kpi;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "kpi_source_exclusions")
@Getter
@Setter
@NoArgsConstructor
public class KpiSourceExclusion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_module", nullable = false, length = 50)
    private String sourceModule;

    @Column(name = "source_record_key", nullable = false, length = 160)
    private String sourceRecordKey;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(nullable = false)
    private boolean active;
}
