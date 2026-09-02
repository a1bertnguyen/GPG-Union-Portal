package vn.gpg.unionportal.repository.kpi;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gpg.unionportal.model.kpi.KpiClassificationGate;

import java.util.List;

public interface KpiClassificationGateRepository extends JpaRepository<KpiClassificationGate, Long> {
    List<KpiClassificationGate> findByVersionId(String versionId);
}
