package vn.gpg.unionportal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.gpg.unionportal.model.DomainEnums.EmploymentStatus;
import vn.gpg.unionportal.model.DomainEnums.Gender;
import vn.gpg.unionportal.model.DomainEnums.MembershipStatus;

import java.time.LocalDate;

@Entity
@Table(name = "members")
@Getter
@Setter
@NoArgsConstructor
public class Member extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_code", nullable = false, unique = true, length = 40)
    private String employeeCode;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "union_unit_id", nullable = false)
    private UnionUnit unionUnit;

    @Column(name = "job_title", length = 120)
    private String jobTitle;

    @Column(length = 150)
    private String workplace;

    @Column(name = "join_date")
    private LocalDate joinDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_status", nullable = false, length = 30)
    private MembershipStatus membershipStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status", nullable = false, length = 30)
    private EmploymentStatus employmentStatus;

    @Column(length = 150)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(length = 150)
    private String company;

    @Column(name = "proposed_union_title", length = 120)
    private String proposedUnionTitle;

    @Column(name = "professional_title", length = 120)
    private String professionalTitle;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(length = 60)
    private String ethnicity;

    @Column(name = "place_of_birth", length = 150)
    private String placeOfBirth;

    @Column(name = "national_id", length = 20)
    private String nationalId;

    @Column(name = "party_member", nullable = false)
    private boolean partyMember;

    @Column(length = 100)
    private String education;

    @Column(length = 150)
    private String specialization;

    @Column(name = "political_theory", length = 100)
    private String politicalTheory;

    @Column(name = "foreign_language", length = 100)
    private String foreignLanguage;

    @Column(name = "start_work_date")
    private LocalDate startWorkDate;

    @Column(name = "current_residence", length = 200)
    private String currentResidence;
}
