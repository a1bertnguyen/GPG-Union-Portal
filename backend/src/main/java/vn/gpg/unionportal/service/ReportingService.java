package vn.gpg.unionportal.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.exception.ResourceNotFoundException;
import vn.gpg.unionportal.model.*;
import vn.gpg.unionportal.model.DomainEnums.*;
import vn.gpg.unionportal.repository.*;
import vn.gpg.unionportal.dto.ApiModels.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.function.Predicate;

@Service
@Transactional(readOnly = true)
public class ReportingService {
    private final UnionUnitRepository unitRepository;
    private final MemberRepository memberRepository;
    private final WelfareRecordRepository welfareRepository;
    private final LaborCaseRepository caseRepository;
    private final UnionActivityRepository activityRepository;
    private final FinanceEntryRepository financeRepository;
    private final MonthlyReportRepository reportRepository;

    public ReportingService(UnionUnitRepository unitRepository, MemberRepository memberRepository,
                            WelfareRecordRepository welfareRepository, LaborCaseRepository caseRepository,
                            UnionActivityRepository activityRepository, FinanceEntryRepository financeRepository,
                            MonthlyReportRepository reportRepository) {
        this.unitRepository = unitRepository;
        this.memberRepository = memberRepository;
        this.welfareRepository = welfareRepository;
        this.caseRepository = caseRepository;
        this.activityRepository = activityRepository;
        this.financeRepository = financeRepository;
        this.reportRepository = reportRepository;
    }

    public DashboardSummary dashboard(YearMonth month) {
        return dashboard(month, null);
    }

    public DashboardSummary dashboard(YearMonth month, Long unitId) {
        if (unitId != null && !unitRepository.existsById(unitId)) {
            throw new ResourceNotFoundException("Không tìm thấy CĐCS với id=" + unitId);
        }
        Predicate<UnionUnit> unitFilter = candidate -> unitId == null || candidate.getId().equals(unitId);
        var members = memberRepository.findAll().stream().filter(m -> unitFilter.test(m.getUnionUnit())).toList();
        var welfare = welfareRepository.findAll().stream().filter(w -> unitFilter.test(w.getUnionUnit()))
                .filter(w -> inMonth(w.getEventDate(), month)).toList();
        var cases = caseRepository.findAll().stream().filter(c -> unitFilter.test(c.getUnionUnit())).toList();
        var entries = financeRepository.findAll().stream().filter(e -> unitFilter.test(e.getUnionUnit())).toList();
        var reports = reportRepository.findAll().stream().filter(r -> unitFilter.test(r.getUnionUnit()))
                .filter(r -> YearMonth.from(r.getReportMonth()).equals(month)).toList();

        long completedWelfare = welfare.stream().filter(w -> w.getStatus() == WorkStatus.COMPLETED).count();
        double completionRate = welfare.isEmpty() ? 100.0 : BigDecimal.valueOf(completedWelfare * 100.0 / welfare.size())
                .setScale(1, RoundingMode.HALF_UP).doubleValue();
        long openCases = cases.stream().filter(c -> c.getStatus() != CaseStatus.CLOSED).count();
        long overdueCases = cases.stream().filter(this::isOverdue).count();
        var monthFinance = financeSummary(entries.stream().filter(e -> inMonth(e.getTransactionDate(), month)).toList());
        var allFinance = financeSummary(entries);
        long submittedUnitCount = reports.stream().filter(r -> r.getStatus() != ReportStatus.DRAFT)
                .map(r -> r.getUnionUnit().getId()).distinct().count();
        long unitCount = unitId == null ? unitRepository.count() : 1;
        long pendingReports = Math.max(0, unitCount - submittedUnitCount);

        var alerts = new ArrayList<AlertItem>();
        if (overdueCases > 0) alerts.add(new AlertItem("danger", "Vụ việc quá hạn", overdueCases + " vụ việc cần cập nhật lý do/ETA"));
        long incompleteWelfare = welfare.stream().filter(w -> w.getDocumentStatus() == DocumentStatus.INCOMPLETE).count();
        if (incompleteWelfare > 0) alerts.add(new AlertItem("warning", "Hồ sơ chăm lo thiếu chứng từ", incompleteWelfare + " hồ sơ cần bổ sung"));
        if (monthFinance.incompleteDocuments() > 0) alerts.add(new AlertItem("warning", "Chứng từ tài chính chưa đủ", monthFinance.incompleteDocuments() + " giao dịch cần hoàn thiện"));
        if (pendingReports > 0) alerts.add(new AlertItem("info", "Báo cáo chưa nộp", pendingReports + " đơn vị chưa gửi báo cáo tháng"));

        return new DashboardSummary(
                unitCount,
                members.stream().filter(m -> m.getEmploymentStatus() == EmploymentStatus.ACTIVE).count(),
                members.stream().filter(m -> m.getMembershipStatus() == MembershipStatus.MEMBER).count(),
                completionRate,
                openCases,
                overdueCases,
                monthFinance.income(),
                monthFinance.expense(),
                allFinance.balance(),
                pendingReports,
                alerts);
    }

