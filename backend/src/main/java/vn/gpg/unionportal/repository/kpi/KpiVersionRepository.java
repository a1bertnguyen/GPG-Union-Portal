package vn.gpg.unionportal.repository.kpi;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gpg.unionportal.model.kpi.KpiVersion;

public interface KpiVersionRepository extends JpaRepository<KpiVersion, String> {
}
