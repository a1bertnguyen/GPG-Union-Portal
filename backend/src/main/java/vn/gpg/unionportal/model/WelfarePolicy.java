package vn.gpg.unionportal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.gpg.unionportal.model.DomainEnums.WelfarePolicySource;
import vn.gpg.unionportal.model.DomainEnums.WelfareType;

import java.math.BigDecimal;

@Entity
@Table(name = "welfare_policies")
@Getter
@Setter
@NoArgsConstructor
public class WelfarePolicy extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WelfarePolicySource source;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "welfare_type", nullable = false, length = 30)
    private WelfareType welfareType;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(name = "support_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal supportAmount;

    @Column(name = "eligibility_notes", length = 1000)
    private String eligibilityNotes;

    @Column(name = "processing_weeks", nullable = false)
    private Integer processingWeeks;

    @Column(nullable = false)
    private Boolean active;
}
