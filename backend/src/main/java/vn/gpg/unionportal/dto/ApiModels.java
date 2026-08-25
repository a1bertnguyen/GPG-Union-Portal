package vn.gpg.unionportal.dto;

import jakarta.validation.constraints.*;
import vn.gpg.unionportal.model.DomainEnums.*;
import vn.gpg.unionportal.model.MonthlyReport;
import vn.gpg.unionportal.model.UnionUnit;
import vn.gpg.unionportal.model.IntegrationRun;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class ApiModels {
    private ApiModels() {
    }

    public record UnionUnitRequest(
            @NotBlank @Size(max = 30) String code,
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Size(max = 150) String companyName,
            @Size(max = 150) String location,
            @Size(max = 120) String chairperson,
            LocalDate termStart,
            LocalDate termEnd,
            @Size(max = 80) String decisionNumber,
            @NotNull LegalStatus legalStatus,
            @Size(max = 120) String contactPerson) {
    }

    public record MemberRequest(
            @NotBlank @Size(max = 40) String employeeCode,
            @NotBlank @Size(max = 150) String fullName,
            @NotNull Long unionUnitId,
            @Size(max = 120) String jobTitle,
            @Size(max = 150) String workplace,
            LocalDate joinDate,
            @NotNull MembershipStatus membershipStatus,
            @NotNull EmploymentStatus employmentStatus,
            @Email @Size(max = 150) String email,
            @Size(max = 30) String phone) {
    }

    public record WelfareRequest(
            @NotBlank @Size(max = 40) String recordCode,
            @NotNull WelfareType welfareType,
            @NotNull Long unionUnitId,
            @NotBlank @Size(max = 150) String beneficiaryName,
            @NotNull LocalDate eventDate,
            @NotNull WorkStatus status,
            @NotNull @DecimalMin("0.00") BigDecimal amount,
            @NotNull DocumentStatus documentStatus,
            @Size(max = 1000) String notes) {
    }

    public record LaborCaseRequest(
            @NotBlank @Size(max = 40) String caseCode,
            @NotNull LocalDate receivedDate,
            @NotNull Long unionUnitId,
            @NotBlank @Size(max = 120) String issueGroup,
            @NotNull CaseSeverity severity,
            @NotBlank @Size(max = 150) String ownerName,
            @NotNull LocalDate deadline,
            @NotNull CaseStatus status,
            @NotBlank @Size(max = 2000) String description,
            @NotNull @Min(1) Integer affectedPeople,
            @Size(max = 2000) String resultText,
            @Size(max = 1000) String overdueReason) {
    }

    public record ActivityRequest(
            @NotBlank @Size(max = 40) String activityCode,
            @NotBlank @Size(max = 200) String name,
            @NotNull Long unionUnitId,
            @NotNull LocalDate eventDate,
            @NotNull ActivityStatus status,
            @Size(max = 1000) String objective,
            @NotNull @DecimalMin("0.00") BigDecimal plannedBudget,
            @NotNull @DecimalMin("0.00") BigDecimal actualCost,
            @NotNull @Min(0) Integer participantCount,
            @DecimalMin("0.00") @DecimalMax("5.00") BigDecimal usefulnessScore,
            @NotNull Boolean reportCompleted,
            @Size(max = 150) String followUpOwner,
            LocalDate followUpDeadline) {
    }

    public record FinanceRequest(
            @NotBlank @Size(max = 40) String entryCode,
            @NotNull Long unionUnitId,
            @NotNull LocalDate transactionDate,
            @NotNull FinanceEntryType entryType,
            @NotBlank @Size(max = 120) String category,
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            @NotBlank @Size(max = 1000) String description,
            @Size(max = 80) String documentNumber,
            @NotNull DocumentStatus documentStatus) {
    }

    public record MonthlyReportRequest(
            @NotNull Long unionUnitId,
            @NotNull @Pattern(regexp = "\\d{4}-\\d{2}") String month,
            @NotBlank @Size(max = 150) String preparedBy,
            @Size(max = 2000) String planNextMonth,
            @Size(max = 2000) String supportRequest,
            @NotNull ReportStatus status) {
    }

    public record PulseSurveyRequest(
            @NotBlank @Size(max = 40) String surveyCode,
            @NotBlank @Size(max = 200) String title,
            @NotNull Long unionUnitId,
            @NotBlank @Size(max = 1000) String questionText,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            @NotNull SurveyStatus status,
            @NotNull @Min(1) Integer targetResponses) {
    }

    public record PulseSurveyResponseRequest(
            @NotNull @Min(1) @Max(5) Integer rating,
            @NotBlank @Size(max = 120) String needCategory,
            @Size(max = 2000) String suggestion,
            @NotNull Boolean anonymous,
            @Size(max = 150) String respondentName) {
    }

    public record AlertItem(String level, String title, String detail) {
    }

    public record DashboardSummary(
            long unitCount,
            long activeMemberCount,
            long unionMemberCount,
            double welfareCompletionRate,
            long openCaseCount,
            long overdueCaseCount,
            BigDecimal monthIncome,
            BigDecimal monthExpense,
            BigDecimal allTimeBalance,
            long pendingReportCount,
            List<AlertItem> alerts) {
    }

    public record FinanceSummary(BigDecimal income, BigDecimal expense, BigDecimal balance, long incompleteDocuments) {
    }

    public record MemberImportResult(
            int totalRows,
            int importedRows,
            int createdRows,
            int updatedRows,
            List<String> errors) {
    }

    public record IntegrationImportResult(
            IntegrationRun run,
            int createdRows,
            int updatedRows,
            List<String> errors) {
    }

    public record SpreadsheetImportResult(
            IntegrationRun run,
            String resource,
            int createdRows,
            int updatedRows,
            List<String> errors) {
    }

    public record PulseSurveyView(
            Long id,
            String surveyCode,
            String title,
            UnionUnit unionUnit,
            String questionText,
            LocalDate startDate,
            LocalDate endDate,
            SurveyStatus status,
            int targetResponses,
            long responseCount,
            double responseRate) {
    }

    public record NeedCount(String category, long count) {
    }

    public record EngagementSummary(
            String month,
            long activeSurveyCount,
            long totalSurveyCount,
            long totalResponses,
            double surveyResponseRate,
            double averageRating,
            double caseResponseRate,
            double averageActivityScore,
            List<NeedCount> topNeeds,
            List<AlertItem> alerts) {
    }

    public record MonthlySummary(
            String month,
            Long unionUnitId,
            String unionUnitName,
            long activeEmployees,
            long unionMembers,
            long welfareCases,
            long completedWelfareCases,
            long laborCases,
            long closedLaborCases,
            long activities,
            int participants,
            BigDecimal income,
            BigDecimal expense,
            BigDecimal netChange,
            long incompleteDocuments,
            MonthlyReport narrative) {
    }
}
