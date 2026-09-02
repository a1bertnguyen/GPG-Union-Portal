package vn.gpg.unionportal.service.kpi;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;
import vn.gpg.unionportal.dto.KpiModels.Detail;
import vn.gpg.unionportal.dto.KpiModels.PeriodType;
import vn.gpg.unionportal.dto.KpiModels.ResultStatus;
import vn.gpg.unionportal.dto.KpiModels.RunStatus;
import vn.gpg.unionportal.model.*;
import vn.gpg.unionportal.model.DomainEnums.*;
import vn.gpg.unionportal.model.kpi.KpiClassificationRule;
import vn.gpg.unionportal.model.kpi.KpiClassificationGate;
import vn.gpg.unionportal.model.kpi.KpiDefinition;
import vn.gpg.unionportal.model.kpi.KpiNoOccurrenceConfirmation;
import vn.gpg.unionportal.model.kpi.KpiAdjustment;
import vn.gpg.unionportal.model.kpi.KpiVersion;
import vn.gpg.unionportal.model.kpi.PenaltyRule;
import vn.gpg.unionportal.model.kpi.SlaRule;
import vn.gpg.unionportal.repository.*;
import vn.gpg.unionportal.repository.kpi.*;
import vn.gpg.unionportal.service.CurrentUserService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GpgKpiEngineTests {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Bangkok");
    private static final YearMonth CURRENT_MONTH = YearMonth.now(BUSINESS_ZONE);
    private static final YearMonth PREVIOUS_MONTH = CURRENT_MONTH.minusMonths(1);
    private static final Map<String, BigDecimal> WEIGHTS = Map.ofEntries(
            Map.entry("GOV01", bd("5")), Map.entry("GOV02", bd("4")),
            Map.entry("GOV03", bd("3")), Map.entry("GOV04", bd("3")),
            Map.entry("DATA01", bd("5")), Map.entry("DATA02", bd("4")),
            Map.entry("DATA03", bd("3")), Map.entry("DATA04", bd("3")),
            Map.entry("REP01", bd("6")), Map.entry("REP02", bd("4")),
            Map.entry("REP03", bd("3")), Map.entry("REP04", bd("2")),
            Map.entry("CARE01", bd("4")), Map.entry("CARE02", bd("6")),
            Map.entry("CARE03", bd("4")), Map.entry("CARE04", bd("3")),
            Map.entry("CARE05", bd("3")), Map.entry("GRV01", bd("3")),
            Map.entry("GRV02", bd("5")), Map.entry("GRV03", bd("3")),
            Map.entry("GRV04", bd("2")), Map.entry("GRV05", bd("2")),
            Map.entry("ACT01", bd("3")), Map.entry("ACT02", bd("2")),
            Map.entry("ACT03", bd("2")), Map.entry("ACT04", bd("2")),
            Map.entry("ACT05", bd("1")), Map.entry("FIN01", bd("4")),
            Map.entry("FIN02", bd("2")), Map.entry("FIN03", bd("2")),
            Map.entry("FIN04", bd("2")));

    @Test
    @SuppressWarnings("unchecked")
    void evaluatesSupportedKpisFromSourceRowsAndLeavesUnsupportedOnesMissing() {
        UnionUnitRepository units = mock(UnionUnitRepository.class);
        MemberRepository members = mock(MemberRepository.class);
        MemberChangeRepository memberChanges = mock(MemberChangeRepository.class);
        MonthlyReportRepository reports = mock(MonthlyReportRepository.class);
        WelfareRecordRepository welfare = mock(WelfareRecordRepository.class);
        LaborCaseRepository cases = mock(LaborCaseRepository.class);
        UnionActivityRepository activities = mock(UnionActivityRepository.class);
        FinanceEntryRepository finance = mock(FinanceEntryRepository.class);
        KpiVersionRepository versions = mock(KpiVersionRepository.class);
        KpiDefinitionRepository definitions = mock(KpiDefinitionRepository.class);
        KpiClassificationRuleRepository classifications = mock(KpiClassificationRuleRepository.class);
        KpiClassificationGateRepository gates = mock(KpiClassificationGateRepository.class);
        PenaltyRuleRepository penalties = mock(PenaltyRuleRepository.class);
        KpiNoOccurrenceConfirmationRepository confirmations = mock(KpiNoOccurrenceConfirmationRepository.class);
        KpiAdjustmentRepository adjustments = mock(KpiAdjustmentRepository.class);
        KpiSourceExclusionRepository exclusions = mock(KpiSourceExclusionRepository.class);
        SlaRuleRepository slas = mock(SlaRuleRepository.class);
        BusinessCalendarDayRepository calendar = mock(BusinessCalendarDayRepository.class);
        CurrentUserService currentUser = mock(CurrentUserService.class);

        UnionUnit unit = unit();
        Member member = member(unit);
        LaborCase closedCase = closedCase(unit);
        KpiVersion version = version();
        KpiVersion draftVersion = draftVersion();
        List<KpiDefinition> catalog = catalog();

        when(definitions.findByVersionIdOrderById(version.getVersionId())).thenReturn(catalog);
        when(classifications.findByVersionIdOrderByMinimumScoreDesc(version.getVersionId()))
                .thenReturn(classificationRules());
        when(gates.findByVersionId(version.getVersionId())).thenReturn(gateRules());
        when(penalties.findByVersionId(version.getVersionId())).thenReturn(penaltyRules());
        when(exclusions.findByActiveTrue()).thenReturn(List.of());
        when(slas.findByVersionId(version.getVersionId())).thenReturn(List.of());
        when(units.findAll(any(Sort.class))).thenReturn(List.of(unit));
        when(versions.findAll(any(Sort.class))).thenReturn(List.of(draftVersion, version));
        when(currentUser.scopedUnitId(null)).thenReturn(null);
        when(currentUser.isAdmin()).thenReturn(true);
        when(members.findAll(any(Specification.class))).thenReturn(List.of(member));
        when(memberChanges.findAll(any(Specification.class))).thenReturn(List.of());
        when(reports.findAll(any(Specification.class))).thenReturn(List.of());
        when(welfare.findAll(any(Specification.class))).thenReturn(List.of());
        when(cases.findAll(any(Specification.class))).thenReturn(List.of(closedCase));
        when(activities.findAll(any(Specification.class))).thenReturn(List.of());
        when(finance.findAll(any(Specification.class))).thenReturn(List.of());
        when(adjustments.findByUnionUnitIdAndPeriodTypeAndPeriodStartAndPeriodEndAndVersionIdAndApprovedByIsNotNullAndApprovedAtIsNotNull(
                unit.getId(), "MONTH", CURRENT_MONTH.atDay(1), CURRENT_MONTH.atEndOfMonth(), version.getVersionId()))
                .thenReturn(List.of());
        when(confirmations.findByUnionUnitIdAndVersionIdAndPeriodStartAndPeriodEndAndReconciledTrueAndApprovedByIsNotNullAndApprovedAtIsNotNull(
                unit.getId(), version.getVersionId(), CURRENT_MONTH.atDay(1), CURRENT_MONTH.atEndOfMonth()))
                .thenReturn(List.of());

        GpgKpiEngine engine = new GpgKpiEngine(units, members, memberChanges, reports, welfare, cases,
                activities, finance, versions, definitions, classifications, gates, penalties, confirmations,
                adjustments, exclusions, slas, calendar, currentUser);

        var dashboard = engine.evaluate(PeriodType.MONTH, CURRENT_MONTH.getYear(), CURRENT_MONTH.getMonthValue(), null);
        var result = dashboard.results().getFirst();

        assertThat(result.runStatus()).isEqualTo(RunStatus.PROVISIONAL);
        assertThat(result.dataQualityRate()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(result.baseScore()).isEqualByComparingTo("5");
        assertThat(result.details()).hasSize(31);
        assertThat(detail(result.details(), "DATA01").resultStatus()).isEqualTo(ResultStatus.CALCULATED);
        assertThat(detail(result.details(), "DATA01").evidence())
                .allSatisfy(evidence -> assertThat(evidence.validationStatus().name()).isEqualTo("VALID"));
        assertThat(detail(result.details(), "GRV03").resultStatus()).isEqualTo(ResultStatus.MISSING_DATA);
        assertThat(detail(result.details(), "ACT02").resultStatus()).isEqualTo(ResultStatus.MISSING_DATA);
        assertThat(detail(result.details(), "GRV03").evidence())
                .allSatisfy(evidence -> {
                    assertThat(evidence.sourceRecordId()).isEqualTo(String.valueOf(closedCase.getId()));
                    assertThat(evidence.redacted()).isFalse();
                    assertThat(evidence.evidenceUrl()).isEqualTo("/kpi/evidence/labor-case/20");
                });
        assertThat(engine.metadata().versions()).singleElement()
                .satisfies(item -> assertThat(item.versionId()).isEqualTo(version.getVersionId()));

        KpiAdjustment bonus = approvedAdjustment(51L, "BONUS", null, "5", "Sáng kiến đã xác minh");
        KpiAdjustment privacyPenalty = approvedAdjustment(52L, "PENALTY", "P07", "10",
                "Vi phạm bảo mật đã xác minh");
        when(adjustments.findByUnionUnitIdAndPeriodTypeAndPeriodStartAndPeriodEndAndVersionIdAndApprovedByIsNotNullAndApprovedAtIsNotNull(
                unit.getId(), "MONTH", CURRENT_MONTH.atDay(1), CURRENT_MONTH.atEndOfMonth(), version.getVersionId()))
                .thenReturn(List.of(bonus, privacyPenalty));
        var adjusted = engine.evaluate(PeriodType.MONTH,
                CURRENT_MONTH.getYear(), CURRENT_MONTH.getMonthValue(), null).results().getFirst();
        assertThat(adjusted.bonusPoints()).isEqualByComparingTo("3");
        assertThat(adjusted.penaltyPoints()).isEqualByComparingTo("10");
        assertThat(adjusted.finalClassification()).isEqualTo("Không đạt");
        assertThat(adjusted.adjustments()).hasSize(2).allSatisfy(audit -> {
            assertThat(audit.redacted()).isFalse();
            assertThat(audit.reason()).isNotBlank();
            assertThat(audit.requestedBy()).isEqualTo("requester");
            assertThat(audit.approvedBy()).isEqualTo("approver");
            assertThat(audit.approvedAt()).isNotNull();
        });
        assertThat(adjusted.warnings()).extracting(warning -> warning.code())
                .contains("BONUS", "P07");

        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.scopedUnitId(null)).thenReturn(unit.getId());
        when(units.findById(unit.getId())).thenReturn(Optional.of(unit));
        var protectedResult = engine.evaluate(PeriodType.MONTH,
                CURRENT_MONTH.getYear(), CURRENT_MONTH.getMonthValue(), null).results().getFirst();
        assertThat(protectedResult.adjustments()).hasSize(2).allSatisfy(audit -> {
            assertThat(audit.redacted()).isTrue();
            assertThat(audit.reason()).isNull();
            assertThat(audit.requestedBy()).isNull();
            assertThat(audit.approvedBy()).isNull();
        });
        Detail protectedGrievance = detail(protectedResult.details(), "GRV03");
        assertThat(protectedGrievance.evidence()).allSatisfy(evidence -> {
            assertThat(evidence.sourceRecordId()).isEqualTo("REDACTED");
            assertThat(evidence.redacted()).isTrue();
        });
        when(adjustments.findByUnionUnitIdAndPeriodTypeAndPeriodStartAndPeriodEndAndVersionIdAndApprovedByIsNotNullAndApprovedAtIsNotNull(
                unit.getId(), "MONTH", CURRENT_MONTH.atDay(1), CURRENT_MONTH.atEndOfMonth(), version.getVersionId()))
                .thenReturn(List.of());

        SlaRule reportSla = reportSla();
        when(slas.findByVersionId(version.getVersionId())).thenReturn(List.of(reportSla));
        var overdueReport = engine.evaluate(PeriodType.MONTH,
                PREVIOUS_MONTH.getYear(), PREVIOUS_MONTH.getMonthValue(), null).results().getFirst();
        assertThat(overdueReport.activeMemberCount()).isNull();
        assertThat(detail(overdueReport.details(), "REP01").resultStatus()).isEqualTo(ResultStatus.CALCULATED);
        assertThat(detail(overdueReport.details(), "DATA01").resultStatus()).isEqualTo(ResultStatus.MISSING_DATA);
        assertThat(detail(overdueReport.details(), "REP01").numerator()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(detail(overdueReport.details(), "REP01").denominator()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(detail(overdueReport.details(), "REP01").evidence()).singleElement().satisfies(evidence -> {
            assertThat(evidence.sourceRecordId()).isEqualTo("1:" + PREVIOUS_MONTH);
            assertThat(evidence.evidenceUrl()).isEqualTo("/kpi/evidence/report-obligation/1:" + PREVIOUS_MONTH);
        });
        assertThat(overdueReport.penaltyPoints()).isEqualByComparingTo("5");

        MonthlyReport invalidReport = new MonthlyReport();
        invalidReport.setId(40L);
        invalidReport.setUnionUnit(unit);
        invalidReport.setReportMonth(PREVIOUS_MONTH.atDay(1));
        invalidReport.setPreparedBy("Người lập");
        invalidReport.setStatus(ReportStatus.SUBMITTED);
        created(invalidReport);
        when(reports.findAll(any(Specification.class))).thenReturn(List.of(invalidReport));
        Detail failedReport = detail(engine.evaluate(PeriodType.MONTH,
                PREVIOUS_MONTH.getYear(), PREVIOUS_MONTH.getMonthValue(), null)
                .results().getFirst().details(), "REP01");
        assertThat(failedReport.resultStatus()).isEqualTo(ResultStatus.FAILED_VALIDATION);
        assertThat(failedReport.evidence()).allSatisfy(evidence ->
                assertThat(evidence.validationStatus().name()).isEqualTo("INVALID"));
        when(reports.findAll(any(Specification.class))).thenReturn(List.of());

        member.setEmail(null);
        Detail incompleteMember = detail(engine.evaluate(PeriodType.MONTH,
                CURRENT_MONTH.getYear(), CURRENT_MONTH.getMonthValue(), null)
                .results().getFirst().details(), "DATA01");
        assertThat(incompleteMember.resultStatus()).isEqualTo(ResultStatus.CALCULATED);
        assertThat(incompleteMember.numerator()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(incompleteMember.evidence()).singleElement().satisfies(evidence ->
                assertThat(evidence.validationStatus().name()).isEqualTo("INVALID"));
        assertThat(incompleteMember.warnings()).extracting(warning -> warning.code())
                .contains("INVALID_SOURCE_RECORD");
        member.setEmail("member@example.test");

        closedCase.setResponseDate(closedCase.getReceivedDate().minusDays(1));
        var invalidChronology = engine.evaluate(PeriodType.MONTH,
                CURRENT_MONTH.getYear(), CURRENT_MONTH.getMonthValue(), null).results().getFirst();
        assertThat(detail(invalidChronology.details(), "GRV03").numerator()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(invalidChronology.dataQualityRate()).isLessThan(BigDecimal.ONE);
        closedCase.setResponseDate(CURRENT_MONTH.atDay(1));

        KpiNoOccurrenceConfirmation confirmation = noOccurrence(unit, version);
        when(slas.findByVersionId(version.getVersionId())).thenReturn(List.of());
        when(cases.findAll(any(Specification.class))).thenReturn(List.of());
        when(confirmations.findByUnionUnitIdAndVersionIdAndPeriodStartAndPeriodEndAndReconciledTrueAndApprovedByIsNotNullAndApprovedAtIsNotNull(
                unit.getId(), version.getVersionId(), CURRENT_MONTH.atDay(1), CURRENT_MONTH.atEndOfMonth()))
                .thenReturn(List.of(confirmation));
        Detail confirmedNa = detail(engine.evaluate(PeriodType.MONTH,
                CURRENT_MONTH.getYear(), CURRENT_MONTH.getMonthValue(), null)
                .results().getFirst().details(), "GRV03");
        assertThat(confirmedNa.resultStatus()).isEqualTo(ResultStatus.NA);
        assertThat(confirmedNa.evidence()).hasSize(2).allSatisfy(evidence -> {
            assertThat(evidence.sourceModule()).isEqualTo("KPI_NO_OCCURRENCE");
            assertThat(evidence.evidenceUrl()).isEqualTo("/kpi/evidence/no-occurrence/30");
        });

        confirmation.setReconciliationSourceModule("SO_KIEN_NGHI");
        assertThat(detail(engine.evaluate(PeriodType.MONTH,
                CURRENT_MONTH.getYear(), CURRENT_MONTH.getMonthValue(), null).results().getFirst().details(), "GRV03")
                .resultStatus()).isEqualTo(ResultStatus.MISSING_DATA);

        LaborCase notYetDue = closedCase(unit);
        notYetDue.setId(21L);
        notYetDue.setCaseCode("KN-REAL-02");
        notYetDue.setStatus(CaseStatus.NEW);
        notYetDue.setDeadline(CURRENT_MONTH.atEndOfMonth());
        confirmation.setKpiCode("GRV02");
        confirmation.setReconciliationSourceModule("INDEPENDENT_INTAKE");
        when(cases.findAll(any(Specification.class))).thenReturn(List.of(notYetDue));
        Detail notDueSla = detail(engine.evaluate(PeriodType.MONTH,
                CURRENT_MONTH.getYear(), CURRENT_MONTH.getMonthValue(), null).results().getFirst().details(), "GRV02");
        assertThat(notDueSla.resultStatus()).isEqualTo(ResultStatus.NA);
    }

    @Test
    void capsCombinedAutomaticAndApprovedPenaltyAndOnlyPenaltyCanTriggerIntegrityGate() {
        GpgKpiEngine engine = emptyEngine();
        PenaltyRule p01 = penaltyRule("P01", "5", "15");

        KpiAdjustment approvedPenalty = new KpiAdjustment();
        approvedPenalty.setAdjustmentType("PENALTY");
        approvedPenalty.setPenaltyCode("P01");
        approvedPenalty.setPoints(bd("12"));
        BigDecimal combined = ReflectionTestUtils.invokeMethod(engine, "totalPenalty",
                List.of(approvedPenalty), Map.of("P01", p01), Map.of("P01", 1));
        assertThat(combined).isEqualByComparingTo("15");

        KpiAdjustment integrity = new KpiAdjustment();
        integrity.setAdjustmentType("BONUS");
        integrity.setPenaltyCode("P06");
        integrity.setPoints(bd("15"));
        assertThat((Boolean) ReflectionTestUtils.invokeMethod(engine, "hasIntegrityViolation",
                List.of(integrity))).isFalse();

        integrity.setAdjustmentType("PENALTY");
        assertThat((Boolean) ReflectionTestUtils.invokeMethod(engine, "hasIntegrityViolation",
                List.of(integrity))).isTrue();

        KpiAdjustment unknownPenalty = new KpiAdjustment();
        unknownPenalty.setAdjustmentType("PENALTY");
        unknownPenalty.setPenaltyCode("P99");
        unknownPenalty.setPoints(bd("99"));
        assertThat((BigDecimal) ReflectionTestUtils.invokeMethod(engine, "totalPenalty",
                List.of(unknownPenalty), Map.of("P01", p01), Map.of())).isEqualByComparingTo(BigDecimal.ZERO);

        KpiAdjustment unverifiedBonus = approvedAdjustment(90L, "BONUS", null, "3", "Chưa đủ kiểm tra");
        unverifiedBonus.setEffectivenessVerified(false);
        assertThat((Boolean) ReflectionTestUtils.invokeMethod(engine, "isApplicableAdjustment",
                unverifiedBonus, Map.of("P01", p01))).isFalse();
    }

    @Test
    void rejectsKpiPeriodThatHasNotStarted() {
        YearMonth nextMonth = YearMonth.now(ZoneId.of("Asia/Bangkok")).plusMonths(1);
        assertThatThrownBy(() -> emptyEngine().evaluate(PeriodType.MONTH,
                nextMonth.getYear(), nextMonth.getMonthValue(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("kỳ chưa bắt đầu");
    }

    @Test
    void requiresOneApprovedVersionToCoverTheWholePeriodWithoutOverlap() {
        KpiVersionRepository versionRepository = mock(KpiVersionRepository.class);
        GpgKpiEngine engine = emptyEngine(versionRepository);
        KpiVersion partial = version();
        partial.setEffectiveTo(LocalDate.of(2026, 8, 15));
        when(versionRepository.findAll(any(Sort.class))).thenReturn(List.of(partial));
        var august = GpgKpiEngine.resolvePeriod(PeriodType.MONTH, 2026, 8);
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(engine, "activeVersion", august))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bao phủ toàn bộ kỳ");

        KpiVersion overlapping = version();
        overlapping.setVersionId("GPG-CD-KPI-OVERLAP");
        partial.setEffectiveTo(null);
        when(versionRepository.findAll(any(Sort.class))).thenReturn(List.of(overlapping, partial));
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(engine, "activeVersion", august))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("chồng lấn");
    }

    @Test
    void rejectsNonHundredPointScaleAndInvalidReportSla() {
        GpgKpiEngine engine = emptyEngine();
        KpiVersion invalidScale = version();
        invalidScale.setScoreScale(bd("200"));
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(engine, "validateVersion", invalidScale))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("phiên bản KPI");

        SlaRule invalidSla = reportSla();
        invalidSla.setDurationValue(0);
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(engine, "validateReportSla", invalidSla))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("REPORT_SUBMISSION");

        invalidSla.setDurationValue(1);
        invalidSla.setDurationUnit("TYPO");
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(engine, "validateReportSla", invalidSla))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("REPORT_SUBMISSION");
    }

    private static Detail detail(List<Detail> details, String code) {
        return details.stream().filter(item -> code.equals(item.kpiCode())).findFirst().orElseThrow();
    }

    private static GpgKpiEngine emptyEngine() {
        return emptyEngine(mock(KpiVersionRepository.class));
    }

    private static GpgKpiEngine emptyEngine(KpiVersionRepository versionRepository) {
        return new GpgKpiEngine(mock(UnionUnitRepository.class), mock(MemberRepository.class),
                mock(MemberChangeRepository.class), mock(MonthlyReportRepository.class),
                mock(WelfareRecordRepository.class), mock(LaborCaseRepository.class),
                mock(UnionActivityRepository.class), mock(FinanceEntryRepository.class),
                versionRepository, mock(KpiDefinitionRepository.class),
                mock(KpiClassificationRuleRepository.class), mock(KpiClassificationGateRepository.class),
                mock(PenaltyRuleRepository.class), mock(KpiNoOccurrenceConfirmationRepository.class),
                mock(KpiAdjustmentRepository.class), mock(KpiSourceExclusionRepository.class),
                mock(SlaRuleRepository.class), mock(BusinessCalendarDayRepository.class),
                mock(CurrentUserService.class));
    }

    private static SlaRule reportSla() {
        SlaRule result = new SlaRule();
        result.setSlaCode("REPORT_SUBMISSION");
        result.setVersionId("GPG-CD-KPI-V1");
        result.setCaseType("REPORT");
        result.setPriority("ALL");
        result.setDurationValue(1);
        result.setDurationUnit("BUSINESS_DAY");
        result.setBusinessCalendarId("GPG_DEFAULT");
        return result;
    }

    private static PenaltyRule penaltyRule(String code, String points, String cap) {
        PenaltyRule result = new PenaltyRule();
        result.setPenaltyCode(code);
        result.setVersionId("GPG-CD-KPI-V1");
        result.setPointsPerCase(bd(points));
        result.setPeriodCap(cap == null ? null : bd(cap));
        result.setDetectionRule("test_rule");
        return result;
    }

    private static List<PenaltyRule> penaltyRules() {
        return List.of(
                penaltyRule("P01", "5", "15"), penaltyRule("P02", "2", "10"),
                penaltyRule("P03", "5", "15"), penaltyRule("P04", "3", "12"),
                penaltyRule("P05", "3", "12"), penaltyRule("P06", "15", null),
                penaltyRule("P07", "10", null));
    }

    private static List<KpiClassificationGate> gateRules() {
        return List.of(
                gate("INTEGRITY_VIOLATION", "Không đạt"), gate("SERIOUS_OPEN_CASE", "Trung bình"),
                gate("MISSING_MANDATORY_REPORT", "Khá"), gate("GOVERNANCE_INCOMPLETE", "Khá"));
    }

    private static KpiClassificationGate gate(String code, String cap) {
        KpiClassificationGate result = new KpiClassificationGate();
        result.setGateCode(code);
        result.setClassificationCap(cap);
        result.setDetectionRule("test_gate");
        return result;
    }

    private static KpiNoOccurrenceConfirmation noOccurrence(UnionUnit unit, KpiVersion version) {
        KpiNoOccurrenceConfirmation result = new KpiNoOccurrenceConfirmation();
        result.setId(30L);
        result.setUnionUnitId(unit.getId());
        result.setVersionId(version.getVersionId());
        result.setKpiCode("GRV03");
        result.setPeriodStart(CURRENT_MONTH.atDay(1));
        result.setPeriodEnd(CURRENT_MONTH.atEndOfMonth());
        result.setSourceModule("SO_KIEN_NGHI");
        result.setReconciliationSourceModule("INDEPENDENT_INTAKE");
        result.setReconciled(true);
        result.setConfirmedBy("checker");
        result.setApprovedBy("approver");
        result.setApprovedAt(Instant.now().minusSeconds(60));
        return result;
    }

    private static KpiAdjustment approvedAdjustment(Long id, String type, String penaltyCode,
                                                     String points, String reason) {
        KpiAdjustment result = new KpiAdjustment();
        result.setId(id);
        result.setAdjustmentType(type);
        result.setPenaltyCode(penaltyCode);
        result.setPoints(bd(points));
        result.setReason(reason);
        result.setEvidenceModule("HOAT_DONG");
        result.setEvidenceRecordId("ACT-REAL-01");
        result.setEffectivenessVerified("BONUS".equals(type));
        result.setNonDuplicateVerified("BONUS".equals(type));
        result.setRequestedBy("requester");
        result.setApprovedBy("approver");
        result.setApprovedAt(Instant.now().minusSeconds(60));
        return result;
    }

    private static UnionUnit unit() {
        UnionUnit result = new UnionUnit();
        result.setId(1L);
        result.setCode("REAL");
        result.setName("Công đoàn dữ liệu thật");
        result.setCompanyName("GPG");
        result.setChairperson("Chủ tịch");
        result.setDecisionNumber("QD-01");
        result.setTermStart(LocalDate.of(2020, 1, 1));
        result.setTermEnd(LocalDate.of(2200, 12, 31));
        result.setLegalStatus(LegalStatus.ACTIVE);
        created(result);
        return result;
    }

    private static Member member(UnionUnit unit) {
        Member result = new Member();
        result.setId(10L);
        result.setUnionUnit(unit);
        result.setEmployeeCode("NV-REAL-01");
        result.setFullName("Nguyễn Dữ Liệu");
        result.setCompany("GPG");
        result.setJobTitle("Điều phối");
        result.setWorkplace("Kho vận");
        result.setStartWorkDate(CURRENT_MONTH.atDay(1).minusYears(1));
        result.setJoinDate(CURRENT_MONTH.atDay(1).minusMonths(6));
        result.setEmail("member@example.test");
        result.setPhone("0900000000");
        result.setMembershipStatus(MembershipStatus.MEMBER);
        result.setEmploymentStatus(EmploymentStatus.ACTIVE);
        created(result);
        return result;
    }

    private static LaborCase closedCase(UnionUnit unit) {
        LaborCase result = new LaborCase();
        result.setId(20L);
        result.setUnionUnit(unit);
        result.setCaseCode("KN-REAL-01");
        result.setReceivedDate(CURRENT_MONTH.atDay(1));
        result.setRequesterName("NLĐ đã ẩn");
        result.setIssueGroup("Quyền lợi");
        result.setSeverity(CaseSeverity.MEDIUM);
        result.setStatus(CaseStatus.CLOSED);
        result.setDescription("Nội dung kiến nghị");
        result.setAffectedPeople(1);
        result.setResultText("Đã xử lý");
        result.setResponseDate(CURRENT_MONTH.atDay(1));
        result.setApprovedBy("admin");
        result.setApprovedAt(CURRENT_MONTH.atDay(1).atStartOfDay(BUSINESS_ZONE).toInstant());
        created(result);
        return result;
    }

    private static KpiVersion version() {
        KpiVersion result = new KpiVersion();
        result.setVersionId("GPG-CD-KPI-V1");
        result.setEffectiveFrom(LocalDate.of(2020, 1, 1));
        result.setStatus("ACTIVE");
        result.setScoreScale(bd("100"));
        result.setRoundDisplay(2);
        result.setBonusCap(bd("3"));
        result.setDataQualityFinalThreshold(bd("0.8"));
        result.setApprovedBy("approver");
        result.setApprovedAt(Instant.parse("2019-12-01T00:00:00Z"));
        return result;
    }

    private static KpiVersion draftVersion() {
        KpiVersion result = version();
        result.setVersionId("GPG-CD-KPI-DRAFT");
        result.setEffectiveFrom(CURRENT_MONTH.atDay(1));
        result.setStatus("DRAFT");
        return result;
    }

    private static List<KpiDefinition> catalog() {
        List<KpiDefinition> result = new ArrayList<>();
        for (String code : GpgKpiEngine.EXPECTED_CODES) {
            KpiDefinition definition = new KpiDefinition();
            definition.setKpiCode(code);
            definition.setGroupCode(code.replaceAll("\\d", ""));
            definition.setName(code);
            definition.setWeight(WEIGHTS.get(code));
            definition.setDirection("HIGHER_BETTER");
            definition.setTargetValue(BigDecimal.ONE);
            definition.setSourceModule(code.startsWith("GRV") ? "SO_KIEN_NGHI" : "TEST_SOURCE");
            if (code.startsWith("GRV")) definition.setNaAllowed(true);
            definition.setNumeratorRule("test_numerator");
            definition.setDenominatorRule("test_denominator");
            definition.setEvidenceRule("test_evidence");
            result.add(definition);
        }
        return result;
    }

    private static List<KpiClassificationRule> classificationRules() {
        return List.of(rule("Xuất sắc", "90"), rule("Tốt", "80"), rule("Khá", "65"),
                rule("Trung bình", "50"), rule("Không đạt", "0"));
    }

    private static KpiClassificationRule rule(String label, String score) {
        KpiClassificationRule result = new KpiClassificationRule();
        result.setLabel(label);
        result.setMinimumScore(bd(score));
        return result;
    }

    private static void created(BaseEntity entity) {
        Instant createdAt = CURRENT_MONTH.atDay(1).minusMonths(1).atStartOfDay(BUSINESS_ZONE).toInstant();
        ReflectionTestUtils.setField(entity, "createdAt", createdAt);
        ReflectionTestUtils.setField(entity, "updatedAt", createdAt);
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