    public FinanceSummary financeSummary(YearMonth month, Long unitId) {
        return financeSummary(financeRepository.findAll().stream()
                .filter(e -> inMonth(e.getTransactionDate(), month))
                .filter(e -> unitId == null || e.getUnionUnit().getId().equals(unitId))
                .toList());
    }

    public MonthlySummary monthlySummary(YearMonth month, Long unitId) {
        UnionUnit unit = unitId == null ? null : unitRepository.findById(unitId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy CĐCS với id=" + unitId));
        Predicate<UnionUnit> unitFilter = candidate -> unitId == null || candidate.getId().equals(unitId);

        var members = memberRepository.findAll().stream().filter(m -> unitFilter.test(m.getUnionUnit())).toList();
        var welfare = welfareRepository.findAll().stream().filter(w -> unitFilter.test(w.getUnionUnit()))
                .filter(w -> inMonth(w.getEventDate(), month)).toList();
        var cases = caseRepository.findAll().stream().filter(c -> unitFilter.test(c.getUnionUnit()))
                .filter(c -> inMonth(c.getReceivedDate(), month)).toList();
        var activities = activityRepository.findAll().stream().filter(a -> unitFilter.test(a.getUnionUnit()))
                .filter(a -> inMonth(a.getEventDate(), month)).toList();
        var finances = financeRepository.findAll().stream().filter(e -> unitFilter.test(e.getUnionUnit()))
                .filter(e -> inMonth(e.getTransactionDate(), month)).toList();
        var finance = financeSummary(finances);
        MonthlyReport narrative = unitId == null ? null
                : reportRepository.findByUnionUnitIdAndReportMonth(unitId, month.atDay(1)).orElse(null);

        return new MonthlySummary(
                month.toString(), unitId, unit == null ? "Toàn hệ thống" : unit.getName(),
                members.stream().filter(m -> m.getEmploymentStatus() == EmploymentStatus.ACTIVE).count(),
                members.stream().filter(m -> m.getMembershipStatus() == MembershipStatus.MEMBER).count(),
                welfare.size(), welfare.stream().filter(w -> w.getStatus() == WorkStatus.COMPLETED).count(),
                cases.size(), cases.stream().filter(c -> c.getStatus() == CaseStatus.CLOSED).count(),
                activities.size(), activities.stream().mapToInt(UnionActivity::getParticipantCount).sum(),
                finance.income(), finance.expense(), finance.balance(), finance.incompleteDocuments(), narrative);
    }

    private FinanceSummary financeSummary(java.util.List<FinanceEntry> entries) {
        BigDecimal income = sum(entries, FinanceEntryType.INCOME);
        BigDecimal expense = sum(entries, FinanceEntryType.EXPENSE);
        long incomplete = entries.stream().filter(e -> e.getDocumentStatus() == DocumentStatus.INCOMPLETE).count();
        return new FinanceSummary(income, expense, income.subtract(expense), incomplete);
    }

    private BigDecimal sum(java.util.List<FinanceEntry> entries, FinanceEntryType type) {
        return entries.stream().filter(e -> e.getEntryType() == type).map(FinanceEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean inMonth(LocalDate date, YearMonth month) {
        return date != null && YearMonth.from(date).equals(month);
    }

    private boolean isOverdue(LaborCase laborCase) {
        return laborCase.getStatus() != CaseStatus.CLOSED && laborCase.getDeadline().isBefore(LocalDate.now());
    }
}
