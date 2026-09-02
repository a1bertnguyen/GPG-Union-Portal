package vn.gpg.unionportal.repository.kpi;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gpg.unionportal.model.kpi.KpiDefinition;

import java.util.List;

public interface KpiDefinitionRepository extends JpaRepository<KpiDefinition, Long> {
    List<KpiDefinition> findByVersionIdOrderById(String versionId);
}
