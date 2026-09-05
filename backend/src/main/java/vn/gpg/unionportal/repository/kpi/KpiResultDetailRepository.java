package vn.gpg.unionportal.repository.kpi;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gpg.unionportal.model.kpi.KpiResultDetail;

import java.util.Collection;
import java.util.List;

public interface KpiResultDetailRepository extends JpaRepository<KpiResultDetail, Long> {
    List<KpiResultDetail> findByRunIdOrderByKpiCode(Long runId);

    List<KpiResultDetail> findByRunIdInOrderByKpiCode(Collection<Long> runIds);
}
