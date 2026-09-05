package vn.gpg.unionportal.service.kpi;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.dto.KpiModels.*;
import vn.gpg.unionportal.exception.ResourceNotFoundException;
import vn.gpg.unionportal.model.*;
import vn.gpg.unionportal.model.DomainEnums.*;
import vn.gpg.unionportal.model.kpi.*;
import vn.gpg.unionportal.repository.*;
import vn.gpg.unionportal.repository.kpi.*;
import vn.gpg.unionportal.service.CurrentUserService;
import vn.gpg.unionportal.spec.MemberSpecs;
import vn.gpg.unionportal.spec.Specs;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class GpgKpiEngine {
    static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Bangkok");
    private static final MathContext MC = MathContext.DECIMAL128;
    /** Joins a module and a record key into one exclusion lookup key; NUL cannot occur in either part. */
    private static final char EXCLUSION_SEPARATOR = 0;
    private static final BigDecimal USEFULNESS_SCALE_MAX = BigDecimal.valueOf(5);
    private static final List<String> GROUP_ORDER = List.of("GOV", "DATA", "REP", "CARE", "GRV", "ACT", "FIN");
    private static final Map<String, String> GROUP_NAMES = Map.of(
            "GOV", "Tổ chức, hồ sơ và năng lực BCH",
            "DATA", "Đoàn viên và chất lượng dữ liệu",
            "REP", "Báo cáo, kế hoạch và tuân thủ kỳ",
            "CARE", "Chăm lo, chính sách và quyền lợi NLĐ",
            "GRV", "Kiến nghị, phản ánh và quan hệ lao động",
            "ACT", "Hoạt động, chương trình và gắn kết",
            "FIN", "Tài chính, ngân sách và chứng từ");
    private static final Set<String> SUPPORTED_DIRECTIONS = Set.of(
            "HIGHER_BETTER", "LOWER_BETTER", "RATING_1_5", "BOOLEAN");
    private static final Set<String> SELECTABLE_VERSION_STATUSES = Set.of("ACTIVE", "RETIRED");
    private static final Set<String> REQUIRED_PENALTY_CODES = Set.of("P01", "P02", "P03", "P04", "P05", "P06", "P07");
    private static final Set<String> REQUIRED_GATE_CODES = Set.of(
            "INTEGRITY_VIOLATION", "SERIOUS_OPEN_CASE", "MISSING_MANDATORY_REPORT", "GOVERNANCE_INCOMPLETE");
    static final String REPORT_SLA = "REPORT_SUBMISSION";
    static final String MEMBER_CHANGE_SLA = "MEMBER_CHANGE";
    static final String GRIEVANCE_ACK_SLA = "GRV_ACK";
    static final String CARE_INTAKE_SLA = "CARE_NORMAL";

    /**
     * Catalog of {@code GPG-CD-KPI-V2}. Every code here has a calculator below that reads both a numerator
     * and a denominator from data the schema actually stores. The V1 codes that needed sources which do not
     * exist yet (cadre training, an organisation change log, an approval log, worker feedback, approved
     * programme goals, balance reconciliation) are deliberately absent: they only ever produced
     * {@code MISSING_DATA} and dragged every unit's score to zero. Adding one back means a new version plus a
     * calculator, never a config-only change.
     */
    static final Set<String> EXPECTED_CODES = Set.of(
            "GOV01", "GOV02",
            "DATA01", "DATA02", "DATA03", "DATA04",
            "REP01", "REP02",
            "CARE01", "CARE02", "CARE03", "CARE04",
            "GRV01", "GRV02", "GRV03", "GRV04",
            "ACT01", "ACT02", "ACT03", "ACT04",
            "FIN01", "FIN02", "FIN03");

    private final UnionUnitRepository units;
    private final MemberRepository members;
    private final MemberChangeRepository memberChanges;
    private final MonthlyReportRepository reports;
    private final WelfareRecordRepository welfare;
    private final LaborCaseRepository cases;
    private final UnionActivityRepository activities;
    private final FinanceEntryRepository finance;
    private final KpiVersionRepository versions;
    private final KpiDefinitionRepository definitions;
    private final KpiClassificationRuleRepository classifications;
    private final KpiClassificationGateRepository classificationGates;
    private final PenaltyRuleRepository penaltyRules;
    private final KpiNoOccurrenceConfirmationRepository noOccurrence;
    private final KpiAdjustmentRepository adjustments;
    private final KpiSourceExclusionRepository sourceExclusions;
    private final SlaRuleRepository slaRules;
    private final BusinessCalendarDayRepository calendarDays;
    private final KpiSourceEvidenceIndex evidenceIndex;
    private final CurrentUserService currentUser;
    private KpiPopulationService populations;

    @org.springframework.beans.factory.annotation.Autowired
    public void setPopulations(KpiPopulationService populations) { this.populations = populations; }

    public GpgKpiEngine(UnionUnitRepository units, MemberRepository members,
                        MemberChangeRepository memberChanges, MonthlyReportRepository reports,
                        WelfareRecordRepository welfare, LaborCaseRepository cases,
                        UnionActivityRepository activities, FinanceEntryRepository finance,
                        KpiVersionRepository versions, KpiDefinitionRepository definitions,
                        KpiClassificationRuleRepository classifications,
                        KpiClassificationGateRepository classificationGates, PenaltyRuleRepository penaltyRules,
                        KpiNoOccurrenceConfirmationRepository noOccurrence,
                        KpiAdjustmentRepository adjustments, KpiSourceExclusionRepository sourceExclusions,
                        SlaRuleRepository slaRules, BusinessCalendarDayRepository calendarDays,
                        KpiSourceEvidenceIndex evidenceIndex, CurrentUserService currentUser) {
        this.units = units;
        this.members = members;
        this.memberChanges = memberChanges;
        this.reports = reports;
        this.welfare = welfare;
        this.cases = cases;
        this.activities = activities;
        this.finance = finance;
        this.versions = versions;
        this.definitions = definitions;
        this.classifications = classifications;
        this.classificationGates = classificationGates;
        this.penaltyRules = penaltyRules;
        this.noOccurrence = noOccurrence;
        this.adjustments = adjustments;
        this.sourceExclusions = sourceExclusions;
        this.slaRules = slaRules;
        this.calendarDays = calendarDays;
        this.evidenceIndex = evidenceIndex;
        this.currentUser = currentUser;
    }

    public Dashboard evaluate(PeriodType periodType, int year, int ordinal, Long requestedUnitId) {
        Period period = resolvePeriod(periodType, year, ordinal);
        Instant cutoff = Instant.now();
        LocalDate cutoffDate = LocalDate.ofInstant(cutoff, BUSINESS_ZONE);
        if (period.periodStart().isAfter(cutoffDate)) {
            throw new IllegalArgumentException("Không thể tính KPI cho kỳ chưa bắt đầu");
        }
        KpiVersion version = activeVersion(period);
        validateVersion(version);
        List<KpiDefinition> configured = definitions.findByVersionIdOrderById(version.getVersionId());
        validateCatalog(configured);
        List<KpiClassificationRule> classRules = classifications
                .findByVersionIdOrderByMinimumScoreDesc(version.getVersionId());
        if (classRules.isEmpty()) throw new IllegalStateException("Phiên bản KPI chưa có ngưỡng xếp loại");
        Map<String, PenaltyRule> penalties = penaltyRules.findByVersionId(version.getVersionId()).stream()
                .collect(Collectors.toMap(PenaltyRule::getPenaltyCode, Function.identity()));
        Map<String, KpiClassificationGate> gates = classificationGates.findByVersionId(version.getVersionId()).stream()
                .collect(Collectors.toMap(KpiClassificationGate::getGateCode, Function.identity()));
        validateRules(version, classRules, penalties, gates);
        Set<String> excludedKeys = sourceExclusions.findByActiveTrue().stream()
                .map(item -> exclusionKey(item.getSourceModule(), item.getSourceRecordKey()))
                .collect(Collectors.toUnmodifiableSet());
        Map<String, SlaRule> slaByCode = slaRulesByCode(version);
        Map<LocalDate, Boolean> calendarOverrides = calendarOverrides(slaByCode.values(), period);

        Long scopedUnitId = currentUser.scopedUnitId(requestedUnitId);
        List<UnionUnit> selected = scopedUnitId == null
                ? units.findAll(Sort.by("code"))
                : List.of(units.findById(scopedUnitId).orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy CĐCS với id=" + scopedUnitId)));

        List<UnitResult> calculatedResults = selected.stream()
                .map(unit -> evaluateUnit(unit, period, cutoff, version, configured, classRules, penalties, gates,
                        excludedKeys, slaByCode, calendarOverrides))
                .sorted(rankingComparator())
                .toList();
        List<UnitResult> results = rankOfficial(calculatedResults);
        Summary summary = summarize(results, version);
        return new Dashboard(version.getVersionId(), period.periodType(), period.periodStart(), period.periodEnd(),
                cutoff, Instant.now(), summary, results);
    }

    public Metadata metadata() {
        List<VersionWindow> available = versions.findAll(Sort.by(Sort.Direction.DESC, "effectiveFrom")).stream()
                .filter(this::isSelectableVersion)
                .map(item -> new VersionWindow(item.getVersionId(), item.getName(), item.getEffectiveFrom(),
                        item.getEffectiveTo(), item.getStatus()))
                .toList();
        return new Metadata(available);
    }

    public static Period resolvePeriod(PeriodType type, int year, int ordinal) {
        if (year < 2000 || year > 2200) throw new IllegalArgumentException("Năm KPI không hợp lệ");
        return switch (type) {
            case MONTH -> {
                if (ordinal < 1 || ordinal > 12) throw new IllegalArgumentException("Tháng phải từ 1 đến 12");
                YearMonth month = YearMonth.of(year, ordinal);
                yield new Period(type, month.atDay(1), month.atEndOfMonth(), month.toString(), year, ordinal);
            }
            case QUARTER -> {
                if (ordinal < 1 || ordinal > 4) throw new IllegalArgumentException("Quý phải từ 1 đến 4");
                int firstMonth = (ordinal - 1) * 3 + 1;
                LocalDate start = LocalDate.of(year, firstMonth, 1);
                yield new Period(type, start, start.plusMonths(3).minusDays(1), "Q" + ordinal + "/" + year, year, ordinal);
            }
            case HALF_YEAR -> {
                if (ordinal < 1 || ordinal > 2) throw new IllegalArgumentException("Nửa năm phải là 1 hoặc 2");
                int firstMonth = ordinal == 1 ? 1 : 7;
                LocalDate start = LocalDate.of(year, firstMonth, 1);
                yield new Period(type, start, start.plusMonths(6).minusDays(1), "H" + ordinal + "/" + year, year, ordinal);
            }
            case YEAR -> {
                LocalDate start = LocalDate.of(year, 1, 1);
                yield new Period(type, start, LocalDate.of(year, 12, 31), String.valueOf(year), year, 1);
            }
        };
    }

    /** One row per SLA code; the unique key on {@code sla_rules} makes a duplicate a configuration error. */
    private Map<String, SlaRule> slaRulesByCode(KpiVersion version) {
        Map<String, SlaRule> result = new HashMap<>();
        for (SlaRule rule : slaRules.findByVersionId(version.getVersionId())) {
            if (result.put(rule.getSlaCode(), rule) != null) {
                throw new IllegalStateException("Phiên bản KPI có nhiều SLA " + rule.getSlaCode());
            }
        }
        validateSla(result.get(REPORT_SLA), REPORT_SLA);
        validateSla(result.get(MEMBER_CHANGE_SLA), MEMBER_CHANGE_SLA);
        validateSla(result.get(GRIEVANCE_ACK_SLA), GRIEVANCE_ACK_SLA);
        validateSla(result.get(CARE_INTAKE_SLA), CARE_INTAKE_SLA);
        return Map.copyOf(result);
    }

    /**
     * Non-working days for every calendar the version's SLA rules point at. The window reaches past the
     * period on both sides because an SLA anchored near a boundary resolves outside it.
     */
    private Map<LocalDate, Boolean> calendarOverrides(Collection<SlaRule> rules, Period period) {
        Set<String> calendarIds = rules.stream().map(SlaRule::getBusinessCalendarId)
                .filter(this::present).collect(Collectors.toCollection(LinkedHashSet::new));
        if (calendarIds.isEmpty()) return Map.of();
        Map<LocalDate, Boolean> result = new HashMap<>();
        for (String calendarId : calendarIds) {
            calendarDays.findByBusinessCalendarIdAndCalendarDateBetween(calendarId,
                            period.periodStart().minusDays(60), period.periodEnd().plusDays(60))
                    .forEach(day -> result.merge(day.getCalendarDate(), day.isWorkingDay(),
                            (left, right) -> left && right));
        }
        return Map.copyOf(result);
    }

    private KpiVersion activeVersion(Period period) {
        List<KpiVersion> matching = versions.findAll(Sort.by(Sort.Direction.DESC, "effectiveFrom")).stream()
                .filter(this::isSelectableVersion)
                .filter(item -> !item.getEffectiveFrom().isAfter(period.periodStart()))
                .filter(item -> item.getEffectiveTo() == null || !item.getEffectiveTo().isBefore(period.periodEnd()))
                .toList();
        if (matching.isEmpty()) {
            throw new IllegalArgumentException("Không có một phiên bản KPI bao phủ toàn bộ kỳ đã chọn");
        }
        if (matching.size() > 1) {
            throw new IllegalStateException("Nhiều phiên bản KPI đang chồng lấn toàn bộ kỳ đã chọn");
        }
        return matching.getFirst();
    }

    private boolean isSelectableVersion(KpiVersion version) {
        return version.getApprovedAt() != null && present(version.getApprovedBy()) && present(version.getStatus())
                && SELECTABLE_VERSION_STATUSES.contains(version.getStatus().trim().toUpperCase(Locale.ROOT));
    }

    private void validateVersion(KpiVersion version) {
        boolean invalid = version.getEffectiveFrom() == null
                || version.getEffectiveTo() != null && version.getEffectiveTo().isBefore(version.getEffectiveFrom())
                || version.getScoreScale() == null
                || version.getScoreScale().compareTo(BigDecimal.valueOf(100)) != 0
                || version.getBonusCap() == null || version.getBonusCap().signum() < 0
                || version.getBonusCap().compareTo(version.getScoreScale()) > 0
                || version.getDataQualityFinalThreshold() == null
                || version.getDataQualityFinalThreshold().signum() < 0
                || version.getDataQualityFinalThreshold().compareTo(BigDecimal.ONE) > 0
                || version.getRoundDisplay() < 0 || version.getRoundDisplay() > 6;
        if (invalid) throw new IllegalStateException("Cấu hình phiên bản KPI không hợp lệ");
    }

    private void validateReportSla(SlaRule rule) {
        validateSla(rule, REPORT_SLA);
    }

    /** An absent SLA is tolerated — the KPIs that need it report missing data instead of guessing. */
    private void validateSla(SlaRule rule, String slaCode) {
        if (rule == null) return;
        boolean invalid = rule.getDurationValue() <= 0
                || !("BUSINESS_DAY".equals(rule.getDurationUnit())
                || "CALENDAR_DAY".equals(rule.getDurationUnit()))
                || !present(rule.getBusinessCalendarId());
        if (invalid) throw new IllegalStateException("SLA " + slaCode + " không hợp lệ");
    }

    private void validateCatalog(List<KpiDefinition> configured) {
        Set<String> actual = configured.stream().map(KpiDefinition::getKpiCode).collect(Collectors.toSet());
        BigDecimal totalWeight = configured.stream().map(KpiDefinition::getWeight)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean invalidDefinition = configured.stream().anyMatch(item -> item.getWeight() == null
                || item.getWeight().signum() <= 0 || !GROUP_ORDER.contains(item.getGroupCode())
                || !SUPPORTED_DIRECTIONS.contains(item.getDirection()) || !present(item.getSourceModule())
                || !present(item.getNumeratorRule()) || !present(item.getDenominatorRule())
                || !present(item.getEvidenceRule()) || invalidTarget(item));
        Map<String, BigDecimal> groupWeights = configured.stream().collect(Collectors.groupingBy(
                KpiDefinition::getGroupCode, Collectors.reducing(BigDecimal.ZERO, KpiDefinition::getWeight,
                        BigDecimal::add)));
        boolean invalidGroupWeight = !groupWeights.keySet().equals(Set.copyOf(GROUP_ORDER))
                || groupWeights.values().stream().anyMatch(weight -> weight.signum() <= 0);
        Set<String> expected = new HashSet<>(EXPECTED_CODES);
        if (configured.stream().anyMatch(d -> "GPG-CD-KPI-V3".equals(d.getVersionId()))) expected.add("CARE05");
        if (configured.size() != expected.size() || !actual.equals(expected)
                || invalidDefinition || invalidGroupWeight || totalWeight.compareTo(BigDecimal.valueOf(100)) != 0) {
            throw new IllegalStateException("Phiên bản KPI phải có đúng " + EXPECTED_CODES.size()
                    + " mã theo đặc tả GPG");
        }
    }

    private boolean invalidTarget(KpiDefinition definition) {
        return switch (definition.getDirection()) {
            case "HIGHER_BETTER" -> definition.getTargetValue() == null
                    || definition.getTargetValue().signum() <= 0;
            case "LOWER_BETTER" -> definition.getTargetValue() == null
                    || definition.getMaxAllowedValue() == null
                    || definition.getMaxAllowedValue().compareTo(definition.getTargetValue()) <= 0;
            default -> false;
        };
    }

    private void validateRules(KpiVersion version, List<KpiClassificationRule> classRules,
                               Map<String, PenaltyRule> penalties,
                               Map<String, KpiClassificationGate> gates) {
        Set<String> labels = classRules.stream().map(KpiClassificationRule::getLabel).collect(Collectors.toSet());
        boolean invalidClassifications = classRules.size() != labels.size()
                || classRules.stream().anyMatch(item -> !present(item.getLabel()) || item.getMinimumScore() == null
                || item.getMinimumScore().signum() < 0
                || item.getMinimumScore().compareTo(version.getScoreScale()) > 0)
                || classRules.stream().noneMatch(item -> item.getMinimumScore().compareTo(BigDecimal.ZERO) == 0);
        boolean invalidGates = gates.values().stream().anyMatch(item -> !labels.contains(item.getClassificationCap())
                || !present(item.getDetectionRule()));
        boolean invalidPenalties = penalties.values().stream().anyMatch(item -> item.getPointsPerCase() == null
                || item.getPointsPerCase().signum() < 0
                || item.getPeriodCap() != null && item.getPeriodCap().signum() < 0
                || !present(item.getDetectionRule()));
        if (invalidClassifications || invalidGates || invalidPenalties
                || !penalties.keySet().containsAll(REQUIRED_PENALTY_CODES)
                || !gates.keySet().containsAll(REQUIRED_GATE_CODES)) {
            throw new IllegalStateException("Ngưỡng xếp loại, cổng hoặc mức phạt của phiên bản KPI không hợp lệ");
        }
    }

    private UnitResult evaluateUnit(UnionUnit unit, Period period, Instant cutoff, KpiVersion version,
                                    List<KpiDefinition> configured, List<KpiClassificationRule> classRules,
                                    Map<String, PenaltyRule> penalties,
                                    Map<String, KpiClassificationGate> gates, Set<String> excludedKeys,
                                    Map<String, SlaRule> slaByCode, Map<LocalDate, Boolean> calendarOverrides) {
        UnitData data = load(unit, period, cutoff, excludedKeys, slaByCode, calendarOverrides);
        boolean canViewSensitive = currentUser.isAdmin();
        String runId = "KPI-" + version.getVersionId() + "-" + period.periodType() + "-"
                + period.periodStart() + "-" + unit.getCode();
        Map<String, KpiNoOccurrenceConfirmation> confirmedNoOccurrence = noOccurrence
                .findByUnionUnitIdAndVersionIdAndPeriodStartAndPeriodEndAndReconciledTrueAndApprovedByIsNotNullAndApprovedAtIsNotNull(
                        unit.getId(), version.getVersionId(), period.periodStart(), period.periodEnd()).stream()
                .filter(item -> present(item.getConfirmedBy()) && present(item.getApprovedBy())
                        && item.getApprovedAt() != null && !item.getApprovedAt().isAfter(cutoff))
                .collect(Collectors.toUnmodifiableMap(KpiNoOccurrenceConfirmation::getKpiCode,
                        Function.identity()));
        List<Detail> details = configured.stream()
                .map(definition -> detail(runId, unit, period, definition, data, canViewSensitive,
                        confirmedNoOccurrence))
                .toList();
        List<GroupResult> groups = groups(details, configured);

        BigDecimal rawBase = KpiScoringPolicy.baseScore(details);
        List<KpiAdjustment> approvedAdjustments = adjustments
                .findByUnionUnitIdAndPeriodTypeAndPeriodStartAndPeriodEndAndVersionIdAndApprovedByIsNotNullAndApprovedAtIsNotNull(
                        unit.getId(), period.periodType().name(), period.periodStart(), period.periodEnd(), version.getVersionId())
                .stream().filter(item -> present(item.getRequestedBy()) && present(item.getApprovedBy())
                        && present(item.getReason()) && item.getApprovedAt() != null
                        && !item.getApprovedAt().isAfter(cutoff)).toList();
        List<KpiAdjustment> appliedAdjustments = approvedAdjustments.stream()
                .filter(item -> isApplicableAdjustment(item, penalties))
                .sorted(Comparator.comparing(KpiAdjustment::getApprovedAt)
                        .thenComparing(KpiAdjustment::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
        BigDecimal bonus = appliedAdjustments.stream().filter(item -> "BONUS".equals(item.getAdjustmentType()))
                .map(KpiAdjustment::getPoints).reduce(BigDecimal.ZERO, BigDecimal::add)
                .min(version.getBonusCap());
        int missingReports = missingMandatoryReports(data);
        int seriousOverdue = data.seriousOverdueCases().size();
        BigDecimal totalPenalty = totalPenalty(appliedAdjustments, penalties,
                Map.of("P01", missingReports, "P02", seriousOverdue));
        BigDecimal rawFinal = KpiScoringPolicy.clampScore(rawBase.add(bonus, MC).subtract(totalPenalty, MC),
                version.getScoreScale());

        String rawClassification = KpiScoringPolicy.classification(rawFinal, classRules);
        String finalClassification = applyGates(rawClassification, appliedAdjustments, unit, data, missingReports,
                classRules, gates);
        BigDecimal dataQuality = dataQualityRate(data);
        boolean incomplete = details.stream().anyMatch(item -> item.resultStatus() == ResultStatus.MISSING_DATA
                || item.resultStatus() == ResultStatus.FAILED_VALIDATION);
        RunStatus runStatus = incomplete || dataQuality.compareTo(version.getDataQualityFinalThreshold()) < 0
                ? RunStatus.PROVISIONAL : RunStatus.DRAFT;
        List<Warning> warnings = aggregateWarnings(details, data, missingReports, appliedAdjustments,
                canViewSensitive);
        List<AdjustmentAudit> adjustmentAudit = adjustmentAudit(appliedAdjustments, canViewSensitive);
        BigDecimal reportRate = details.stream().filter(item -> "REP01".equals(item.kpiCode()))
                .filter(item -> item.resultStatus() == ResultStatus.CALCULATED && item.denominator() != null
                        && item.denominator().signum() > 0)
                .map(item -> item.numerator().divide(item.denominator(), MC)).findFirst().orElse(null);

        Long activeMemberCount = period.periodEnd().isBefore(data.asOf()) ? null : (long) data.members().size();
        return new UnitResult(runId, unit.getId(), unit.getCode(), unit.getName(), activeMemberCount, runStatus,
                dataQuality, rawBase, bonus, totalPenalty, rawFinal, rawClassification,
                finalClassification, null, false, reportRate,
                groups, details, warnings, adjustmentAudit);
    }

    private UnitData load(UnionUnit unit, Period period, Instant cutoff, Set<String> excludedKeys,
                          Map<String, SlaRule> slaByCode, Map<LocalDate, Boolean> calendarOverrides) {
        Long unitId = unit.getId();
        LocalDate cutoffDate = LocalDate.ofInstant(cutoff, BUSINESS_ZONE);
        LocalDate periodAsOf = cutoffDate.isAfter(period.periodEnd()) ? period.periodEnd() : cutoffDate;
        // The whole active roster, not only union members: DATA04 needs the non-members as its denominator.
        var employees = members.findAll(Specs.nullSafe(Specs.allOf(
                        Specs.unitScope(unitId), Specs.eq("employmentStatus", EmploymentStatus.ACTIVE))))
                .stream().filter(item -> beforeCutoff(item, cutoff))
                .filter(item -> item.getStartWorkDate() == null || !item.getStartWorkDate().isAfter(periodAsOf))
                .filter(item -> included(excludedKeys, "DOAN_VIEN", item.getEmployeeCode())).toList();
        var changes = memberChanges.findAll(Specs.nullSafe(Specs.allOf(
                        Specs.unitScopeVia("member", unitId), between("effectiveDate", period))))
                .stream().filter(item -> beforeCutoff(item, cutoff))
                .filter(item -> item.getEffectiveDate() != null && !item.getEffectiveDate().isAfter(periodAsOf))
                .filter(item -> item.getMember() != null && included(excludedKeys, "DOAN_VIEN",
                        item.getMember().getEmployeeCode())).toList();
        var reportRows = reports.findAll(Specs.nullSafe(Specs.allOf(
                        Specs.unitScope(unitId), between("reportMonth", period))))
                .stream().filter(item -> beforeCutoff(item, cutoff))
                .filter(item -> item.getReportMonth() != null && !item.getReportMonth().isAfter(periodAsOf))
                .filter(item -> item.getStatus() == ReportStatus.SUBMITTED
                        || item.getStatus() == ReportStatus.APPROVED)
                .filter(item -> included(excludedKeys, "BAO_CAO_DINH_KY",
                        unit.getCode() + ":" + YearMonth.from(item.getReportMonth()))).toList();
        var careRows = welfare.findAll(Specs.nullSafe(Specs.allOf(
                        Specs.unitScope(unitId), Specs.onOrBefore("eventDate", period.periodEnd()))))
                .stream().filter(item -> beforeCutoff(item, cutoff))
                .filter(item -> item.getEventDate() != null && !item.getEventDate().isAfter(periodAsOf))
                .filter(item -> !item.getEventDate().isBefore(period.periodStart())
                        || item.getStatus() != WorkStatus.COMPLETED)
                .filter(item -> included(excludedKeys, "CHAM_SOC_NLD", item.getRecordCode()))
                .filter(item -> item.getStatus() != WorkStatus.CANCELLED || !present(item.getCancellationReason())).toList();
        var grievanceRows = cases.findAll(Specs.nullSafe(Specs.allOf(
                        Specs.unitScope(unitId), Specs.onOrBefore("receivedDate", period.periodEnd()))))
                .stream().filter(item -> beforeCutoff(item, cutoff))
                .filter(item -> item.getReceivedDate() != null && !item.getReceivedDate().isAfter(periodAsOf))
                .filter(item -> included(excludedKeys, "SO_KIEN_NGHI", item.getCaseCode())).toList();
        var activityRows = activities.findAll(Specs.nullSafe(Specs.allOf(
                        Specs.unitScope(unitId), between("eventDate", period))))
                .stream().filter(item -> beforeCutoff(item, cutoff))
                .filter(item -> item.getEventDate() != null && !item.getEventDate().isAfter(periodAsOf))
                .filter(item -> included(excludedKeys, "HOAT_DONG", item.getActivityCode()))
                .filter(item -> item.getStatus() == ActivityStatus.PLANNED
                        || item.getStatus() == ActivityStatus.CANCELLED && !present(item.getCancellationReason())
                        || item.getStatus() == ActivityStatus.APPROVED
                        || item.getStatus() == ActivityStatus.IN_PROGRESS
                        || item.getStatus() == ActivityStatus.COMPLETED).toList();
        var financeRows = finance.findAll(Specs.nullSafe(Specs.allOf(
                        Specs.unitScope(unitId), between("transactionDate", period))))
                .stream().filter(item -> beforeCutoff(item, cutoff))
                .filter(item -> item.getTransactionDate() != null && !item.getTransactionDate().isAfter(periodAsOf))
                .filter(item -> included(excludedKeys, "TAI_CHINH_CD", item.getEntryCode())).toList();
        boolean governanceSourceExcluded = !included(excludedKeys, "DM_CONG_DOAN", unit.getCode());
        return new UnitData(employees, changes, reportRows, careRows, grievanceRows, activityRows, financeRows,
                evidenceIndex.welfareWithDocuments(ids(careRows, WelfareRecord::getId)),
                evidenceIndex.activitiesWithMedia(ids(activityRows, UnionActivity::getId)),
                evidenceIndex.financeWithDocuments(ids(financeRows, FinanceEntry::getId)),
                careWithFinanceEntry(careRows),
                cutoff, period, slaByCode, calendarOverrides, governanceSourceExcluded);
    }

    /**
     * Care records whose approval produced the matching finance entry. FinanceService derives that entry
     * code from the care record id, so the reconciliation is deterministic without a foreign key.
     */
    private Set<Long> careWithFinanceEntry(List<WelfareRecord> rows) {
        Map<String, Long> byCode = new HashMap<>();
        rows.stream().filter(item -> item.getId() != null)
                .forEach(item -> byCode.putIfAbsent(welfareEntryCode(item.getId()), item.getId()));
        if (byCode.isEmpty()) return Set.of();
        return finance.findAll(Specs.nullSafe(Specs.<FinanceEntry>in("entryCode", byCode.keySet()))).stream()
                .map(item -> byCode.get(item.getEntryCode()))
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    static String welfareEntryCode(Long welfareRecordId) {
        return "PC-CL-" + welfareRecordId;
    }

    private <T> Set<Long> ids(List<T> rows, Function<T, Long> id) {
        return rows.stream().map(id).filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());
    }

    private boolean noOccurrenceEligible(String kpiCode, Period period, UnitData data) {
        return switch (kpiCode) {
            case "DATA02" -> data.memberChanges().isEmpty();
            case "DATA03" -> data.employees().isEmpty();
            case "REP02" -> data.reports().isEmpty();
            case "CARE01", "CARE03", "CARE04" -> data.periodWelfare().isEmpty();
            case "CARE02" -> data.dueWelfare().isEmpty();
            case "GRV01", "GRV04" -> data.periodCases(period).isEmpty();
            case "GRV02" -> data.dueCases(period).isEmpty();
            case "GRV03" -> data.closedCases(period).isEmpty();
            case "ACT01", "ACT02", "ACT03", "ACT04" -> data.activities().isEmpty();
            case "FIN01" -> data.finance().isEmpty();
            case "FIN02" -> data.periodWelfare().isEmpty();
            case "FIN03" -> data.activities().isEmpty();
            default -> false;
        };
    }

    private Detail detail(String runId, UnionUnit unit, Period period, KpiDefinition definition,
                          UnitData data, boolean canViewSensitive,
                          Map<String, KpiNoOccurrenceConfirmation> confirmedNoOccurrenceSources) {
        String resultId = runId + ":" + definition.getKpiCode();
        Metric metric = metric(definition.getKpiCode(), unit, period, data);
        KpiNoOccurrenceConfirmation confirmation = confirmedNoOccurrenceSources.get(definition.getKpiCode());
        boolean confirmedNoOccurrence = metric.denominator() != null && metric.denominator().signum() == 0
                && definition.isNaAllowed() && !definition.isMandatory()
                && noOccurrenceEligible(definition.getKpiCode(), period, data)
                && confirmation != null && sameModule(confirmation.getSourceModule(), definition.getSourceModule())
                && present(confirmation.getReconciliationSourceModule())
                && !sameModule(confirmation.getReconciliationSourceModule(), confirmation.getSourceModule());
        if (confirmedNoOccurrence) {
            Metric confirmationMetric = complete(BigDecimal.ONE, BigDecimal.ONE,
                    "Xác nhận không phát sinh đã được đối soát nguồn độc lập và phê duyệt.",
                    List.of(new SourceRef("KPI_NO_OCCURRENCE", String.valueOf(confirmation.getId()),
                            "no-occurrence", true, true)), false);
            List<Evidence> confirmationEvidence = evidence(resultId, confirmationMetric, canViewSensitive);
            return new Detail(resultId, definition.getKpiCode(), definition.getGroupCode(), definition.getName(),
                    definition.getWeight(), BigDecimal.ZERO, BigDecimal.ZERO, definition.getTargetValue(), null, BigDecimal.ZERO,
                    BigDecimal.ZERO, ResultStatus.NA,
                    "Không phát sinh đã được đối soát nguồn độc lập và phê duyệt; KPI được loại khỏi trọng số.",
                    List.of(), confirmationEvidence, confirmationEvidence.size());
        }

        List<Evidence> evidence = evidence(resultId, metric, canViewSensitive);
        if (metric.failedValidation()) {
            Warning warning = new Warning("FAILED_VALIDATION", WarningSeverity.CRITICAL,
                    metric.explanation(), "Sửa hoặc xác minh lại bản ghi nguồn trước khi tính lại KPI.",
                    null, definition.getSourceModule(), null, metric.sensitive());
            return new Detail(resultId, definition.getKpiCode(), definition.getGroupCode(), definition.getName(),
                    definition.getWeight(), metric.numerator(), metric.denominator(), definition.getTargetValue(),
                    null, definition.getWeight(), BigDecimal.ZERO, ResultStatus.FAILED_VALIDATION,
                    metric.explanation(), List.of(warning), evidence, evidence.size());
        }
        if (!metric.complete() || metric.denominator() == null || metric.denominator().signum() == 0) {
            BigDecimal eligible = definition.getWeight();
            boolean mayBecomeNa = metric.denominator() != null && metric.denominator().signum() == 0
                    && definition.isNaAllowed() && !definition.isMandatory()
                    && noOccurrenceEligible(definition.getKpiCode(), period, data);
            String message = mayBecomeNa
                    ? "Chưa có xác nhận không phát sinh đã đối soát nguồn độc lập và phê duyệt."
                    : metric.explanation();
            Warning warning = new Warning("MISSING_DATA", WarningSeverity.WARNING, message,
                    "Bổ sung nguồn dữ liệu/đối soát bắt buộc trước khi chốt kỳ.", null,
                    definition.getSourceModule(), null, metric.sensitive());
            return new Detail(resultId, definition.getKpiCode(), definition.getGroupCode(), definition.getName(),
                    definition.getWeight(), metric.numerator(), metric.denominator(), definition.getTargetValue(), null, eligible,
                    BigDecimal.ZERO, ResultStatus.MISSING_DATA, message, List.of(warning), evidence, evidence.size());
        }

        BigDecimal normalized = KpiScoringPolicy.normalized(definition, metric.numerator(), metric.denominator());
        if (normalized == null) {
            Warning warning = new Warning("MISSING_TARGET", WarningSeverity.WARNING,
                    "Thiếu target/max_allowed hợp lệ trong phiên bản KPI.",
                    "Cấu hình và phê duyệt ngưỡng cho phiên bản đang hiệu lực.", null,
                    definition.getSourceModule(), null, false);
            return new Detail(resultId, definition.getKpiCode(), definition.getGroupCode(), definition.getName(),
                    definition.getWeight(), metric.numerator(), metric.denominator(), definition.getTargetValue(), null,
                    definition.getWeight(), BigDecimal.ZERO,
                    ResultStatus.MISSING_DATA, warning.message(), List.of(warning), evidence, evidence.size());
        }
        BigDecimal points = KpiScoringPolicy.earnedPoints(normalized, definition.getWeight());
        List<Warning> validationWarnings = metric.refs().stream()
                .filter(ref -> !ref.structurallyValid())
                .map(ref -> new Warning("INVALID_SOURCE_RECORD", WarningSeverity.WARNING,
                        "Bản ghi nguồn thiếu trường bắt buộc hoặc không nhất quán.",
                        "Mở bản ghi chứng minh, bổ sung dữ liệu rồi tính lại.", null, ref.module(),
                        metric.sensitive() && !canViewSensitive ? null : ref.recordId(),
                        metric.sensitive() && !canViewSensitive))
                .toList();
        return new Detail(resultId, definition.getKpiCode(), definition.getGroupCode(), definition.getName(),
                definition.getWeight(), metric.numerator(), metric.denominator(), definition.getTargetValue(),
                normalized, definition.getWeight(), points, ResultStatus.CALCULATED,
                metric.explanation(), validationWarnings, evidence, evidence.size());
    }

    private Metric metric(String code, UnionUnit unit, Period period, UnitData data) {
        if (populations != null && period.periodType() == PeriodType.YEAR
                && Set.of("DATA01", "DATA03", "DATA04").contains(code)) {
            var population = populations.approved(unit.getId(), period.year());
            if (population != null) {
                var rows = population.members().stream().filter(p -> switch (code) {
                    case "DATA01" -> p.unionMember();
                    case "DATA03" -> p.identityDeclared();
                    default -> true;
                }).toList();
                Predicate<KpiPopulationService.Person> valid = p -> switch (code) {
                    case "DATA01" -> p.profileComplete();
                    case "DATA03" -> p.identityUnique();
                    default -> p.unionMember();
                };
                return complete(BigDecimal.valueOf(rows.stream().filter(valid).count()), BigDecimal.valueOf(rows.size()),
                        "Tính từ danh sách nhân sự cuối năm đã phê duyệt, bản " + population.revision(),
                        List.of(new SourceRef("KPI_POPULATION", String.valueOf(population.id()), "population", true, true)), false);
            }
        }
        if ((code.startsWith("CARE") && data.welfare().stream().anyMatch(w -> w.getStatus() == WorkStatus.CANCELLED))
                || (code.startsWith("ACT") && data.activities().stream().anyMatch(a -> a.getStatus() == ActivityStatus.CANCELLED))) {
            return failed(null, null, "Có hồ sơ hủy thiếu lý do; cần đối soát trước khi tính điểm.", List.of(), false);
        }
        return switch (code) {
            case "GOV01" -> governanceLegalProfile(unit, data);
            case "GOV02" -> governanceLeadership(unit, data);
            case "DATA01" -> memberCompleteness(data);
            case "DATA02" -> memberChangeTimeliness(data);
            case "DATA03" -> identityUniqueness(data);
            case "DATA04" -> unionParticipation(data);
            case "REP01" -> reportTimeliness(unit, data);
            case "REP02" -> reportContent(data.reports());
            case "CARE01" -> careIntakeTimeliness(data);
            case "CARE02" -> careOnTime(data);
            case "CARE03" -> careClosure(data);
            case "CARE04" -> careCompliance(data.policyWelfare());
            case "CARE05" -> careCoverage(unit, period, data);
            case "GRV01" -> grievanceAcknowledgement(data, period);
            case "GRV02" -> grievanceSla(data.dueCases(period));
            case "GRV03" -> grievanceClosure(data.closedCases(period));
            case "GRV04" -> grievanceResolution(data.periodCases(period), data.periodAsOf());
            case "ACT01" -> activityCompletion(data.activities());
            case "ACT02" -> activityParticipation(data.activities());
            case "ACT03" -> activityReport(data);
            case "ACT04" -> activitySatisfaction(data.activities());
            case "FIN01" -> financeDocuments(data);
            case "FIN02" -> careFinanceReconciliation(data);
            case "FIN03" -> budgetCompliance(data.activities());
            default -> throw new IllegalStateException("Không có calculator cho " + code);
        };
    }

    private Metric careCoverage(UnionUnit unit, Period period, UnitData data) {
        var population = populations == null ? null : populations.approved(unit.getId(), period.year());
        if (population == null) return missing("Cần danh sách nhân sự cuối năm được phê duyệt để xác định mẫu số chăm lo.");
        if (data.periodWelfare().stream().anyMatch(w -> w.getStatus() == WorkStatus.CANCELLED
                && !present(w.getCancellationReason()))) {
            return failed(null, null, "Hồ sơ chăm lo hủy nhưng thiếu lý do; cần đối soát trước khi tính điểm.",
                    refs("CHAM_SOC_NLD", data.periodWelfare(), WelfareRecord::getId, w -> false), false);
        }
        Set<Long> roster = population.members().stream().map(KpiPopulationService.Person::memberId).collect(Collectors.toSet());
        var birthdays = data.periodWelfare().stream().filter(w -> w.getWelfareType() == WelfareType.BIRTHDAY).toList();
        if (birthdays.stream().anyMatch(w -> w.getMemberId() == null || !roster.contains(w.getMemberId())))
            return failed(null, BigDecimal.valueOf(roster.size()), "Hồ sơ sinh nhật chưa liên kết nhân sự trong danh sách cuối năm.", refs("CHAM_SOC_NLD", birthdays, WelfareRecord::getId, w -> false), false);
        Predicate<WelfareRecord> completed = w -> w.getStatus() == WorkStatus.COMPLETED
                && w.getCompletedAt() != null && !w.getCompletedAt().isAfter(data.cutoff())
                && !w.getCompletedAt().atZone(BUSINESS_ZONE).toLocalDate().isAfter(data.periodAsOf());
        long birthdayDone = birthdays.stream().filter(completed).map(WelfareRecord::getMemberId).distinct().count();
        var other = data.periodWelfare().stream().filter(w -> w.getWelfareType() != WelfareType.BIRTHDAY).toList();
        List<SourceRef> references = new ArrayList<>(refs("CHAM_SOC_NLD", other, WelfareRecord::getId, completed));
        for (var person : population.members())
            references.add(new SourceRef("KPI_POPULATION", population.id() + ":" + person.memberId(), "population", false, true, EvidenceRole.DENOMINATOR));
        Set<Long> counted = new HashSet<>();
        for (var birthday : birthdays.stream().sorted(Comparator.comparing(WelfareRecord::getId)).toList()) {
            boolean countedNow = completed.test(birthday) && counted.add(birthday.getMemberId());
            references.add(new SourceRef("CHAM_SOC_NLD", String.valueOf(birthday.getId()), "welfare", countedNow, true,
                    countedNow ? EvidenceRole.NUMERATOR : EvidenceRole.EXCLUDED));
        }
        return complete(BigDecimal.valueOf(birthdayDone + other.stream().filter(completed).count()),
                BigDecimal.valueOf(roster.size() + other.size()),
                "Sinh nhật đếm người duy nhất; năm nhóm chăm lo khác đếm từng sự việc. Mẫu số = nhân sự cuối năm + sự việc khác.",
                references, false);
    }

    private Metric governanceLegalProfile(UnionUnit unit, UnitData data) {
        if (data.governanceSourceExcluded()) {
            return missing("Hồ sơ pháp lý mẫu khởi tạo đã bị loại khỏi nguồn KPI; cần nhập hồ sơ CĐCS thật.");
        }
        LocalDate asOf = data.periodAsOf();
        boolean valid = present(unit.getDecisionNumber()) && unit.getLegalStatus() == LegalStatus.ACTIVE
                && unit.getTermStart() != null && unit.getTermEnd() != null
                && !unit.getTermStart().isAfter(asOf) && !unit.getTermEnd().isBefore(asOf);
        return complete(count(valid), BigDecimal.ONE,
                valid ? "Có số quyết định, trạng thái pháp lý hiệu lực và nhiệm kỳ phủ ngày chốt."
                        : "Thiếu số quyết định, trạng thái pháp lý hoặc nhiệm kỳ không phủ ngày chốt.",
                refs("DM_CONG_DOAN", unit.getId(), valid), false);
    }

    private Metric governanceLeadership(UnionUnit unit, UnitData data) {
        if (data.governanceSourceExcluded()) {
            return missing("Dữ liệu BCH mẫu khởi tạo đã bị loại khỏi nguồn KPI; cần nhập BCH và đầu mối thật.");
        }
        boolean valid = present(unit.getChairperson()) && present(unit.getContactPerson());
        return complete(count(valid), BigDecimal.ONE,
                valid ? "Đã khai chủ tịch/BCH và đầu mối liên hệ của CĐCS."
                        : "Thiếu chủ tịch/BCH hoặc đầu mối liên hệ của CĐCS.",
                refs("DM_CONG_DOAN", unit.getId(), valid), false);
    }

    private Metric memberCompleteness(UnitData data) {
        if (data.period().periodEnd().isBefore(data.asOf())) {
            return missing("Thiếu snapshot đoàn viên/HR tại ngày chốt của kỳ lịch sử; không dùng trạng thái hiện tại để chấm ngược kỳ.");
        }
        List<Member> rows = data.members();
        Predicate<Member> isComplete = this::isMemberComplete;
        long complete = rows.stream().filter(isComplete).count();
        return complete(BigDecimal.valueOf(complete), BigDecimal.valueOf(rows.size()),
                complete + "/" + rows.size() + " hồ sơ đoàn viên đang hoạt động đủ trường cốt lõi.",
                refs("DOAN_VIEN", rows, Member::getId, isComplete), false);
    }

    private Metric memberChangeTimeliness(UnitData data) {
        SlaRule rule = data.slaRule(MEMBER_CHANGE_SLA);
        List<MemberChange> rows = data.memberChanges();
        if (rule == null) {
            return partial(null, null, "Phiên bản KPI chưa có SLA MEMBER_CHANGE.",
                    refs("DOAN_VIEN", rows, MemberChange::getId, ignored -> false), false);
        }
        Predicate<MemberChange> isTimely = item -> item.getEffectiveDate() != null && item.getCreatedAt() != null
                && !item.getCreatedAt().atZone(BUSINESS_ZONE).toLocalDate()
                .isAfter(slaDeadline(item.getEffectiveDate(), rule, data.calendarOverrides()));
        long timely = rows.stream().filter(isTimely).count();
        return complete(BigDecimal.valueOf(timely), BigDecimal.valueOf(rows.size()),
                timely + "/" + rows.size() + " biến động được ghi nhận trong "
                        + rule.getDurationValue() + " ngày làm việc kể từ ngày phát sinh.",
                refs("DOAN_VIEN", rows, MemberChange::getId, isTimely), false);
    }

    /**
     * Duplicate identities inside one unit. The employee code is unique by database constraint, so the check
     * that is worth scoring is the one the database does not enforce: the same CCCD or phone on two people.
     */
    private Metric identityUniqueness(UnitData data) {
        List<Member> declared = data.employees().stream()
                .filter(item -> present(item.getNationalId()) || present(item.getPhone())).toList();
        Map<String, Long> nationalIds = normalizedCounts(data.employees(), Member::getNationalId);
        Map<String, Long> phones = normalizedCounts(data.employees(), Member::getPhone);
        Predicate<Member> unique = item -> uniqueValue(nationalIds, item.getNationalId())
                && uniqueValue(phones, item.getPhone());
        long distinct = declared.stream().filter(unique).count();
        return complete(BigDecimal.valueOf(distinct), BigDecimal.valueOf(declared.size()),
                distinct + "/" + declared.size() + " hồ sơ có CCCD và số điện thoại không trùng trong đơn vị.",
                refs("DOAN_VIEN", declared, Member::getId, unique), false);
    }

    private Metric unionParticipation(UnitData data) {
        if (data.period().periodEnd().isBefore(data.asOf())) {
            return missing("Thiếu snapshot nhân sự tại ngày chốt của kỳ lịch sử; không dùng trạng thái hiện tại để chấm ngược kỳ.");
        }
        List<Member> employees = data.employees();
        Predicate<Member> joined = item -> item.getMembershipStatus() == MembershipStatus.MEMBER;
        long count = employees.stream().filter(joined).count();
        return complete(BigDecimal.valueOf(count), BigDecimal.valueOf(employees.size()),
                count + "/" + employees.size() + " NLĐ đang làm việc đã là đoàn viên.",
                refs("DOAN_VIEN", employees, Member::getId, joined), false);
    }

    private Map<String, Long> normalizedCounts(List<Member> rows, Function<Member, String> field) {
        return rows.stream().map(field).filter(this::present)
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    private boolean uniqueValue(Map<String, Long> counts, String value) {
        return !present(value) || counts.getOrDefault(value.trim().toLowerCase(Locale.ROOT), 0L) <= 1;
    }

    private Metric reportTimeliness(UnionUnit unit, UnitData data) {
        SlaRule rule = data.slaRule(REPORT_SLA);
        if (rule == null) {
            return partial(null, null, "Phiên bản KPI chưa có SLA REPORT_SUBMISSION.",
                    refs("BAO_CAO_DINH_KY", data.reports(), MonthlyReport::getId, ignored -> false), false);
        }
        List<YearMonth> dueMonths = dueReportMonths(data);
        if (dueMonths.isEmpty()) {
            // Reading the dashboard mid-period: nothing is late yet, but nothing is proven on time either.
            return partial(BigDecimal.ZERO, BigDecimal.ZERO,
                    "Chưa có tháng nào trong kỳ đến hạn nộp báo cáo tính đến ngày chốt.", List.of(), false);
        }
        List<MonthlyReport> invalidReports = data.reports().stream()
                .filter(item -> item.getReportMonth() != null
                        && dueMonths.contains(YearMonth.from(item.getReportMonth())))
                .filter(item -> item.getSubmittedAt() == null).toList();
        if (!invalidReports.isEmpty()) {
            return failed(null, BigDecimal.valueOf(dueMonths.size()),
                    invalidReports.size() + " báo cáo đã ở trạng thái nộp/duyệt nhưng thiếu submitted_at.",
                    refs("BAO_CAO_DINH_KY", invalidReports, MonthlyReport::getId, ignored -> false), false);
        }
        Predicate<MonthlyReport> isOnTime = item -> {
            if (item.getSubmittedAt() == null || item.getReportMonth() == null
                    || (item.getStatus() != ReportStatus.SUBMITTED && item.getStatus() != ReportStatus.APPROVED)) return false;
            YearMonth month = YearMonth.from(item.getReportMonth());
            if (!dueMonths.contains(month)) return false;
            LocalDate deadline = reportDeadline(month, rule, data.calendarOverrides());
            return !item.getSubmittedAt().atZone(BUSINESS_ZONE).toLocalDate().isAfter(deadline);
        };
        Map<YearMonth, MonthlyReport> reportsByMonth = data.reports().stream()
                .filter(item -> item.getReportMonth() != null)
                .collect(Collectors.toMap(item -> YearMonth.from(item.getReportMonth()), Function.identity(),
                        (left, right) -> left.getUpdatedAt() != null && right.getUpdatedAt() != null
                                && left.getUpdatedAt().isAfter(right.getUpdatedAt()) ? left : right));
        long onTime = data.reports().stream().filter(isOnTime).count();
        List<SourceRef> dueEvidence = dueMonths.stream().map(month -> {
            MonthlyReport report = reportsByMonth.get(month);
            if (report != null) {
                return new SourceRef("BAO_CAO_DINH_KY", String.valueOf(report.getId()),
                        "monthly-report", isOnTime.test(report), true);
            }
            return new SourceRef("BAO_CAO_DINH_KY", unit.getId() + ":" + month,
                    "report-obligation", false, true);
        }).toList();
        return complete(BigDecimal.valueOf(onTime), BigDecimal.valueOf(dueMonths.size()),
                onTime + "/" + dueMonths.size() + " báo cáo tháng được nộp trong SLA cấu hình.",
                dueEvidence, false);
    }

    private Metric reportContent(List<MonthlyReport> rows) {
        Predicate<MonthlyReport> hasContent = item -> present(item.getPlanNextMonth())
                && present(item.getSupportRequest());
        long filled = rows.stream().filter(hasContent).count();
        return complete(BigDecimal.valueOf(filled), BigDecimal.valueOf(rows.size()),
                filled + "/" + rows.size() + " báo cáo đã nộp có kế hoạch kỳ sau và đề xuất hỗ trợ.",
                refs("BAO_CAO_DINH_KY", rows, MonthlyReport::getId, hasContent), false);
    }

    private Metric careIntakeTimeliness(UnitData data) {
        SlaRule rule = data.slaRule(CARE_INTAKE_SLA);
        List<WelfareRecord> rows = data.periodWelfare();
        if (rule == null) {
            return partial(null, null, "Phiên bản KPI chưa có SLA CARE_NORMAL.",
                    refs("CHAM_SOC_NLD", rows, WelfareRecord::getId, ignored -> false), true);
        }
        Predicate<WelfareRecord> onTime = item -> item.getCreatedAt() != null && item.getEventDate() != null
                && !item.getCreatedAt().atZone(BUSINESS_ZONE).toLocalDate()
                .isAfter(slaDeadline(item.getEventDate(), rule, data.calendarOverrides()));
        long recorded = rows.stream().filter(onTime).count();
        return complete(BigDecimal.valueOf(recorded), BigDecimal.valueOf(rows.size()),
                recorded + "/" + rows.size() + " hồ sơ chăm lo được ghi nhận trong "
                        + rule.getDurationValue() + " ngày làm việc kể từ ngày phát sinh.",
                refs("CHAM_SOC_NLD", rows, WelfareRecord::getId, onTime), true);
    }

    private Metric careOnTime(UnitData data) {
        List<WelfareRecord> rows = data.dueWelfare();
        Predicate<WelfareRecord> isOnTime = item -> item.getStatus() == WorkStatus.COMPLETED
                && item.getCompletedAt() != null && item.getDeadline() != null
                && !item.getCompletedAt().atZone(BUSINESS_ZONE).toLocalDate().isAfter(item.getDeadline());
        long completed = rows.stream().filter(isOnTime).count();
        return complete(BigDecimal.valueOf(completed), BigDecimal.valueOf(rows.size()),
                completed + "/" + rows.size() + " hồ sơ chăm lo đã đến hạn được hoàn tất trước hạn.",
                refs("CHAM_SOC_NLD", rows, WelfareRecord::getId, isOnTime), true);
    }

    private Metric careClosure(UnitData data) {
        List<WelfareRecord> rows = data.completedWelfare();
        Predicate<WelfareRecord> isClosed = item -> item.getPolicyId() != null
                && item.getDocumentStatus() == DocumentStatus.COMPLETE
                && item.getReceiptStatus() == DocumentStatus.COMPLETE && Boolean.TRUE.equals(item.getHasImage())
                && data.welfareWithFiles().contains(item.getId());
        long closed = rows.stream().filter(isClosed).count();
        return complete(BigDecimal.valueOf(closed), BigDecimal.valueOf(rows.size()),
                closed + "/" + rows.size() + " hồ sơ hoàn thành có chính sách, chứng từ, biên nhận và tệp đính kèm thật.",
                refs("CHAM_SOC_NLD", rows, WelfareRecord::getId, isClosed), true);
    }

    private Metric careCompliance(List<WelfareRecord> rows) {
        Predicate<WelfareRecord> isCompliant = item -> item.getAmount() != null
                && item.getStandardAmount() != null && item.getAmount().compareTo(item.getStandardAmount()) == 0;
        long compliant = rows.stream().filter(isCompliant).count();
        return complete(BigDecimal.valueOf(compliant), BigDecimal.valueOf(rows.size()),
                compliant + "/" + rows.size() + " hồ sơ có chính sách được chi đúng định mức đã duyệt.",
                refs("CHAM_SOC_NLD", rows, WelfareRecord::getId, isCompliant), true);
    }

    private Metric careFinanceReconciliation(UnitData data) {
        List<WelfareRecord> rows = data.approvedWelfare();
        Predicate<WelfareRecord> reconciled = item -> data.careWithFinanceEntry().contains(item.getId());
        long matched = rows.stream().filter(reconciled).count();
        return complete(BigDecimal.valueOf(matched), BigDecimal.valueOf(rows.size()),
                matched + "/" + rows.size() + " hồ sơ chăm lo đã duyệt có giao dịch chi tương ứng trong sổ tài chính.",
                refs("CHAM_SOC_NLD", rows, WelfareRecord::getId, reconciled), true);
    }

    private Metric grievanceAcknowledgement(UnitData data, Period period) {
        SlaRule rule = data.slaRule(GRIEVANCE_ACK_SLA);
        List<LaborCase> rows = data.periodCases(period);
        if (rule == null) {
            return partial(null, null, "Phiên bản KPI chưa có SLA GRV_ACK.",
                    refs("SO_KIEN_NGHI", rows, LaborCase::getId, ignored -> false), true);
        }
        Predicate<LaborCase> recorded = item -> item.getCreatedAt() != null && item.getReceivedDate() != null
                && !item.getCreatedAt().atZone(BUSINESS_ZONE).toLocalDate()
                .isAfter(slaDeadline(item.getReceivedDate(), rule, data.calendarOverrides()));
        long inSla = rows.stream().filter(recorded).count();
        return complete(BigDecimal.valueOf(inSla), BigDecimal.valueOf(rows.size()),
                inSla + "/" + rows.size() + " kiến nghị được ghi sổ trong "
                        + rule.getDurationValue() + " ngày làm việc kể từ ngày tiếp nhận.",
                refs("SO_KIEN_NGHI", rows, LaborCase::getId, recorded), true);
    }

    private Metric grievanceSla(List<LaborCase> rows) {
        Predicate<LaborCase> completedOnTime = item -> item.getStatus() == CaseStatus.CLOSED
                && item.getApprovedAt() != null && item.getDeadline() != null
                && !item.getApprovedAt().atZone(BUSINESS_ZONE).toLocalDate().isAfter(item.getDeadline());
        long onTime = rows.stream().filter(completedOnTime).count();
        return complete(BigDecimal.valueOf(onTime), BigDecimal.valueOf(rows.size()),
                onTime + "/" + rows.size() + " kiến nghị đã đến hạn được đóng trong hạn xử lý.",
                refs("SO_KIEN_NGHI", rows, LaborCase::getId, completedOnTime), true);
    }

    private Metric grievanceClosure(List<LaborCase> rows) {
        Predicate<LaborCase> validClosure = this::isValidClosedCase;
        long valid = rows.stream().filter(validClosure).count();
        return complete(BigDecimal.valueOf(valid), BigDecimal.valueOf(rows.size()),
                valid + "/" + rows.size() + " kiến nghị đã đóng có kết quả, ngày phản hồi và người duyệt đóng hợp lệ.",
                refs("SO_KIEN_NGHI", rows, LaborCase::getId, validClosure), true);
    }

    private Metric grievanceResolution(List<LaborCase> rows, LocalDate asOf) {
        Predicate<LaborCase> resolved = item -> item.getStatus() == CaseStatus.CLOSED && item.getApprovedAt() != null
                && !item.getApprovedAt().atZone(BUSINESS_ZONE).toLocalDate().isAfter(asOf);
        long closed = rows.stream().filter(resolved).count();
        return complete(BigDecimal.valueOf(closed), BigDecimal.valueOf(rows.size()),
                closed + "/" + rows.size() + " kiến nghị phát sinh trong kỳ đã được giải quyết và đóng.",
                refs("SO_KIEN_NGHI", rows, LaborCase::getId, resolved), true);
    }

    private boolean isValidClosedCase(LaborCase item) {
        if (item.getReceivedDate() == null || !present(item.getResultText()) || item.getResponseDate() == null
                || !present(item.getApprovedBy()) || item.getApprovedAt() == null) return false;
        LocalDate approvedDate = item.getApprovedAt().atZone(BUSINESS_ZONE).toLocalDate();
        return !item.getResponseDate().isBefore(item.getReceivedDate())
                && !approvedDate.isBefore(item.getReceivedDate())
                && !approvedDate.isBefore(item.getResponseDate());
    }

    private Metric activityCompletion(List<UnionActivity> rows) {
        Predicate<UnionActivity> isCompleted = item -> item.getStatus() == ActivityStatus.COMPLETED;
        long completed = rows.stream().filter(isCompleted).count();
        return complete(BigDecimal.valueOf(completed), BigDecimal.valueOf(rows.size()),
                completed + "/" + rows.size() + " chương trình đã duyệt/đang chạy trong kỳ được hoàn thành.",
                refs("HOAT_DONG", rows, UnionActivity::getId, isCompleted), false);
    }

    private Metric activityParticipation(List<UnionActivity> rows) {
        long invited = rows.stream().mapToLong(item -> safeInt(item.getInvitedCount())).sum();
        long participants = rows.stream().mapToLong(item -> safeInt(item.getParticipantCount())).sum();
        return complete(BigDecimal.valueOf(participants), BigDecimal.valueOf(invited),
                participants + "/" + invited + " lượt tham gia trên số được mời.",
                refs("HOAT_DONG", rows, UnionActivity::getId,
                        item -> safeInt(item.getParticipantCount()) > 0), false);
    }

    private Metric activityReport(UnitData data) {
        List<UnionActivity> rows = data.completedActivities();
        Predicate<UnionActivity> validReport = item -> Boolean.TRUE.equals(item.getReportCompleted())
                && item.getDocumentStatus() == DocumentStatus.COMPLETE
                && data.activitiesWithMedia().contains(item.getId());
        long complete = rows.stream().filter(validReport).count();
        return complete(BigDecimal.valueOf(complete), BigDecimal.valueOf(rows.size()),
                complete + "/" + rows.size() + " chương trình đã hoàn thành có báo cáo, chứng từ và tệp minh chứng thật.",
                refs("HOAT_DONG", rows, UnionActivity::getId, validReport), false);
    }

    /**
     * Usefulness is captured on a 0-5 scale, so the metric is the average over that maximum rather than the
     * 1-5 rating direction, which would fold a zero and a one into the same score.
     */
    private Metric activitySatisfaction(List<UnionActivity> rows) {
        var scored = rows.stream().filter(item -> item.getUsefulnessScore() != null).toList();
        if (scored.isEmpty()) {
            return partial(null, BigDecimal.ZERO,
                    "Chưa có chương trình nào trong kỳ ghi nhận điểm hữu ích.",
                    refs("HOAT_DONG", rows, UnionActivity::getId, ignored -> false), false);
        }
        BigDecimal average = scored.stream().map(UnionActivity::getUsefulnessScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(scored.size()), MC);
        return complete(average, USEFULNESS_SCALE_MAX,
                "Điểm hữu ích trung bình " + KpiScoringPolicy.display(average, 2) + "/5 trên "
                        + scored.size() + " chương trình có ghi nhận.",
                refs("HOAT_DONG", scored, UnionActivity::getId, item -> true), false);
    }

    private Metric financeDocuments(UnitData data) {
        List<FinanceEntry> rows = data.finance();
        Predicate<FinanceEntry> hasDocuments = item -> item.getDocumentStatus() == DocumentStatus.COMPLETE
                && data.financeWithFiles().contains(item.getId());
        long complete = rows.stream().filter(hasDocuments).count();
        return complete(BigDecimal.valueOf(complete), BigDecimal.valueOf(rows.size()),
                complete + "/" + rows.size() + " giao dịch có trạng thái chứng từ đầy đủ và tệp đính kèm thật.",
                refs("TAI_CHINH_CD", rows, FinanceEntry::getId, hasDocuments), false);
    }

    private Metric budgetCompliance(List<UnionActivity> rows) {
        List<UnionActivity> budgeted = rows.stream().filter(item -> item.getPlannedBudget() != null
                && item.getPlannedBudget().signum() > 0 && item.getActualCost() != null).toList();
        Predicate<UnionActivity> withinBudget = item -> item.getActualCost().compareTo(item.getPlannedBudget()) <= 0;
        long within = budgeted.stream().filter(withinBudget).count();
        return complete(BigDecimal.valueOf(within), BigDecimal.valueOf(budgeted.size()),
                within + "/" + budgeted.size() + " chương trình có ngân sách chi không vượt dự toán đã duyệt.",
                refs("HOAT_DONG", budgeted, UnionActivity::getId, withinBudget), false);
    }

    private BigDecimal dataQualityRate(UnitData data) {
        List<WelfareRecord> periodWelfare = data.periodWelfare();
        List<LaborCase> relevantCases = data.relevantCases();
        long total = (long) data.members().size() + data.memberChanges().size() + data.reports().size()
                + periodWelfare.size() + relevantCases.size() + data.activities().size() + data.finance().size();
        if (total == 0) return BigDecimal.ZERO;

        long valid = data.members().stream().filter(this::isMemberComplete).count();
        valid += data.memberChanges().stream().filter(item -> item.getMember() != null
                && present(item.getChangeType()) && item.getEffectiveDate() != null
                && present(item.getDescription()) && present(item.getRecordedBy())).count();
        valid += data.reports().stream().filter(item -> item.getReportMonth() != null
                && present(item.getPreparedBy()) && item.getStatus() != null
                && ((item.getStatus() != ReportStatus.SUBMITTED && item.getStatus() != ReportStatus.APPROVED)
                || item.getSubmittedAt() != null)).count();
        valid += periodWelfare.stream().filter(item -> present(item.getRecordCode())
                && item.getWelfareType() != null && present(item.getBeneficiaryName())
                && item.getEventDate() != null && item.getStatus() != null && item.getAmount() != null
                && item.getAmount().signum() >= 0 && item.getDocumentStatus() != null
                && item.getReceiptStatus() != null && item.getHasImage() != null
                && (item.getStatus() != WorkStatus.COMPLETED
                || item.getCompletedAt() != null
                && item.getDocumentStatus() == DocumentStatus.COMPLETE
                && item.getReceiptStatus() == DocumentStatus.COMPLETE
                && Boolean.TRUE.equals(item.getHasImage()))).count();
        valid += relevantCases.stream().filter(item -> present(item.getCaseCode()) && item.getReceivedDate() != null
                && present(item.getRequesterName()) && present(item.getIssueGroup()) && item.getSeverity() != null
                && item.getStatus() != null && present(item.getDescription())
                && item.getAffectedPeople() != null && item.getAffectedPeople() > 0
                && (item.getStatus() != CaseStatus.CLOSED || isValidClosedCase(item))).count();
        valid += data.activities().stream().filter(item -> present(item.getActivityCode()) && present(item.getName())
                && item.getEventDate() != null && item.getStatus() != null && item.getPlannedBudget() != null
                && item.getPlannedBudget().signum() >= 0 && item.getActualCost() != null
                && item.getActualCost().signum() >= 0 && item.getInvitedCount() != null && item.getInvitedCount() >= 0
                && item.getParticipantCount() != null && item.getParticipantCount() >= 0
                && (item.getStatus() != ActivityStatus.COMPLETED || Boolean.TRUE.equals(item.getReportCompleted())
                && item.getDocumentStatus() == DocumentStatus.COMPLETE && present(item.getParticipantList()))).count();
        valid += data.finance().stream().filter(item -> present(item.getEntryCode())
                && item.getTransactionDate() != null && item.getEntryType() != null && present(item.getCategory())
                && item.getAmount() != null && item.getAmount().signum() >= 0
                && present(item.getDescription()) && item.getDocumentStatus() != null).count();
        return BigDecimal.valueOf(valid).divide(BigDecimal.valueOf(total), MC);
    }

    private boolean isMemberComplete(Member item) {
        return MemberSpecs.hasRequiredProfileFields(item);
    }

    private Metric missing(String explanation) {
        return partial(null, null, explanation, List.of(), false);
    }

    private Metric complete(BigDecimal numerator, BigDecimal denominator, String explanation,
                            List<SourceRef> refs, boolean sensitive) {
        return new Metric(numerator, denominator, true, false, explanation, refs, sensitive);
    }

    private Metric partial(BigDecimal numerator, BigDecimal denominator, String explanation,
                           List<SourceRef> refs, boolean sensitive) {
        return new Metric(numerator, denominator, false, false, explanation, refs, sensitive);
    }

    private Metric failed(BigDecimal numerator, BigDecimal denominator, String explanation,
                          List<SourceRef> refs, boolean sensitive) {
        return new Metric(numerator, denominator, false, true, explanation, refs, sensitive);
    }

    private List<GroupResult> groups(List<Detail> details, List<KpiDefinition> definitions) {
        return GROUP_ORDER.stream().map(groupCode -> {
            List<Detail> groupDetails = details.stream().filter(item -> groupCode.equals(item.groupCode())).toList();
            BigDecimal configuredWeight = definitions.stream().filter(item -> groupCode.equals(item.getGroupCode()))
                    .map(KpiDefinition::getWeight).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal eligibleWeight = groupDetails.stream().map(Detail::eligibleWeight)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal earnedPoints = groupDetails.stream().map(Detail::earnedPoints)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal score = eligibleWeight.signum() == 0 ? null
                    : earnedPoints.divide(eligibleWeight, MC).multiply(BigDecimal.valueOf(100), MC);
            ResultStatus status = groupDetails.stream().anyMatch(item -> item.resultStatus() == ResultStatus.FAILED_VALIDATION)
                    ? ResultStatus.FAILED_VALIDATION
                    : groupDetails.stream().anyMatch(item -> item.resultStatus() == ResultStatus.MISSING_DATA)
                    ? ResultStatus.MISSING_DATA
                    : groupDetails.stream().allMatch(item -> item.resultStatus() == ResultStatus.NA)
                    ? ResultStatus.NA : ResultStatus.CALCULATED;
            return new GroupResult(groupCode, GROUP_NAMES.get(groupCode), configuredWeight, eligibleWeight,
                    earnedPoints, score, status,
                    groupDetails);
        }).toList();
    }

    private List<Evidence> evidence(String resultId, Metric metric, boolean canViewSensitive) {
        List<Evidence> result = new ArrayList<>();
        int sequence = 1;
        for (SourceRef ref : metric.refs()) {
            boolean redacted = metric.sensitive() && !canViewSensitive;
            String visibleRecordId = redacted ? "REDACTED" : ref.recordId();
            String url = redacted ? null : "/kpi/evidence/" + ref.resourceType() + "/"
                    + ("population".equals(ref.resourceType()) ? ref.recordId().split(":")[0] : ref.recordId());
            ValidationStatus validationStatus = metric.complete() || metric.failedValidation()
                    ? (ref.structurallyValid() ? ValidationStatus.VALID : ValidationStatus.INVALID)
                    : ValidationStatus.PENDING;
            if (ref.roleOverride() != null) {
                result.add(new Evidence(resultId + ":R:" + sequence++, resultId, ref.module(), visibleRecordId,
                        ref.roleOverride(), url, null, validationStatus, redacted));
                continue;
            }
            result.add(new Evidence(resultId + ":D:" + sequence++, resultId, ref.module(), visibleRecordId,
                    EvidenceRole.DENOMINATOR, url, null, validationStatus, redacted));
            if (ref.numerator()) {
                result.add(new Evidence(resultId + ":N:" + sequence++, resultId, ref.module(), visibleRecordId,
                        EvidenceRole.NUMERATOR, url, null, validationStatus, redacted));
            }
        }
        return List.copyOf(result);
    }

    private List<Warning> aggregateWarnings(List<Detail> details, UnitData data, int missingReports,
                                            List<KpiAdjustment> appliedAdjustments,
                                            boolean canViewSensitive) {
        List<Warning> result = details.stream().flatMap(item -> item.warnings().stream()).collect(Collectors.toCollection(ArrayList::new));
        if (missingReports > 0) {
            result.add(new Warning("P01", WarningSeverity.CRITICAL,
                    missingReports + " báo cáo tháng bắt buộc chưa được nộp trong kỳ đã đến hạn.",
                    "Nộp báo cáo và gửi phê duyệt; điểm phạt được cấu hình theo phiên bản.", null,
                    "BAO_CAO_DINH_KY", null, false));
        }
        data.dueCasesForWarning().forEach(item -> result.add(new Warning("OVERDUE_GRIEVANCE",
                item.getSeverity() == CaseSeverity.CRITICAL ? WarningSeverity.CRITICAL : WarningSeverity.WARNING,
                "Có kiến nghị quá hạn và chưa đóng.",
                "Phân công xử lý và cập nhật kết quả/phản hồi.", item.getDeadline(), "SO_KIEN_NGHI",
                null, true)));
        if (!data.seriousOverdueCases().isEmpty()) {
            result.add(new Warning("P02", WarningSeverity.CRITICAL,
                    data.seriousOverdueCases().size() + " kiến nghị khẩn cấp vi phạm SLA trong kỳ.",
                    "Kiểm tra hồ sơ xử lý, lý do quá hạn và phê duyệt gia hạn (nếu có).", null,
                    "SO_KIEN_NGHI", null, true));
        }
        data.overdueWelfareForWarning().forEach(item -> result.add(new Warning("OVERDUE_CARE",
                WarningSeverity.WARNING,
                "Hồ sơ chăm lo quá hạn và chưa hoàn tất.",
                "Hoàn tất xác minh, hỗ trợ và cập nhật mốc hoàn thành.",
                item.getDeadline(), "CHAM_SOC_NLD", null, true)));
        appliedAdjustments.forEach(item -> {
            boolean bonus = "BONUS".equals(item.getAdjustmentType());
            String code = bonus ? "BONUS" : item.getPenaltyCode();
            WarningSeverity severity = bonus ? WarningSeverity.INFO
                    : ("P06".equals(code) || "P07".equals(code)
                    ? WarningSeverity.CRITICAL : WarningSeverity.WARNING);
            String pointText = item.getPoints().stripTrailingZeros().toPlainString();
            String message = bonus
                    ? "Điểm thưởng đã duyệt ghi nhận " + pointText
                    + " điểm; tổng thưởng kỳ áp dụng theo giới hạn cấu hình."
                    : "Điểm phạt " + code + " đã duyệt ghi nhận " + pointText
                    + " điểm; tổng phạt kỳ áp dụng theo giới hạn cấu hình.";
            result.add(new Warning(code, severity, message,
                    canViewSensitive
                            ? "Đối chiếu lý do, người đề nghị và người duyệt trong nhật ký điều chỉnh."
                            : "Liên hệ Công đoàn Tổng Công ty để xem phần nhật ký đã được phân quyền.",
                    null, "KPI_ADJUSTMENT",
                    item.getId() == null ? null : String.valueOf(item.getId()), !canViewSensitive));
        });
        return List.copyOf(result);
    }

    private boolean isApplicableAdjustment(KpiAdjustment item, Map<String, PenaltyRule> rules) {
        if (item.getPoints() == null || item.getPoints().signum() < 0) return false;
        if (!present(item.getEvidenceModule()) || !present(item.getEvidenceRecordId())) return false;
        if ("BONUS".equals(item.getAdjustmentType())) {
            return item.isEffectivenessVerified() && item.isNonDuplicateVerified();
        }
        return "PENALTY".equals(item.getAdjustmentType())
                && present(item.getPenaltyCode()) && rules.containsKey(item.getPenaltyCode());
    }

    private List<AdjustmentAudit> adjustmentAudit(List<KpiAdjustment> appliedAdjustments,
                                                   boolean canViewSensitive) {
        return appliedAdjustments.stream().map(item -> new AdjustmentAudit(
                item.getId(), item.getAdjustmentType(), item.getPenaltyCode(), item.getPoints(),
                canViewSensitive ? item.getReason() : null,
                canViewSensitive ? item.getEvidenceModule() : null,
                canViewSensitive ? item.getEvidenceRecordId() : null,
                item.isEffectivenessVerified(), item.isNonDuplicateVerified(),
                canViewSensitive ? item.getRequestedBy() : null,
                canViewSensitive ? item.getApprovedBy() : null,
                item.getApprovedAt(), !canViewSensitive)).toList();
    }

    private BigDecimal totalPenalty(List<KpiAdjustment> approved, Map<String, PenaltyRule> rules,
                                    Map<String, Integer> automaticCases) {
        Map<String, BigDecimal> grouped = approved.stream()
                .filter(item -> "PENALTY".equals(item.getAdjustmentType()))
                .filter(item -> item.getPoints() != null && item.getPoints().signum() >= 0)
                .filter(item -> item.getPenaltyCode() != null && rules.containsKey(item.getPenaltyCode()))
                .collect(Collectors.groupingBy(KpiAdjustment::getPenaltyCode,
                        Collectors.reducing(BigDecimal.ZERO, KpiAdjustment::getPoints, BigDecimal::add)));
        automaticCases.forEach((code, cases) -> {
            PenaltyRule rule = rules.get(code);
            if (cases > 0 && rule != null) {
                BigDecimal points = rule.getPointsPerCase().multiply(BigDecimal.valueOf(cases), MC);
                grouped.merge(code, points, (left, right) -> left.add(right, MC));
            }
        });
        return grouped.entrySet().stream().map(entry -> {
            PenaltyRule rule = rules.get(entry.getKey());
            return rule == null || rule.getPeriodCap() == null ? entry.getValue() : entry.getValue().min(rule.getPeriodCap());
        }).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String applyGates(String raw, List<KpiAdjustment> approvedAdjustments, UnionUnit unit,
                              UnitData data, int missingReports, List<KpiClassificationRule> rules,
                              Map<String, KpiClassificationGate> gates) {
        String result = raw;
        boolean integrityViolation = hasIntegrityViolation(approvedAdjustments);
        if (integrityViolation) return applyGate(result, "INTEGRITY_VIOLATION", rules, gates);
        boolean seriousOpen = data.dueCasesForWarning().stream()
                .anyMatch(item -> item.getSeverity() == CaseSeverity.CRITICAL);
        if (seriousOpen) result = applyGate(result, "SERIOUS_OPEN_CASE", rules, gates);
        if (missingReports > 0) result = applyGate(result, "MISSING_MANDATORY_REPORT", rules, gates);
        boolean governanceIncomplete = data.governanceSourceExcluded()
                || unit.getLegalStatus() != LegalStatus.ACTIVE
                || !present(unit.getDecisionNumber()) || !present(unit.getChairperson())
                || unit.getTermStart() == null || unit.getTermEnd() == null
                || unit.getTermStart().isAfter(data.periodAsOf()) || unit.getTermEnd().isBefore(data.periodAsOf());
        if (governanceIncomplete) result = applyGate(result, "GOVERNANCE_INCOMPLETE", rules, gates);
        return result;
    }

    private boolean hasIntegrityViolation(List<KpiAdjustment> approvedAdjustments) {
        return approvedAdjustments.stream()
                .filter(item -> "PENALTY".equals(item.getAdjustmentType()))
                .anyMatch(item -> "P06".equals(item.getPenaltyCode()) || "P07".equals(item.getPenaltyCode()));
    }

    private String applyGate(String current, String code, List<KpiClassificationRule> rules,
                             Map<String, KpiClassificationGate> gates) {
        KpiClassificationGate gate = gates.get(code);
        return gate == null ? current : cap(current, gate.getClassificationCap(), rules);
    }

    private String cap(String current, String limit, List<KpiClassificationRule> rules) {
        Map<String, Integer> order = new HashMap<>();
        for (int index = 0; index < rules.size(); index++) order.put(rules.get(index).getLabel(), index);
        return order.getOrDefault(current, rules.size()) < order.getOrDefault(limit, rules.size()) ? limit : current;
    }

    private int missingMandatoryReports(UnitData data) {
        if (data.slaRule(REPORT_SLA) == null) return 0;
        Set<YearMonth> submitted = data.reports().stream()
                .filter(item -> item.getStatus() == ReportStatus.SUBMITTED || item.getStatus() == ReportStatus.APPROVED)
                .filter(item -> item.getSubmittedAt() != null)
                .map(item -> YearMonth.from(item.getReportMonth())).collect(Collectors.toSet());
        return (int) dueReportMonths(data).stream().filter(month -> !submitted.contains(month)).count();
    }

    private List<YearMonth> dueReportMonths(UnitData data) {
        SlaRule rule = data.slaRule(REPORT_SLA);
        if (rule == null) return List.of();
        YearMonth cursor = YearMonth.from(data.period().periodStart());
        YearMonth last = YearMonth.from(data.period().periodEnd());
        List<YearMonth> due = new ArrayList<>();
        while (!cursor.isAfter(last)) {
            if (!reportDeadline(cursor, rule, data.calendarOverrides()).isAfter(data.asOf())) due.add(cursor);
            cursor = cursor.plusMonths(1);
        }
        return List.copyOf(due);
    }

    static LocalDate reportDeadline(YearMonth month, SlaRule rule, Map<LocalDate, Boolean> overrides) {
        return slaDeadline(month.atEndOfMonth(), rule, overrides);
    }

    /**
     * Deadline for an SLA anchored on a business date. Business days skip weekends unless the approved
     * calendar says otherwise, so a missing holiday row silently makes the deadline earlier than reality.
     */
    static LocalDate slaDeadline(LocalDate anchor, SlaRule rule, Map<LocalDate, Boolean> overrides) {
        if (!"BUSINESS_DAY".equals(rule.getDurationUnit())) return anchor.plusDays(rule.getDurationValue());
        LocalDate date = anchor;
        int remaining = rule.getDurationValue();
        while (remaining > 0) {
            date = date.plusDays(1);
            if (isWorkingDay(date, overrides)) remaining--;
        }
        return date;
    }

    private static boolean isWorkingDay(LocalDate date, Map<LocalDate, Boolean> overrides) {
        Boolean override = overrides.get(date);
        return override != null ? override
                : date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY;
    }

    private Summary summarize(List<UnitResult> results, KpiVersion version) {
        BigDecimal average = results.isEmpty() ? BigDecimal.ZERO : results.stream().map(UnitResult::finalScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(results.size()), MC);
        int finalUnits = (int) results.stream().filter(item -> item.runStatus() == RunStatus.FINAL).count();
        int provisional = (int) results.stream().filter(item -> item.runStatus() != RunStatus.FINAL).count();
        int excellent = (int) results.stream().filter(item -> item.runStatus() == RunStatus.FINAL)
                .filter(item -> "Xuất sắc".equals(item.finalClassification())).count();
        int attention = (int) results.stream().filter(item -> "Trung bình".equals(item.finalClassification())
                || "Không đạt".equals(item.finalClassification())).count();
        int lockEligible = (int) results.stream()
                .filter(item -> lockEligible(item, version.getDataQualityFinalThreshold())).count();
        return new Summary(average, finalUnits, provisional, excellent, attention, lockEligible);
    }

    /**
     * Whether a live result may be locked as an official {@code FINAL} run. A unit still carrying missing or
     * failed KPIs is never official, no matter how high its provisional score looks.
     */
    public static boolean lockEligible(UnitResult result, BigDecimal dataQualityThreshold) {
        return result.dataQualityRate() != null && dataQualityThreshold != null
                && result.dataQualityRate().compareTo(dataQualityThreshold) >= 0
                && result.details().stream().noneMatch(item -> item.resultStatus() == ResultStatus.MISSING_DATA
                || item.resultStatus() == ResultStatus.FAILED_VALIDATION);
    }

    static Comparator<UnitResult> rankingComparator() {
        return Comparator.comparing(UnitResult::finalScore, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(item -> groupScore(item, "CARE"), Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(item -> groupScore(item, "GRV"), Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(UnitResult::reportOnTimeRate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(UnitResult::penaltyPoints)
                .thenComparing(UnitResult::unionUnitCode);
    }

    static List<UnitResult> rankOfficial(List<UnitResult> sortedResults) {
        List<UnitResult> result = new ArrayList<>();
        int officialPosition = 0;
        int assignedRank = 0;
        UnitResult previous = null;
        Integer previousResultIndex = null;
        for (UnitResult item : sortedResults) {
            if (item.runStatus() != RunStatus.FINAL) {
                result.add(item);
                continue;
            }
            officialPosition++;
            boolean tied = previous != null && sameTieValues(previous, item);
            if (!tied) assignedRank = officialPosition;
            if (tied && previousResultIndex != null) {
                UnitResult previousResult = result.get(previousResultIndex);
                if (!previousResult.tied()) result.set(previousResultIndex,
                        copyRank(previousResult, previousResult.rank(), true));
            }
            result.add(copyRank(item, assignedRank, tied));
            previousResultIndex = result.size() - 1;
            previous = item;
        }
        return List.copyOf(result);
    }

    private static boolean sameTieValues(UnitResult left, UnitResult right) {
        return sameNumber(left.finalScore(), right.finalScore())
                && sameNumber(groupScore(left, "CARE"), groupScore(right, "CARE"))
                && sameNumber(groupScore(left, "GRV"), groupScore(right, "GRV"))
                && sameNumber(left.reportOnTimeRate(), right.reportOnTimeRate())
                && sameNumber(left.penaltyPoints(), right.penaltyPoints());
    }

    private static boolean sameNumber(BigDecimal left, BigDecimal right) {
        return left == null ? right == null : right != null && left.compareTo(right) == 0;
    }

    /** The same result promoted to an official run; only a period lock may do this. */
    public static UnitResult asFinal(UnitResult item) {
        return new UnitResult(item.runId(), item.unionUnitId(), item.unionUnitCode(), item.unionUnitName(),
                item.activeMemberCount(), RunStatus.FINAL, item.dataQualityRate(), item.baseScore(),
                item.bonusPoints(), item.penaltyPoints(), item.finalScore(), item.rawClassification(),
                item.finalClassification(), item.rank(), item.tied(), item.reportOnTimeRate(), item.groups(),
                item.details(), item.warnings(), item.adjustments());
    }

    static UnitResult copyRank(UnitResult item, Integer rank, boolean tied) {
        return new UnitResult(item.runId(), item.unionUnitId(), item.unionUnitCode(), item.unionUnitName(),
                item.activeMemberCount(), item.runStatus(), item.dataQualityRate(), item.baseScore(),
                item.bonusPoints(), item.penaltyPoints(), item.finalScore(), item.rawClassification(),
                item.finalClassification(), rank, tied, item.reportOnTimeRate(), item.groups(),
                item.details(), item.warnings(), item.adjustments());
    }

    private static BigDecimal groupScore(UnitResult result, String code) {
        return result.groups().stream().filter(item -> code.equals(item.groupCode()))
                .map(GroupResult::score).findFirst().orElse(null);
    }

    private <T> org.springframework.data.jpa.domain.Specification<T> between(String field, Period period) {
        return (root, query, cb) -> cb.between(root.get(field), period.periodStart(), period.periodEnd());
    }

    private boolean beforeCutoff(BaseEntity entity, Instant cutoff) {
        return entity.getCreatedAt() != null && !entity.getCreatedAt().isAfter(cutoff);
    }

    private boolean included(Set<String> exclusions, String module, String key) {
        return key != null && !exclusions.contains(exclusionKey(module, key));
    }

    private static String exclusionKey(String module, String key) {
        return module + EXCLUSION_SEPARATOR + key;
    }

    private boolean present(String value) {
        return value != null && !value.isBlank();
    }

    private boolean sameModule(String left, String right) {
        return present(left) && present(right) && left.trim().equalsIgnoreCase(right.trim());
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal count(boolean value) {
        return value ? BigDecimal.ONE : BigDecimal.ZERO;
    }

    private List<SourceRef> refs(String module, Long id, boolean valid) {
        return id == null ? List.of()
                : List.of(new SourceRef(module, String.valueOf(id), "union-unit", valid, valid));
    }

    private <T> List<SourceRef> refs(String module, List<T> rows, Function<T, Long> id,
                                     Predicate<T> inNumerator) {
        return rows.stream().map(item -> new SourceRef(module, String.valueOf(id.apply(item)),
                resourceType(item), inNumerator.test(item), inNumerator.test(item))).toList();
    }

    private String resourceType(Object item) {
        if (item instanceof Member) return "member";
        if (item instanceof MemberChange) return "member-change";
        if (item instanceof MonthlyReport) return "monthly-report";
        if (item instanceof WelfareRecord) return "welfare";
        if (item instanceof LaborCase) return "labor-case";
        if (item instanceof UnionActivity) return "activity";
        if (item instanceof FinanceEntry) return "finance-entry";
        throw new IllegalArgumentException("Loại nguồn KPI chưa có đường dẫn drill-down: " + item.getClass().getName());
    }

    private record SourceRef(String module, String recordId, String resourceType, boolean numerator,
                             boolean structurallyValid, EvidenceRole roleOverride) {
        SourceRef(String module, String recordId, String resourceType, boolean numerator, boolean structurallyValid) {
            this(module, recordId, resourceType, numerator, structurallyValid, null);
        }
    }

    private record Metric(BigDecimal numerator, BigDecimal denominator, boolean complete, boolean failedValidation,
                          String explanation, List<SourceRef> refs, boolean sensitive) {
    }

    private record UnitData(List<Member> employees, List<MemberChange> memberChanges,
                            List<MonthlyReport> reports, List<WelfareRecord> welfare,
                            List<LaborCase> cases, List<UnionActivity> activities,
                            List<FinanceEntry> finance, Set<Long> welfareWithFiles,
                            Set<Long> activitiesWithMedia, Set<Long> financeWithFiles,
                            Set<Long> careWithFinanceEntry, Instant cutoff, Period period,
                            Map<String, SlaRule> slaRules, Map<LocalDate, Boolean> calendarOverrides,
                            boolean governanceSourceExcluded) {
        SlaRule slaRule(String slaCode) {
            return slaRules.get(slaCode);
        }

        /** Union members only: the roster in {@code employees} also carries the workers who have not joined. */
        List<Member> members() {
            return employees.stream()
                    .filter(item -> item.getMembershipStatus() == MembershipStatus.MEMBER)
                    .filter(item -> item.getJoinDate() == null || !item.getJoinDate().isAfter(periodAsOf()))
                    .toList();
        }

        List<WelfareRecord> periodWelfare() {
            return welfare.stream().filter(item -> !item.getEventDate().isBefore(period.periodStart())
                    && !item.getEventDate().isAfter(periodAsOf())).toList();
        }

        List<WelfareRecord> dueWelfare() {
            return periodWelfare().stream().filter(item -> item.getDeadline() != null)
                    .filter(item -> !item.getDeadline().isAfter(periodAsOf())).toList();
        }

        List<WelfareRecord> completedWelfare() {
            return periodWelfare().stream().filter(item -> item.getStatus() == WorkStatus.COMPLETED).toList();
        }

        List<WelfareRecord> policyWelfare() {
            return periodWelfare().stream().filter(item -> item.getPolicyId() != null).toList();
        }

        /** Care that passed approval, so a matching finance entry must exist. */
        List<WelfareRecord> approvedWelfare() {
            return periodWelfare().stream().filter(item -> item.getStatus() == WorkStatus.IN_PROGRESS
                    || item.getStatus() == WorkStatus.COMPLETED).toList();
        }

        List<WelfareRecord> overdueWelfareForWarning() {
            return welfare.stream().filter(item -> item.getDeadline() != null
                    && item.getDeadline().isBefore(periodAsOf())
                    && item.getStatus() != WorkStatus.COMPLETED).toList();
        }

        List<UnionActivity> completedActivities() {
            return activities.stream().filter(item -> item.getStatus() == ActivityStatus.COMPLETED).toList();
        }

        List<LaborCase> periodCases(Period period) {
            return cases.stream().filter(item -> !item.getReceivedDate().isBefore(period.periodStart())
                    && !item.getReceivedDate().isAfter(periodAsOf())).toList();
        }

        List<LaborCase> dueCases(Period period) {
            return cases.stream().filter(item -> item.getDeadline() != null)
                    .filter(item -> !item.getDeadline().isAfter(periodAsOf()))
                    .filter(item -> !item.getDeadline().isBefore(period.periodStart())
                            || closedAt(item) == null || !closedAt(item).isBefore(period.periodStart()))
                    .toList();
        }

        List<LaborCase> closedCases(Period period) {
            return cases.stream().filter(item -> item.getStatus() == CaseStatus.CLOSED && item.getApprovedAt() != null)
                    .filter(item -> {
                        LocalDate closedAt = item.getApprovedAt().atZone(BUSINESS_ZONE).toLocalDate();
                        return !closedAt.isBefore(period.periodStart()) && !closedAt.isAfter(period.periodEnd())
                                && !item.getApprovedAt().isAfter(cutoff);
                    }).toList();
        }

        List<LaborCase> dueCasesForWarning() {
            return cases.stream().filter(item -> item.getDeadline() != null
                    && item.getDeadline().isBefore(periodAsOf()))
                    .filter(item -> !closedBy(item, periodAsOf()))
                    .filter(item -> !item.getDeadline().isBefore(period.periodStart())
                            || closedAt(item) == null || !closedAt(item).isBefore(period.periodStart()))
                    .toList();
        }

        List<LaborCase> seriousOverdueCases() {
            return cases.stream().filter(item -> item.getSeverity() == CaseSeverity.CRITICAL
                    && item.getDeadline() != null && item.getDeadline().isBefore(periodAsOf()))
                    .filter(item -> closedAt(item) == null || closedAt(item).isAfter(item.getDeadline()))
                    .filter(item -> !item.getDeadline().isBefore(period.periodStart())
                            || closedAt(item) == null || !closedAt(item).isBefore(period.periodStart()))
                    .toList();
        }

        List<LaborCase> relevantCases() {
            Set<LaborCase> result = new LinkedHashSet<>();
            result.addAll(periodCases(period));
            result.addAll(dueCases(period));
            result.addAll(closedCases(period));
            return List.copyOf(result);
        }

        private LocalDate closedAt(LaborCase item) {
            return item.getStatus() == CaseStatus.CLOSED && item.getApprovedAt() != null
                    ? item.getApprovedAt().atZone(BUSINESS_ZONE).toLocalDate() : null;
        }

        private boolean closedBy(LaborCase item, LocalDate date) {
            LocalDate closedAt = closedAt(item);
            return closedAt != null && !closedAt.isAfter(date);
        }

        LocalDate asOf() {
            return LocalDate.ofInstant(cutoff, BUSINESS_ZONE);
        }

        LocalDate periodAsOf() {
            LocalDate cutoffDate = asOf();
            return cutoffDate.isAfter(period.periodEnd()) ? period.periodEnd() : cutoffDate;
        }
    }
}
