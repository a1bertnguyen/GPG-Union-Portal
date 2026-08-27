package vn.gpg.unionportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.gpg.unionportal.model.MonthlyReport;

import java.time.LocalDate;
import java.util.Optional;

public interface MonthlyReportRepository extends JpaRepository<MonthlyReport, Long>, JpaSpecificationExecutor<MonthlyReport> {
    Optional<MonthlyReport> findByUnionUnitIdAndReportMonth(Long unionUnitId, LocalDate reportMonth);
}
