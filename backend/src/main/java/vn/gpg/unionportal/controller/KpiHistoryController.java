package vn.gpg.unionportal.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.gpg.unionportal.dto.KpiModels.*;
import vn.gpg.unionportal.exception.ResourceNotFoundException;
import vn.gpg.unionportal.model.kpi.KpiEvidenceRow;
import vn.gpg.unionportal.model.kpi.KpiRun;
import vn.gpg.unionportal.service.CurrentUserService;
import vn.gpg.unionportal.service.kpi.KpiActivityService;
import vn.gpg.unionportal.service.kpi.KpiPopulationService;
import vn.gpg.unionportal.service.kpi.KpiRunService;

import java.util.List;

/**
 * Official KPI runs: locking a finished year, reading the locked history, and the year-end population lists
 * those runs are measured against.
 */
@RestController
@RequestMapping("/api/kpi")
public class KpiHistoryController {
    private final KpiRunService runs;
    private final KpiPopulationService populations;
    private final CurrentUserService user;
    private final JdbcTemplate jdbc;

    public KpiHistoryController(KpiRunService runs, KpiPopulationService populations,
                                CurrentUserService user, JdbcTemplate jdbc) {
        this.runs = runs;
        this.populations = populations;
        this.user = user;
        this.jdbc = jdbc;
    }

    public record LinkRequest(Long memberId) {
    }

    public record CancellationRequest(String reason) {
    }

    @PostMapping("/lock")
    public LockResult lock(@RequestParam int year,
                           @RequestParam(defaultValue = "YEAR") PeriodType periodType,
                           @RequestParam(defaultValue = "1") int period) {
        return runs.lock(periodType, year, period);
    }

    @GetMapping("/history")
    public List<KpiRun> history(@RequestParam int fromYear, @RequestParam int toYear,
                                @RequestParam(required = false) Long unitId,
                                @RequestParam(defaultValue = "false") boolean includeSuperseded) {
        return runs.history(fromYear, toYear, unitId, includeSuperseded);
    }

    @GetMapping("/runs/{id}")
    public KpiRunService.RunDetail read(@PathVariable long id) {
        return runs.read(id);
    }

    @GetMapping("/results/{id}/evidence")
    public List<KpiEvidenceRow> evidence(@PathVariable long id,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "50") int size,
                                         @RequestParam(required = false) String role) {
        return runs.evidence(id, page, size, role);
    }

    @GetMapping("/readiness")
    public List<KpiRunService.Readiness> readiness(@RequestParam int year,
                                                   @RequestParam(required = false) Long unitId) {
        return runs.readiness(year, unitId);
    }

    @GetMapping("/statistics")
    public KpiActivityService.ActivityView statistics(@RequestParam int year, @RequestParam Long unitId,
                                                     @RequestParam(defaultValue = "YEAR") PeriodType periodType,
                                                     @RequestParam(defaultValue = "1") int period) {
        return runs.statistics(periodType, year, period, unitId);
    }

    @GetMapping("/populations")
    public List<KpiPopulationService.Population> populations(@RequestParam int year,
                                                            @RequestParam(required = false) Long unitId) {
        return populations.list(year, unitId);
    }

    @GetMapping("/populations/{id}")
    public KpiPopulationService.Population population(@PathVariable long id) {
        return populations.get(id);
    }

    @GetMapping("/evidence/population/{id}")
    public EvidenceRecord populationEvidence(@PathVariable long id) {
        KpiPopulationService.Population population = populations.get(id);
        return new EvidenceRecord("KPI_POPULATION", String.valueOf(id),
                "Danh sách nhân sự cuối năm " + population.year(),
                List.of(new EvidenceField("Phiên bản", String.valueOf(population.revision())),
                        new EvidenceField("Trạng thái", population.status()),
                        new EvidenceField("Nhân sự", String.valueOf(population.members().size())),
                        new EvidenceField("Nguồn đối soát", population.reconciliationNote()),
                        new EvidenceField("Người duyệt", population.approvedBy())),
                List.of());
    }

    @PostMapping("/populations")
    public KpiPopulationService.Population prepare(@RequestBody KpiPopulationService.PrepareRequest request) {
        return populations.prepare(request);
    }

    @PostMapping("/populations/{id}/submit")
    public KpiPopulationService.Population submit(@PathVariable long id) {
        return populations.submit(id);
    }

    @PostMapping("/populations/{id}/approve")
    public KpiPopulationService.Population approve(@PathVariable long id) {
        return populations.approve(id);
    }

    /**
     * Attaches a birthday care record to the member it was for. Birthday coverage is counted per member, so
     * an unlinked record cannot be credited to anyone.
     *
     * <p>This and the two cancellation endpoints write welfare and activity rows. They live here because only
     * the KPI screens use them; the welfare and activity modules are the tidier home once anything else does.
     */
    @PutMapping("/welfare/{id}/link")
    @Transactional
    public void link(@PathVariable long id, @RequestBody LinkRequest request) {
        List<CareOwner> owners = jdbc.query(
                "SELECT union_unit_id, welfare_type FROM welfare_records WHERE id = ?",
                (row, index) -> new CareOwner(row.getLong(1), row.getString(2)), id);
        if (owners.isEmpty()) throw new ResourceNotFoundException("Không tìm thấy hồ sơ");
        CareOwner owner = owners.getFirst();
        if (!"BIRTHDAY".equals(owner.welfareType())) {
            throw new IllegalArgumentException("Chỉ liên kết hồ sơ sinh nhật");
        }
        user.requireUnitAccess(owner.unitId());
        if (request.memberId() != null) {
            List<Long> memberUnits = jdbc.query("SELECT union_unit_id FROM members WHERE id = ?",
                    (row, index) -> row.getLong(1), request.memberId());
            if (memberUnits.isEmpty() || memberUnits.getFirst() != owner.unitId()) {
                throw new IllegalArgumentException("Nhân sự không thuộc CĐCS của hồ sơ");
            }
        }
        jdbc.update("UPDATE welfare_records SET member_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                request.memberId(), id);
    }

    @PutMapping("/welfare/{id}/cancellation")
    @Transactional
    public void careCancellation(@PathVariable long id, @RequestBody CancellationRequest request) {
        cancellation("welfare_records", id, request);
    }

    @PutMapping("/activities/{id}/cancellation")
    @Transactional
    public void activityCancellation(@PathVariable long id, @RequestBody CancellationRequest request) {
        cancellation("union_activities", id, request);
    }

    /** {@code table} is never user input: both callers pass a literal. */
    private void cancellation(String table, long id, CancellationRequest request) {
        requireUnitAccess(table, id);
        String reason = request.reason();
        if (reason == null || reason.isBlank() || reason.length() > 1000) {
            throw new IllegalArgumentException("Cần lý do hủy, tối đa 1000 ký tự");
        }
        jdbc.update("UPDATE " + table + " SET cancellation_reason = ?, updated_at = CURRENT_TIMESTAMP"
                + " WHERE id = ?", reason, id);
    }

    private void requireUnitAccess(String table, long id) {
        List<Long> unitIds = jdbc.query("SELECT union_unit_id FROM " + table + " WHERE id = ?",
                (row, index) -> row.getLong(1), id);
        if (unitIds.isEmpty()) throw new ResourceNotFoundException("Không tìm thấy hồ sơ");
        user.requireUnitAccess(unitIds.getFirst());
    }

    private record CareOwner(long unitId, String welfareType) {
    }
}
