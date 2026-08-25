package vn.gpg.unionportal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.gpg.unionportal.model.DomainEnums.CaseSeverity;
import vn.gpg.unionportal.model.DomainEnums.CaseStatus;

import java.time.LocalDate;

@Entity
@Table(name = "labor_cases")
@Getter
@Setter
@NoArgsConstructor
public class LaborCase extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_code", nullable = false, unique = true, length = 40)
    private String caseCode;

    @Column(name = "received_date", nullable = false)
    private LocalDate receivedDate;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "union_unit_id", nullable = false)
    private UnionUnit unionUnit;

    @Column(name = "issue_group", nullable = false, length = 120)
    private String issueGroup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CaseSeverity severity;

    @Column(name = "owner_name", nullable = false, length = 150)
    private String ownerName;

    @Column(nullable = false)
    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CaseStatus status;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(name = "affected_people", nullable = false)
    private Integer affectedPeople;

    @Column(name = "result_text", length = 2000)
    private String resultText;

    @Column(name = "overdue_reason", length = 1000)
    private String overdueReason;
}
