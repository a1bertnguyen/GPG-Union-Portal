package vn.gpg.unionportal.repository.kpi;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gpg.unionportal.model.kpi.KpiNoOccurrenceConfirmation;

import java.time.LocalDate;
import java.util.List;

public interface KpiNoOccurrenceConfirmationRepository extends JpaRepository<KpiNoOccurrenceConfirmation, Long> {
    List<KpiNoOccurrenceConfirmation> findByUnionUnitIdAndVersionIdAndPeriodStartAndPeriodEndAndReconciledTrueAndApprovedByIsNotNullAndApprovedAtIsNotNull(
            Long unionUnitId, String versionId, LocalDate periodStart, LocalDate periodEnd);
}
