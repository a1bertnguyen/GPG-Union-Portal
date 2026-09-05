package vn.gpg.unionportal.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class KpiModels {
    private KpiModels() {
    }

    public enum PeriodType { MONTH, QUARTER, HALF_YEAR, YEAR }
    public enum RunStatus { DRAFT, PROVISIONAL, FINAL, REOPENED }
    public enum ResultStatus { CALCULATED, NA, MISSING_DATA, FAILED_VALIDATION }
    public enum EvidenceRole { NUMERATOR, DENOMINATOR, EXCLUDED }
    public enum ValidationStatus { VALID, INVALID, PENDING }
    public enum WarningSeverity { INFO, WARNING, CRITICAL }

    public record Period(PeriodType periodType, LocalDate periodStart, LocalDate periodEnd,
                         String periodLabel, int year, int period) {
    }

    public record Warning(String code, WarningSeverity severity, String message,
                          String recommendedAction, LocalDate dueAt, String sourceModule,
                          String sourceRecordId, boolean redacted) {
    }

    public record AdjustmentAudit(Long adjustmentId, String adjustmentType, String penaltyCode,
                                  BigDecimal points, String reason, String evidenceModule,
                                  String evidenceRecordId, boolean effectivenessVerified,
                                  boolean nonDuplicateVerified, String requestedBy,
                                  String approvedBy, Instant approvedAt, boolean redacted) {
    }

    public record Evidence(String evidenceId, String resultId, String sourceModule,
                           String sourceRecordId, EvidenceRole role, String evidenceUrl,
                           String fileName, ValidationStatus validationStatus, boolean redacted) {
    }

    public record Detail(String resultId, String kpiCode, String groupCode, String name,
                         BigDecimal weight, BigDecimal numerator, BigDecimal denominator, BigDecimal targetValue,
                         BigDecimal normalizedScore, BigDecimal eligibleWeight,
                         BigDecimal earnedPoints, ResultStatus resultStatus,
                         String explanation, List<Warning> warnings,
                         List<Evidence> evidence, int evidenceCount) {
    }

    public record GroupResult(String groupCode, String name, BigDecimal configuredWeight,
                              BigDecimal eligibleWeight, BigDecimal earnedPoints,
                              BigDecimal score, ResultStatus status,
                              List<Detail> details) {
    }

    public record UnitResult(String runId, Long unionUnitId, String unionUnitCode,
                             String unionUnitName, Long activeMemberCount, RunStatus runStatus,
                             BigDecimal dataQualityRate, BigDecimal baseScore,
                             BigDecimal bonusPoints, BigDecimal penaltyPoints,
                             BigDecimal finalScore, String rawClassification,
                             String finalClassification, Integer rank, boolean tied,
                             BigDecimal reportOnTimeRate, List<GroupResult> groups,
                             List<Detail> details, List<Warning> warnings,
                             List<AdjustmentAudit> adjustments) {
    }

    /**
     * {@code lockEligibleCount} is how many units a period lock would turn into an official FINAL run:
     * a live dashboard read never produces FINAL on its own, so the other counters stay zero until a lock.
     */
    public record Summary(BigDecimal averageScore, int finalUnitCount, int provisionalUnitCount,
                          int excellentCount, int attentionCount, int lockEligibleCount) {
    }

    public record Dashboard(String versionId, PeriodType periodType, LocalDate periodStart,
                            LocalDate periodEnd, Instant cutoffAt, Instant generatedAt,
                            Summary summary, List<UnitResult> results) {
    }

    public record VersionWindow(String versionId, String name, LocalDate effectiveFrom,
                                LocalDate effectiveTo, String status) {
    }

    public record Metadata(List<VersionWindow> versions) {
    }

    /** What a period lock did to one unit. */
    public record LockedUnit(Long unionUnitId, String unionUnitCode, String unionUnitName, Long runId,
                             int revision, RunStatus runStatus, BigDecimal finalScore,
                             String finalClassification, Integer rank, boolean unchanged,
                             List<String> blockingKpiCodes) {
    }

    public record LockResult(String versionId, PeriodType periodType, LocalDate periodStart,
                             LocalDate periodEnd, Instant lockedAt, String lockedBy, int finalCount,
                             int provisionalCount, int unchangedCount, List<LockedUnit> units) {
    }

    public record HistoryPoint(PeriodType periodType, LocalDate periodStart, LocalDate periodEnd,
                               String periodLabel, String versionId, int revision, RunStatus runStatus,
                               BigDecimal baseScore, BigDecimal bonusPoints, BigDecimal penaltyPoints,
                               BigDecimal finalScore, String rawClassification, String finalClassification,
                               Integer rank, BigDecimal dataQualityRate, Instant lockedAt, String lockedBy) {
    }

    public record UnitHistory(Long unionUnitId, String unionUnitCode, String unionUnitName,
                              List<HistoryPoint> points) {
    }

    public record History(int fromYear, int toYear, PeriodType periodType, List<UnitHistory> units) {
    }

    public record EvidenceField(String label, String value) {
    }

    public record EvidenceAttachment(Long id, String fileName, String downloadPath) {
    }

    public record EvidenceRecord(String sourceModule, String sourceRecordId, String title,
                                 List<EvidenceField> fields, List<EvidenceAttachment> attachments) {
    }
}
