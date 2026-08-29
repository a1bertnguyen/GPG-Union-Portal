package vn.gpg.unionportal.dto;

import jakarta.validation.constraints.*;
import org.springframework.data.domain.Page;
import vn.gpg.unionportal.model.DomainEnums.*;
import vn.gpg.unionportal.model.MonthlyReport;
import vn.gpg.unionportal.model.UnionUnit;
import vn.gpg.unionportal.model.IntegrationRun;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class ApiModels {
    private ApiModels() {
    }

    /** Envelope returned by every list endpoint so the client always reads the same shape. */
    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
        public static <T> PageResponse<T> of(Page<T> page) {
            return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                    page.getTotalElements(), page.getTotalPages());
        }

        /** Single-page envelope for {@code all=true} callers such as dropdown lookups. */
        public static <T> PageResponse<T> ofAll(List<T> content) {
            return new PageResponse<>(content, 0, content.size(), content.size(), content.isEmpty() ? 0 : 1);
        }

        /**
         * Picks the paged or the unpaged path based on {@code all}, so each controller states the
         * two service methods once instead of repeating the branch.
         */
        public static <T> PageResponse<T> from(ListQuery query,
                                               java.util.function.Function<ListQuery, Page<T>> paged,
                                               java.util.function.Function<ListQuery, List<T>> all) {
            return query.fetchAll() ? ofAll(all.apply(query)) : of(paged.apply(query));
        }

        public <R> PageResponse<R> map(java.util.function.Function<T, R> mapper) {
            return new PageResponse<>(content.stream().map(mapper).toList(), page, size, totalElements, totalPages);
        }
    }

    /**
     * Whole-dataset numbers for a filtered list. The metric cards and the status dropdown must stay
     * accurate across every page, so they cannot be derived from the page slice.
     *
     * <p>The three fields have deliberately different scopes:
     * <ul>
     *   <li>{@code total} — grand total for the caller's CĐCS scope, ignoring the search box and the
     *       filter dropdowns. Feeds the "trên tổng N" hint; {@code PageResponse.totalElements} is the
     *       filtered count.</li>
     *   <li>{@code statusValues} — distinct status values in that same unfiltered scope, so the
     *       dropdown does not shrink as the user narrows the list.</li>
     *   <li>{@code metrics} — computed over the <em>filtered</em> set. Raw numbers only; labels, tones
     *       and money formatting stay in the frontend.</li>
     * </ul>
     */
    public record ListFacets(long total, List<String> statusValues, Map<String, Number> metrics) {
        public static ListFacets empty() {
            return new ListFacets(0, List.of(), Map.of());
        }
    }

    /** Per-issue-group rollup used by the case analytics bars, which group over the whole dataset. */
    public record CaseGroupCount(String issueGroup, long count, long affectedPeople, long overdue) {
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
            @Size(max = 30) String phone,
            @Size(max = 150) String company,
            @Size(max = 120) String proposedUnionTitle,
            @Size(max = 120) String professionalTitle,
            Gender gender,
            @Size(max = 60) String ethnicity,
            @Size(max = 150) String placeOfBirth,
            @Size(max = 20) String nationalId,
            boolean partyMember,
            @Size(max = 100) String education,
            @Size(max = 150) String specialization,
            @Size(max = 100) String politicalTheory,
            @Size(max = 100) String foreignLanguage,
            LocalDate startWorkDate,
            @Size(max = 200) String currentResidence) {
    }

    public record WelfareRequest(
            @NotBlank @Size(max = 40) String recordCode,
            @NotNull WelfareType welfareType,
            @Size(max = 180) String policyName,
            @NotNull Long unionUnitId,
            @NotBlank @Size(max = 150) String beneficiaryName,
            @NotNull LocalDate eventDate,
            LocalDate deadline,
            @NotNull WorkStatus status,
            @NotNull @DecimalMin("0.00") BigDecimal amount,
            @DecimalMin("0.00") BigDecimal standardAmount,
            @NotNull DocumentStatus documentStatus,
            @NotNull DocumentStatus receiptStatus,
            @NotNull Boolean hasImage,
            @Size(max = 1000) String notes,
            Long policyId) {
    }

    public record WelfarePolicyRequest(
            @NotBlank @Size(max = 40) String code,
            @NotNull WelfarePolicySource source,
            @NotNull @Min(1) Integer sequenceNumber,
            @NotNull WelfareType welfareType,
            @NotBlank @Size(max = 180) String name,
            @NotNull @DecimalMin("0.00") BigDecimal supportAmount,
            @Size(max = 1000) String eligibilityNotes,
            @NotNull @Min(1) @Max(8) Integer processingWeeks,
            @NotNull Boolean active) {
    }

    public record LaborCaseRequest(
            @NotBlank @Size(max = 40) String caseCode,
            @NotNull LocalDate receivedDate,
            @NotNull Long unionUnitId,
            @NotBlank @Size(max = 150) String requesterName,
            @Size(max = 40) String employeeCode,
            @Size(max = 150) String jobTitle,
            @Size(max = 200) String workplace,
            LocalDate startWorkDate,
            LocalDate leaveDate,
            @Size(max = 30) String phone,
            @Size(max = 120) String source,
            @NotBlank @Size(max = 120) String issueGroup,
            @NotNull CaseSeverity severity,
            @Size(max = 150) String ownerName,
            LocalDate deadline,
            @NotNull CaseStatus status,
            @NotBlank @Size(max = 2000) String description,
            @NotNull @Min(1) Integer affectedPeople,
            @Size(max = 500) String attachmentNote,
            @Size(max = 2000) String resultText,
            LocalDate responseDate,
            @Size(max = 1000) String overdueReason) {
    }

    public record CaseApprovalRequest(
            @NotBlank @Size(max = 150) String ownerName,
            @NotNull LocalDate deadline) {
    }

    public record ActivityRequest(
            @NotBlank @Size(max = 40) String activityCode,
            @NotBlank @Size(max = 200) String name,
            @NotNull Long unionUnitId,
            @NotNull LocalDate eventDate,
            LocalTime eventTime,
            @Size(max = 300) String location,
            @Size(max = 150) String programPic,
            @NotNull ActivityStatus status,
            @Size(max = 1000) String objective,
            @NotNull @DecimalMin("0.00") BigDecimal plannedBudget,
            @NotNull @DecimalMin("0.00") BigDecimal actualCost,
            @NotNull @Min(0) Integer invitedCount,
            @NotNull @Min(0) Integer participantCount,
            @Size(max = 2000) String participantList,
            @Size(max = 500) String employeeGroup,
            @NotNull @Min(0) Integer checkInCount,
            @Size(max = 3000) String actualContent,
            @Size(max = 2000) String planDifference,
            @NotNull @Min(0) Integer workersReached,
            @DecimalMin("0.00") @DecimalMax("5.00") BigDecimal usefulnessScore,
            @Size(max = 2000) String quickFeedback,
            @Size(max = 2000) String issues,
            @Size(max = 2000) String outputProposal,
            @Size(max = 2000) String communicationContent,
            @Size(max = 2000) String strengths,
            @Size(max = 2000) String weaknesses,
            @NotNull Boolean reportCompleted,
            @NotNull DocumentStatus documentStatus,
            @Size(max = 2000) String followUpIssue,
            @Size(max = 150) String followUpOwner,
            LocalDate followUpDeadline,
            @Size(max = 60) String followUpStatus,
            @Size(max = 2000) String lessonsLearned) {
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

    public record FinanceSummary(BigDecimal income, BigDecimal expense, BigDecimal advance,
                                 BigDecimal balance, long incompleteDocuments) {
    }

    public record MemberChangeRequest(
            @NotNull Long memberId,
            @NotBlank @Size(max = 120) String changeType,
            @NotNull LocalDate effectiveDate,
            @NotBlank @Size(max = 2000) String description) {
    }

    public record MemberChangeView(
            Long id,
            Long memberId,
            String employeeCode,
            String memberName,
            UnionUnit unionUnit,
            String changeType,
            LocalDate effectiveDate,
            String description,
            String recordedBy,
            Instant createdAt) {
    }

    public record MemberDocumentView(
            Long id,
            Long memberId,
            String employeeCode,
            String memberName,
            UnionUnit unionUnit,
            MemberDocumentType documentType,
            String fileName,
            String contentType,
            Long fileSize,
            String uploadedBy,
            Instant createdAt) {
    }

    public record FinanceDocumentView(
            Long id,
            Long financeEntryId,
            String entryCode,
            String fileName,
            String contentType,
            long fileSize,
            String uploadedBy,
            Instant createdAt) {
    }

    public record WelfareDocumentView(
            Long id,
            Long welfareRecordId,
            String recordCode,
            WelfareDocumentType documentType,
            String fileName,
            String contentType,
            Long fileSize,
            String uploadedBy,
            Instant createdAt) {
    }

    public record DocumentLibraryView(
            Long id,
            UnionUnit unionUnit,
            String category,
            String title,
            String description,
            String fileName,
            String contentType,
            Long fileSize,
            String uploadedBy,
            Instant createdAt) {
    }

    /**
     * One member's required-document status. Built server-side because the compliance grid used to
     * cross-join every member against every document in the browser, which cannot be paginated.
     */
    public record MemberComplianceView(
            Long memberId,
            String employeeCode,
            String memberName,
            UnionUnit unionUnit,
            List<MemberDocumentView> documents,
            List<MemberDocumentType> missing) {
    }

    public record ActivityMediaView(
            Long id,
            Long activityId,
            String activityCode,
            String activityName,
            UnionUnit unionUnit,
            ActivityMediaType mediaType,
            String title,
            String fileName,
            String contentType,
            Long fileSize,
            String uploadedBy,
            Instant createdAt) {
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

    public record KpiCriterionView(
            String code,
            String label,
            String target,
            double actual,
            String actualLabel,
            boolean met,
            String note) {
    }

    public record UnitKpiView(
            Long unionUnitId,
            String unionUnitCode,
            String unionUnitName,
            String month,
            int score,
            String rating,
            long passedCriteria,
            List<KpiCriterionView> criteria) {
    }

    public record MonthlySummary(
            String month,
            Long unionUnitId,
            String unionUnitName,
            long activeEmployees,
            long unionMembers,
            long memberChanges,
            long welfareCases,
            long completedWelfareCases,
            long laborCases,
            long closedLaborCases,
            long activities,
            int participants,
            BigDecimal income,
            BigDecimal expense,
            BigDecimal advance,
            BigDecimal netChange,
            long incompleteDocuments,
            MonthlyReport narrative) {
    }
}
