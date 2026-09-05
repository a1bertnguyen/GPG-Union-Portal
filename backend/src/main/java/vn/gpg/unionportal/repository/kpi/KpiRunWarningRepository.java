package vn.gpg.unionportal.repository.kpi;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gpg.unionportal.model.kpi.KpiRunWarning;

import java.util.List;

public interface KpiRunWarningRepository extends JpaRepository<KpiRunWarning, Long> {
    List<KpiRunWarning> findByRunId(Long runId);
}
