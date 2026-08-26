package vn.gpg.unionportal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.gpg.unionportal.model.DomainEnums.ActivityStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "union_activities")
@Getter
@Setter
@NoArgsConstructor
public class UnionActivity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "activity_code", nullable = false, unique = true, length = 40)
    private String activityCode;

    @Column(nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "union_unit_id", nullable = false)
    private UnionUnit unionUnit;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ActivityStatus status;

    @Column(length = 1000)
    private String objective;

    @Column(name = "planned_budget", nullable = false, precision = 15, scale = 2)
    private BigDecimal plannedBudget;

    @Column(name = "actual_cost", nullable = false, precision = 15, scale = 2)
    private BigDecimal actualCost;

    @Column(name = "participant_count", nullable = false)
    private Integer participantCount;

    @Column(name = "participant_list", length = 2000)
    private String participantList;

    @Column(name = "check_in_count", nullable = false)
    private Integer checkInCount;

    @Column(name = "usefulness_score", precision = 3, scale = 2)
    private BigDecimal usefulnessScore;

    @Column(name = "quick_feedback", length = 2000)
    private String quickFeedback;

    @Column(length = 2000)
    private String issues;

    @Column(name = "report_completed", nullable = false)
    private Boolean reportCompleted;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_status", nullable = false, length = 30)
    private vn.gpg.unionportal.model.DomainEnums.DocumentStatus documentStatus;

    @Column(name = "follow_up_owner", length = 150)
    private String followUpOwner;

    @Column(name = "follow_up_deadline")
    private LocalDate followUpDeadline;

    @Column(name = "lessons_learned", length = 2000)
    private String lessonsLearned;
}
