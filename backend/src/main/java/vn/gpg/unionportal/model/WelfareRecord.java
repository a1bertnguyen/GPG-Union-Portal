package vn.gpg.unionportal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.gpg.unionportal.model.DomainEnums.DocumentStatus;
import vn.gpg.unionportal.model.DomainEnums.WelfareType;
import vn.gpg.unionportal.model.DomainEnums.WorkStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "welfare_records")
@Getter
@Setter
@NoArgsConstructor
public class WelfareRecord extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "record_code", nullable = false, unique = true, length = 40)
    private String recordCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "welfare_type", nullable = false, length = 30)
    private WelfareType welfareType;

    @Column(name = "policy_name", length = 180)
    private String policyName;

    @Column(name = "policy_id")
    private Long policyId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "union_unit_id", nullable = false)
    private UnionUnit unionUnit;

    @Column(name = "beneficiary_name", nullable = false, length = 150)
    private String beneficiaryName;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column
    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WorkStatus status;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "standard_amount", precision = 15, scale = 2)
    private BigDecimal standardAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_status", nullable = false, length = 30)
    private DocumentStatus documentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "receipt_status", nullable = false, length = 30)
    private DocumentStatus receiptStatus;

    @Column(name = "has_image", nullable = false)
    private Boolean hasImage;

    @Column(length = 1000)
    private String notes;
}
