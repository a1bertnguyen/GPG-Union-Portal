package vn.gpg.unionportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gpg.unionportal.model.PulseSurvey;

import java.util.Optional;

public interface PulseSurveyRepository extends JpaRepository<PulseSurvey, Long> {
    Optional<PulseSurvey> findBySurveyCodeIgnoreCase(String surveyCode);
}
