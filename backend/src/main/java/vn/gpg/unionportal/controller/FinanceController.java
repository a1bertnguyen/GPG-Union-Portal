package vn.gpg.unionportal.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.gpg.unionportal.dto.ApiModels.FinanceRequest;
import vn.gpg.unionportal.dto.ApiModels.FinanceSummary;
import vn.gpg.unionportal.dto.ApiModels.ListFacets;
import vn.gpg.unionportal.dto.ApiModels.PageResponse;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.model.FinanceEntry;
import vn.gpg.unionportal.service.CurrentUserService;
import vn.gpg.unionportal.service.FinanceService;
import vn.gpg.unionportal.service.ReportingService;

import java.time.YearMonth;

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
    public PageResponse<FinanceEntry> list(@ModelAttribute ListQuery query) {
        return PageResponse.from(query, service::page, service::search);
    }

    /** Monthly income/expense rollup for the dashboards — unrelated to the list facets below. */
    @GetMapping("/summary")
    public FinanceSummary summary(@RequestParam String month, @RequestParam(required = false) Long unitId) {
        return reportingService.financeSummary(YearMonth.parse(month), currentUser.scopedUnitId(unitId));
    }

    @GetMapping("/facets")
    public ListFacets facets(@ModelAttribute ListQuery query) {
        return service.facets(query);
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
