package vn.gpg.unionportal.repository.kpi;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gpg.unionportal.model.kpi.KpiAdjustment;

import java.time.LocalDate;
import java.util.List;

public interface KpiAdjustmentRepository extends JpaRepository<KpiAdjustment, Long> {
    List<KpiAdjustment> findByUnionUnitIdAndPeriodTypeAndPeriodStartAndPeriodEndAndVersionIdAndApprovedByIsNotNullAndApprovedAtIsNotNull(
            Long unionUnitId, String periodType, LocalDate periodStart, LocalDate periodEnd, String versionId);
}
