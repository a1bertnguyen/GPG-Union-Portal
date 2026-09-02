package vn.gpg.unionportal.repository.kpi;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gpg.unionportal.model.kpi.KpiSourceExclusion;

import java.util.List;

public interface KpiSourceExclusionRepository extends JpaRepository<KpiSourceExclusion, Long> {
    List<KpiSourceExclusion> findByActiveTrue();
}
