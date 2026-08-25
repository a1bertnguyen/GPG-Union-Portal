package vn.gpg.unionportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gpg.unionportal.model.PulseSurveyResponse;

import java.util.List;

public interface PulseSurveyResponseRepository extends JpaRepository<PulseSurveyResponse, Long> {
    long countBySurveyId(Long surveyId);
    List<PulseSurveyResponse> findBySurveyIdOrderBySubmittedOnDesc(Long surveyId);
}
