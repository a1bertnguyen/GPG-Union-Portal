package vn.gpg.unionportal.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.gpg.unionportal.service.ReportingService;
import vn.gpg.unionportal.service.CurrentUserService;
import vn.gpg.unionportal.dto.ApiModels.DashboardSummary;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final ReportingService reportingService;
    private final CurrentUserService currentUser;

    public DashboardController(ReportingService reportingService, CurrentUserService currentUser) {
        this.reportingService = reportingService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public DashboardSummary dashboard(@RequestParam(required = false) String month) {
        return reportingService.dashboard(
                month == null || month.isBlank() ? YearMonth.now() : YearMonth.parse(month),
                currentUser.scopedUnitId(null));
    }
}
