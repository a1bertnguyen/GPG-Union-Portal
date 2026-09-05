package vn.gpg.unionportal.service.kpi;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import vn.gpg.unionportal.dto.KpiModels.*;
import vn.gpg.unionportal.exception.ResourceNotFoundException;
import vn.gpg.unionportal.model.kpi.*;
import vn.gpg.unionportal.repository.kpi.*;
import vn.gpg.unionportal.service.CurrentUserService;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Locks a live KPI evaluation into an official run, and reads the locked runs back as history.
 *
 * <p>{@code GET /api/kpi} recomputes and stays DRAFT/PROVISIONAL by design, so nothing there can be quoted as
 * an official ranking. A lock is the only writer of {@code kpi_runs}: it promotes the units that carry no
 * missing or failed KPI to {@code FINAL}, ranks that subset, and stores the per-KPI numbers. That storage is
 * also the only way a closed period stays readable, because state-at-period-end KPIs cannot be recomputed
 * once the source rows move on.
 */
@Service
@Transactional(readOnly = true)
public class KpiRunService {
    private static final String CASE_MODULE = "SO_KIEN_NGHI";
    private static final String CASE_GROUP = "GRV";
    private static final int MAX_HISTORY_YEARS = 20;
    private static final int MAX_EVIDENCE_PAGE_SIZE = 200;

    private final GpgKpiEngine engine;
    private final KpiActivityService activity;
    private final KpiVersionRepository versions;
    private final KpiRunRepository runs;
    private final KpiResultDetailRepository details;
    private final KpiRunWarningRepository warnings;
    private final KpiEvidenceRowRepository evidenceRows;
    private final CurrentUserService currentUser;
    private final JdbcTemplate jdbc;

    /**
     * Snapshot payloads are hashed into {@code kpi_runs.input_hash}, so this mapper stays local and
     * unconfigured on purpose: adopting the shared web mapper would change stored JSON and silently
     * invalidate every hash written before the change.
     */
    private final JsonMapper json = JsonMapper.builder().findAndAddModules().build();

    public KpiRunService(GpgKpiEngine engine, KpiActivityService activity, KpiVersionRepository versions,
                         KpiRunRepository runs, KpiResultDetailRepository details,
                         KpiRunWarningRepository warnings, KpiEvidenceRowRepository evidenceRows,
                         CurrentUserService currentUser, JdbcTemplate jdbc) {
        this.engine = engine;
        this.activity = activity;
        this.versions = versions;
        this.runs = runs;
        this.details = details;
        this.warnings = warnings;
        this.evidenceRows = evidenceRows;
        this.currentUser = currentUser;
        this.jdbc = jdbc;
    }

    /** What a locked run looks like when read back, including the activity statistics of that period. */
    public record RunDetail(KpiRun run, UnitResult result, KpiActivityService.ActivityView activity) {
    }

    /** Whether a unit could be locked as official today, and what stands in the way if not. */
    public record Readiness(Long unitId, String unitName, boolean ready, List<String> blockers) {
    }

    /**
     * Turns the live evaluation of a finished year into official runs. Only YEAR periods lock: the shorter
     * periods exist for monitoring, and ranking them would invite quoting a partial year as a result.
     */
    @Transactional
    public LockResult lock(PeriodType periodType, int year, int period) {
        if (!currentUser.isAdmin()) throw new AccessDeniedException("Chỉ ADMIN được chốt kỳ KPI");
        if (periodType != PeriodType.YEAR || period != 1) {
            throw new IllegalArgumentException("Chỉ chốt KPI theo năm");
        }
        // Serialize all-unit ranking and revision assignment in a stable order.
        jdbc.queryForList("SELECT id FROM union_units ORDER BY id FOR UPDATE");
        Dashboard dashboard = engine.evaluate(periodType, year, period, null);
        if (!dashboard.periodEnd().isBefore(LocalDate.now(GpgKpiEngine.BUSINESS_ZONE))) {
            throw new IllegalArgumentException("Chỉ được chốt kỳ đã kết thúc");
        }
        KpiVersion version = versions.findById(dashboard.versionId()).orElseThrow(() ->
                new ResourceNotFoundException("Không tìm thấy phiên bản KPI " + dashboard.versionId()));
        BigDecimal threshold = version.getDataQualityFinalThreshold();

        List<UnitResult> promoted = dashboard.results().stream()
                .map(item -> GpgKpiEngine.lockEligible(item, threshold) ? GpgKpiEngine.asFinal(item) : item)
                .sorted(GpgKpiEngine.rankingComparator())
                .toList();
        List<UnitResult> ranked = GpgKpiEngine.rankOfficial(promoted);

        String lockedBy = currentUser.username();
        Instant lockedAt = Instant.now();
        List<LockedUnit> locked = new ArrayList<>();
        for (UnitResult result : ranked) {
            locked.add(persist(result, dashboard, version, lockedAt, lockedBy));
        }
        int finalCount = (int) locked.stream().filter(item -> item.runStatus() == RunStatus.FINAL).count();
        int unchanged = (int) locked.stream().filter(LockedUnit::unchanged).count();
        return new LockResult(version.getVersionId(), dashboard.periodType(), dashboard.periodStart(),
                dashboard.periodEnd(), lockedAt, lockedBy, finalCount, locked.size() - finalCount,
                unchanged, List.copyOf(locked));
    }

