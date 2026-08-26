package vn.gpg.unionportal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "member_changes")
@Getter
@Setter
@NoArgsConstructor
public class MemberChange extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "change_type", nullable = false, length = 120)
    private String changeType;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(name = "recorded_by", nullable = false, length = 150)
    private String recordedBy;
}
