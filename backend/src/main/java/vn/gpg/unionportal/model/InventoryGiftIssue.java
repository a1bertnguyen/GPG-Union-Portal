package vn.gpg.unionportal.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * A gift-issue slip. Recipient attributes are copied from {@link Member} at issue time so later
 * profile edits do not rewrite a historical slip.
 */
@Entity
@Table(name = "inventory_gift_issues")
@Getter
@Setter
@NoArgsConstructor
public class InventoryGiftIssue extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "union_unit_id", nullable = false)
    private UnionUnit unionUnit;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem item;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "program_name", length = 200)
    private String programName;

    @Column(name = "reference_no", length = 80)
    private String referenceNo;

    @Column(length = 1000)
    private String note;

    @Column(name = "employee_code_snapshot", nullable = false, length = 40)
    private String employeeCodeSnapshot;

    @Column(name = "recipient_name_snapshot", nullable = false, length = 150)
    private String recipientNameSnapshot;

    @Column(name = "company_name_snapshot", nullable = false, length = 150)
    private String companyNameSnapshot;

    @Column(name = "job_title_snapshot", length = 120)
    private String jobTitleSnapshot;

    @Column(name = "professional_title_snapshot", length = 120)
    private String professionalTitleSnapshot;

    @Column(name = "workplace_snapshot", length = 150)
    private String workplaceSnapshot;

    @Column(name = "email_snapshot", length = 150)
    private String emailSnapshot;

    @Column(name = "phone_snapshot", length = 30)
    private String phoneSnapshot;

    @Column(name = "gender_snapshot", length = 10)
    private String genderSnapshot;

    @Column(name = "place_of_birth_snapshot", length = 150)
    private String placeOfBirthSnapshot;

    @Column(name = "current_residence_snapshot", length = 200)
    private String currentResidenceSnapshot;

    @Column(name = "start_work_date_snapshot")
    private LocalDate startWorkDateSnapshot;
}
