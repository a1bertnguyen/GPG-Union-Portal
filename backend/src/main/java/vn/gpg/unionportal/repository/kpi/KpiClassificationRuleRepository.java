package vn.gpg.unionportal.repository.kpi;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gpg.unionportal.model.kpi.KpiClassificationRule;

import java.util.List;

public interface KpiClassificationRuleRepository extends JpaRepository<KpiClassificationRule, Long> {
    List<KpiClassificationRule> findByVersionIdOrderByMinimumScoreDesc(String versionId);
}
