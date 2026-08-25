package vn.gpg.unionportal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "pulse_survey_responses")
@Getter
@Setter
@NoArgsConstructor
public class PulseSurveyResponse extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "survey_id", nullable = false)
    private PulseSurvey survey;

    @Column(nullable = false)
    private Integer rating;

    @Column(name = "need_category", nullable = false, length = 120)
    private String needCategory;

    @Column(length = 2000)
    private String suggestion;

    @Column(nullable = false)
    private Boolean anonymous;

    @Column(name = "respondent_name", length = 150)
    private String respondentName;

    @Column(name = "submitted_on", nullable = false)
    private LocalDate submittedOn;
}
