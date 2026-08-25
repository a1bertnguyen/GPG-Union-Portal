package vn.gpg.unionportal.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.gpg.unionportal.service.EngagementService;
import vn.gpg.unionportal.service.CurrentUserService;
import vn.gpg.unionportal.dto.ApiModels.EngagementSummary;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/engagement")
public class EngagementController {
    private final EngagementService engagementService;
    private final CurrentUserService currentUser;

    public EngagementController(EngagementService engagementService, CurrentUserService currentUser) {
        this.engagementService = engagementService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public EngagementSummary summary(@RequestParam(required = false) String month,
                                     @RequestParam(required = false) Long unitId) {
        return engagementService.summary(month == null ? YearMonth.now() : YearMonth.parse(month),
                currentUser.scopedUnitId(unitId));
    }
}
