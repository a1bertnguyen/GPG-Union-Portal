package vn.gpg.unionportal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "case_issue_groups")
@Getter
@Setter
@NoArgsConstructor
public class CaseIssueGroup extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @Column(nullable = false)
    private boolean active;
}
