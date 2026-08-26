package vn.gpg.unionportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.gpg.unionportal.model.PulseSurvey;

import java.util.Optional;

public interface PulseSurveyRepository extends JpaRepository<PulseSurvey, Long>, JpaSpecificationExecutor<PulseSurvey> {
    Optional<PulseSurvey> findBySurveyCodeIgnoreCase(String surveyCode);
}