    private LockedUnit persist(UnitResult result, Dashboard dashboard, KpiVersion version,
                               Instant lockedAt, String lockedBy) {
        String periodType = dashboard.periodType().name();
        KpiRun previous = runs
                .findFirstByUnionUnitIdAndPeriodTypeAndPeriodStartAndPeriodEndAndVersionIdOrderByRevisionDesc(
                        result.unionUnitId(), periodType, dashboard.periodStart(), dashboard.periodEnd(),
                        version.getVersionId())
                .orElse(null);
        KpiActivityService.ActivityView view = activityView(result, dashboard);
        String inputHash = inputHash(result, view, version);
        List<String> blocking = result.details().stream()
                .filter(item -> item.resultStatus() == ResultStatus.MISSING_DATA
                        || item.resultStatus() == ResultStatus.FAILED_VALIDATION)
                .map(Detail::kpiCode).sorted().toList();
        if (previous != null && inputHash.equals(previous.getInputHash())) {
            // Nothing behind the score changed, so re-locking must not invent a new revision.
            return new LockedUnit(result.unionUnitId(), result.unionUnitCode(), result.unionUnitName(),
                    previous.getId(), previous.getRevision(), RunStatus.valueOf(previous.getRunStatus()),
                    previous.getFinalScore(), previous.getFinalClassification(), previous.getRankingPosition(),
                    true, blocking);
        }

        int revision = previous == null ? 1 : previous.getRevision() + 1;
        KpiRun run = new KpiRun();
        // Unit code and name are copied in: a later rename must not rewrite what a closed year was ranked as.
        run.setUnitCodeSnapshot(result.unionUnitCode());
        run.setUnitNameSnapshot(result.unionUnitName());
        run.setPopulationSnapshotId(view.populationSnapshotId());
        run.setActiveEmployeeCount(view.activeEmployeeCount());
        run.setActiveUnionMemberCount(view.activeUnionMemberCount());
        run.setRunKey("KPI-" + version.getVersionId() + "-" + periodType + "-" + dashboard.periodStart()
                + "-" + result.unionUnitCode() + "-r" + revision);
        run.setUnionUnitId(result.unionUnitId());
        run.setPeriodType(periodType);
        run.setPeriodStart(dashboard.periodStart());
        run.setPeriodEnd(dashboard.periodEnd());
        run.setVersionId(version.getVersionId());
        run.setRevision(revision);
        run.setCutoffAt(dashboard.cutoffAt());
        run.setRunStatus(result.runStatus().name());
        run.setDataQualityRate(result.dataQualityRate());
        run.setBaseScore(result.baseScore());
        run.setBonusPoints(result.bonusPoints());
        run.setPenaltyPoints(result.penaltyPoints());
        run.setFinalScore(result.finalScore());
        run.setRawClassification(result.rawClassification());
        run.setFinalClassification(result.finalClassification());
        run.setRankingPosition(result.rank());
        run.setInputHash(inputHash);
        run.setPreviousRunId(previous == null ? null : previous.getId());
        run.setCalculatedAt(lockedAt);
        run.setCalculatedBy(lockedBy);
        KpiRun saved = runs.saveAndFlush(run);

        storeActivity(saved.getId(), view);
        storeDetails(saved.getId(), result);
        storeWarnings(saved.getId(), result);
        return new LockedUnit(result.unionUnitId(), result.unionUnitCode(), result.unionUnitName(),
                saved.getId(), revision, result.runStatus(), result.finalScore(),
                result.finalClassification(), result.rank(), false, blocking);
    }

