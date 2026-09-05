package vn.gpg.unionportal.repository.kpi;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gpg.unionportal.model.kpi.KpiEvidenceRow;

import java.util.Collection;
import java.util.List;

public interface KpiEvidenceRowRepository extends JpaRepository<KpiEvidenceRow, Long> {
    List<KpiEvidenceRow> findByResultIdIn(Collection<Long> resultIds);
}
