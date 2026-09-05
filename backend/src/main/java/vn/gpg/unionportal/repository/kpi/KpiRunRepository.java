package vn.gpg.unionportal.repository.kpi;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.gpg.unionportal.model.kpi.KpiRun;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface KpiRunRepository extends JpaRepository<KpiRun, Long>, JpaSpecificationExecutor<KpiRun> {
    Optional<KpiRun> findFirstByUnionUnitIdAndPeriodTypeAndPeriodStartAndPeriodEndAndVersionIdOrderByRevisionDesc(
            Long unionUnitId, String periodType, LocalDate periodStart, LocalDate periodEnd, String versionId);

    List<KpiRun> findByPeriodTypeAndPeriodStartAndPeriodEndAndVersionId(
            String periodType, LocalDate periodStart, LocalDate periodEnd, String versionId);
}
