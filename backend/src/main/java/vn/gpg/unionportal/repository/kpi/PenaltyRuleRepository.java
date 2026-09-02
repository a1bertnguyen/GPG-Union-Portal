package vn.gpg.unionportal.repository.kpi;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gpg.unionportal.model.kpi.PenaltyRule;

import java.util.List;

public interface PenaltyRuleRepository extends JpaRepository<PenaltyRule, Long> {
    List<PenaltyRule> findByVersionId(String versionId);
}
