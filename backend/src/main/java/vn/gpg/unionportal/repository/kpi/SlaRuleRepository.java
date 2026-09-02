package vn.gpg.unionportal.repository.kpi;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gpg.unionportal.model.kpi.SlaRule;

import java.util.List;

public interface SlaRuleRepository extends JpaRepository<SlaRule, Long> {
    List<SlaRule> findByVersionId(String versionId);
}
