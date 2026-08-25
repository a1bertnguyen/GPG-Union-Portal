package vn.gpg.unionportal.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.gpg.unionportal.dto.ApiModels.FinanceRequest;
import vn.gpg.unionportal.dto.ApiModels.FinanceSummary;
import vn.gpg.unionportal.model.FinanceEntry;
import vn.gpg.unionportal.service.CurrentUserService;
import vn.gpg.unionportal.service.FinanceService;
import vn.gpg.unionportal.service.ReportingService;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/finance")
public class FinanceController {
    private final FinanceService service;
    private final ReportingService reportingService;
    private final CurrentUserService currentUser;

    public FinanceController(FinanceService service, ReportingService reportingService,
                             CurrentUserService currentUser) {
        this.service = service;
        this.reportingService = reportingService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<FinanceEntry> list(@RequestParam(required = false) Long unitId) {
        return service.list(unitId);
    }

    @GetMapping("/summary")
    public FinanceSummary summary(@RequestParam String month, @RequestParam(required = false) Long unitId) {
        return reportingService.financeSummary(YearMonth.parse(month), currentUser.scopedUnitId(unitId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FinanceEntry create(@Valid @RequestBody FinanceRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public FinanceEntry update(@PathVariable Long id, @Valid @RequestBody FinanceRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
