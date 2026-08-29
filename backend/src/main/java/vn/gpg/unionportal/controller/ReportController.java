package vn.gpg.unionportal.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.gpg.unionportal.dto.ApiModels.MonthlyReportRequest;
import vn.gpg.unionportal.dto.ApiModels.MonthlySummary;
import vn.gpg.unionportal.model.MonthlyReport;
import vn.gpg.unionportal.service.CurrentUserService;
import vn.gpg.unionportal.service.MonthlyReportService;
import vn.gpg.unionportal.service.ReportingService;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final MonthlyReportService service;
    private final ReportingService reportingService;
    private final CurrentUserService currentUser;

    public ReportController(MonthlyReportService service, ReportingService reportingService,
                            CurrentUserService currentUser) {
        this.service = service;
        this.reportingService = reportingService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<MonthlyReport> list() {
        return service.list();
    }

    @GetMapping("/monthly")
    public MonthlySummary monthly(@RequestParam String month, @RequestParam(required = false) Long unitId) {
        return reportingService.monthlySummary(YearMonth.parse(month), currentUser.scopedUnitId(unitId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MonthlyReport upsert(@Valid @RequestBody MonthlyReportRequest request) {
        return service.upsert(request);
    }

    @PutMapping("/{id}")
    public MonthlyReport update(@PathVariable Long id, @Valid @RequestBody MonthlyReportRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/approve")
    public MonthlyReport approve(@PathVariable Long id) {
        return service.approve(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