    /** Locking is YEAR-only, so the activity view is always the whole-year one. */
    private KpiActivityService.ActivityView activityView(UnitResult result, Dashboard dashboard) {
        Period period = GpgKpiEngine.resolvePeriod(PeriodType.YEAR, dashboard.periodStart().getYear(), 1);
        return activity.read(result.unionUnitId(), period, result.details());
    }

    /**
     * Fingerprint of everything the score was computed from. Re-locking a year whose sources have not moved
     * produces the same hash, which is what keeps a repeated lock from inventing an empty revision.
     */
    private String inputHash(UnitResult result, KpiActivityService.ActivityView view, KpiVersion version) {
        return hash(json.writeValueAsString(result) + "\n" + json.writeValueAsString(view) + "\n"
                + version.getVersionId());
    }

    static String hash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException cause) {
            throw new IllegalStateException("JVM thiếu thuật toán SHA-256", cause);
        }
    }

    private void storeActivity(Long runId, KpiActivityService.ActivityView view) {
        jdbc.update("INSERT INTO kpi_activity_statistics(run_id, statistic_payload) VALUES (?, ?)",
                runId, json.writeValueAsString(view));
        List<Object[]> rows = view.sources().stream()
                .map(source -> {
                    String payload = json.writeValueAsString(source);
                    return new Object[]{runId, source.module(), source.id(), hash(payload), payload};
                })
                .toList();
        if (rows.isEmpty()) return;
        jdbc.batchUpdate("INSERT INTO kpi_source_snapshots(run_id, source_module, source_record_id,"
                + " payload_hash, snapshot_payload) VALUES (?, ?, ?, ?, ?)", rows);
    }

    private void storeDetails(Long runId, UnitResult result) {
        for (Detail detail : result.details()) {
            KpiResultDetail row = new KpiResultDetail();
            row.setRunId(runId);
            row.setKpiCode(detail.kpiCode());
            row.setNumerator(detail.numerator());
            row.setDenominator(detail.denominator());
            row.setTargetValue(detail.targetValue());
            row.setNormalizedScore(detail.normalizedScore());
            row.setEligibleWeight(detail.eligibleWeight());
            row.setEarnedPoints(detail.earnedPoints());
            row.setResultStatus(detail.resultStatus().name());
            row.setExplanation(detail.explanation());
            storeEvidence(details.save(row).getId(), detail.evidence());
        }
        String payload = json.writeValueAsString(result);
        jdbc.update("INSERT INTO kpi_source_snapshots(run_id, source_module, source_record_id,"
                        + " payload_hash, snapshot_payload) VALUES (?, 'KPI_RUN', 'result', ?, ?)",
                runId, hash(payload), payload);
    }

    /** Evidence without a source record id is an engine placeholder, not a record anyone can open. */
    private void storeEvidence(Long resultId, List<Evidence> evidence) {
        List<KpiEvidenceRow> rows = new ArrayList<>(evidence.size());
        for (Evidence item : evidence) {
            if (item.sourceRecordId() == null) continue;
            KpiEvidenceRow row = new KpiEvidenceRow();
            row.setResultId(resultId);
            row.setSourceModule(item.sourceModule());
            row.setSourceRecordId(item.sourceRecordId());
            row.setEvidenceRole(item.role().name());
            row.setEvidenceUrl(item.evidenceUrl());
            row.setValidationStatus(item.validationStatus().name());
            row.setRedacted(item.redacted());
            rows.add(row);
        }
        if (!rows.isEmpty()) evidenceRows.saveAll(rows);
    }

    private void storeWarnings(Long runId, UnitResult result) {
        List<KpiRunWarning> rows = new ArrayList<>(result.warnings().size());
        for (Warning warning : result.warnings()) {
            KpiRunWarning row = new KpiRunWarning();
            row.setRunId(runId);
            row.setWarningCode(warning.code());
            row.setSeverity(warning.severity().name());
            row.setMessage(warning.message());
            row.setRecommendedAction(warning.recommendedAction());
            row.setSourceModule(warning.sourceModule());
            row.setSourceRecordId(warning.sourceRecordId());
            row.setRedacted(warning.redacted());
            row.setDueAt(warning.dueAt());
            rows.add(row);
        }
        if (!rows.isEmpty()) warnings.saveAll(rows);
    }

    /** Dry run of a lock: what each unit still has to fix before its year can become official. */
    public List<Readiness> readiness(int year, Long unitId) {
        Dashboard dashboard = engine.evaluate(PeriodType.YEAR, year, 1, unitId);
        Period period = GpgKpiEngine.resolvePeriod(PeriodType.YEAR, year, 1);
        boolean yearStillOpen = !dashboard.periodEnd().isBefore(LocalDate.now(GpgKpiEngine.BUSINESS_ZONE));
        return dashboard.results().stream().map(result -> {
            KpiActivityService.ActivityView view =
                    activity.read(result.unionUnitId(), period, result.details());
            List<String> blockers = new ArrayList<>(view.blockers());
            result.details().stream()
                    .filter(detail -> detail.resultStatus() == ResultStatus.MISSING_DATA
                            || detail.resultStatus() == ResultStatus.FAILED_VALIDATION)
                    .forEach(detail -> blockers.add(detail.kpiCode() + ": " + detail.explanation()));
            if (yearStillOpen) blockers.add("Năm chưa kết thúc");
            return new Readiness(result.unionUnitId(), result.unionUnitName(), blockers.isEmpty(),
                    List.copyOf(blockers));
        }).toList();
    }

    /**
     * Locked runs over a span of years, newest first. Superseded revisions stay in the table for the audit
     * trail and are folded away unless asked for.
     */
    public List<KpiRun> history(int fromYear, int toYear, Long requestedUnit, boolean includeSuperseded) {
        if (fromYear < 2000 || toYear > 2200 || fromYear > toYear || toYear - fromYear > MAX_HISTORY_YEARS) {
            throw new IllegalArgumentException("Khoảng lịch sử tối đa " + MAX_HISTORY_YEARS + " năm");
        }
        Long unitId = currentUser.scopedUnitId(requestedUnit);
        LocalDate from = LocalDate.of(fromYear, 1, 1);
        LocalDate to = LocalDate.of(toYear, 12, 31);
        List<KpiRun> found = runs.findAll((root, query, builder) -> {
            List<Predicate> filters = new ArrayList<>(2);
            filters.add(builder.between(root.get("periodStart"), from, to));
            if (unitId != null) filters.add(builder.equal(root.get("unionUnitId"), unitId));
            return builder.and(filters.toArray(Predicate[]::new));
        }, Sort.by(Sort.Direction.DESC, "periodStart", "id"));
        if (includeSuperseded) return found;

        // Newest first, so the first row seen for a unit and period is its current revision.
        Map<String, KpiRun> latest = new LinkedHashMap<>();
        for (KpiRun run : found) {
            latest.putIfAbsent(run.getUnionUnitId() + ":" + run.getPeriodType() + ":" + run.getPeriodStart(),
                    run);
        }
        return List.copyOf(latest.values());
    }

    /**
     * One locked run as it was stored. The score comes back from the snapshot rather than being recomputed,
     * which is the whole point of locking: the source rows have moved on since.
     */
    public RunDetail read(long id) {
        KpiRun run = runs.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Không tìm thấy bản chốt KPI"));
        currentUser.requireUnitAccess(run.getUnionUnitId());
        List<String> snapshots = jdbc.query("SELECT snapshot_payload FROM kpi_source_snapshots"
                        + " WHERE run_id = ? AND source_module = 'KPI_RUN'",
                (row, index) -> row.getString(1), id);
        if (snapshots.isEmpty()) {
            throw new ResourceNotFoundException("Bản chốt cũ chưa có snapshot chi tiết");
        }
        UnitResult result = json.readValue(snapshots.getFirst(), UnitResult.class);
        List<String> statistics = jdbc.query(
                "SELECT statistic_payload FROM kpi_activity_statistics WHERE run_id = ?",
                (row, index) -> row.getString(1), id);
        KpiActivityService.ActivityView view = statistics.isEmpty() ? null
                : json.readValue(statistics.getFirst(), KpiActivityService.ActivityView.class);
        if (currentUser.isAdmin()) return new RunDetail(run, result, view);
        return new RunDetail(run, redact(result), redact(view));
    }

    public List<KpiEvidenceRow> evidence(long resultId, int page, int size, String role) {
        if (page < 0 || size < 1 || size > MAX_EVIDENCE_PAGE_SIZE) {
            throw new IllegalArgumentException("Phân trang chứng cứ không hợp lệ");
        }
        if (role != null) EvidenceRole.valueOf(role);
        KpiResultDetail detail = details.findById(resultId).orElseThrow(() ->
                new ResourceNotFoundException("Không tìm thấy chỉ tiêu"));
        KpiRun run = runs.findById(detail.getRunId()).orElseThrow(() ->
                new ResourceNotFoundException("Không tìm thấy bản chốt KPI"));
        currentUser.requireUnitAccess(run.getUnionUnitId());
        boolean admin = currentUser.isAdmin();
        return evidenceRows.findByResultIdIn(List.of(resultId)).stream()
                .filter(item -> role == null || role.equals(item.getEvidenceRole()))
                .filter(item -> admin || !CASE_MODULE.equals(item.getSourceModule()))
                .skip((long) page * size)
                .limit(size)
                .toList();
    }


    /**
     * Activity statistics for one unit and period, redacted for non-admin readers. Lives here rather than in
     * the controller so the redaction rule has exactly one implementation.
     */
    public KpiActivityService.ActivityView statistics(PeriodType periodType, int year, int period,
                                                      Long requestedUnitId) {
        Long unitId = currentUser.scopedUnitId(requestedUnitId);
        Dashboard dashboard = engine.evaluate(periodType, year, period, unitId);
        if (dashboard.results().isEmpty()) {
            throw new ResourceNotFoundException("Không có dữ liệu KPI cho CĐCS trong kỳ đã chọn");
        }
        UnitResult result = dashboard.results().getFirst();
        KpiActivityService.ActivityView view = activity.read(result.unionUnitId(),
                GpgKpiEngine.resolvePeriod(periodType, year, period), result.details());
        return currentUser.isAdmin() ? view : redact(view);
    }
    /** Non-admin readers keep every number but lose grievance identifiers and links. */
    private static UnitResult redact(UnitResult result) {
        List<Detail> redactedDetails = result.details().stream().map(KpiRunService::redact).toList();
        List<GroupResult> groups = result.groups().stream()
                .map(group -> new GroupResult(group.groupCode(), group.name(), group.configuredWeight(),
                        group.eligibleWeight(), group.earnedPoints(), group.score(), group.status(),
                        redactedDetails.stream()
                                .filter(detail -> detail.groupCode().equals(group.groupCode()))
                                .toList()))
                .toList();
        return new UnitResult(result.runId(), result.unionUnitId(), result.unionUnitCode(),
                result.unionUnitName(), result.activeMemberCount(), result.runStatus(),
                result.dataQualityRate(), result.baseScore(), result.bonusPoints(), result.penaltyPoints(),
                result.finalScore(), result.rawClassification(), result.finalClassification(), result.rank(),
                result.tied(), result.reportOnTimeRate(), groups, redactedDetails,
                result.warnings().stream().map(KpiRunService::redactWarning).toList(), List.of());
    }

    private static Detail redact(Detail detail) {
        return new Detail(detail.resultId(), detail.kpiCode(), detail.groupCode(), detail.name(),
                detail.weight(), detail.numerator(), detail.denominator(), detail.targetValue(),
                detail.normalizedScore(), detail.eligibleWeight(), detail.earnedPoints(),
                detail.resultStatus(), detail.explanation(),
                detail.warnings().stream().map(KpiRunService::redactWarning).toList(),
                detail.evidence().stream().map(KpiRunService::redact).toList(), detail.evidenceCount());
    }

    private static Evidence redact(Evidence evidence) {
        if (!CASE_MODULE.equals(evidence.sourceModule())) return evidence;
        return new Evidence(evidence.evidenceId(), evidence.resultId(), evidence.sourceModule(), null,
                evidence.role(), null, null, evidence.validationStatus(), true);
    }

    private static Warning redactWarning(Warning warning) {
        if (!CASE_MODULE.equals(warning.sourceModule()) && !warning.redacted()) return warning;
        return new Warning(warning.code(), warning.severity(), "Cảnh báo nghiệp vụ hạn chế truy cập",
                warning.recommendedAction(), warning.dueAt(), warning.sourceModule(), null, true);
    }

    /** Grievance breakdowns stay ADMIN-only, matching the live dashboard. */
    private static KpiActivityService.ActivityView redact(KpiActivityService.ActivityView view) {
        if (view == null) return null;
        List<KpiActivityService.Statistic> statistics = view.statistics().stream()
                .map(item -> CASE_GROUP.equals(item.groupCode())
                        ? new KpiActivityService.Statistic(item.groupCode(), item.code(), item.label(),
                        item.dimensionType(), item.dimensionKey(), item.numerator(), item.denominator(),
                        item.measure(), List.of(), List.of(), List.of(), item.status())
                        : item)
                .toList();
        return new KpiActivityService.ActivityView(view.populationSnapshotId(), view.activeEmployeeCount(),
                view.activeUnionMemberCount(), statistics,
                view.sources().stream().filter(item -> !CASE_MODULE.equals(item.module())).toList(),
                view.blockers());
    }
}
