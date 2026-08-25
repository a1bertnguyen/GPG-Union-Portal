package vn.gpg.unionportal.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.gpg.unionportal.model.DomainEnums.LegalStatus;

import java.time.LocalDate;

@Entity
@Table(name = "union_units")
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class UnionUnit extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "company_name", nullable = false, length = 150)
    private String companyName;

    @Column(length = 150)
    private String location;

    @Column(length = 120)
    private String chairperson;

    @Column(name = "term_start")
    private LocalDate termStart;

    @Column(name = "term_end")
    private LocalDate termEnd;

    @Column(name = "decision_number", length = 80)
    private String decisionNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "legal_status", nullable = false, length = 40)
    private LegalStatus legalStatus;

    @Column(name = "contact_person", length = 120)
    private String contactPerson;
}
