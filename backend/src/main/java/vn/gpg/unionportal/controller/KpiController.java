package vn.gpg.unionportal.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.gpg.unionportal.dto.KpiModels.Dashboard;
import vn.gpg.unionportal.dto.KpiModels.Metadata;
import vn.gpg.unionportal.dto.KpiModels.PeriodType;
import vn.gpg.unionportal.service.KpiService;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/kpi")
public class KpiController {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Bangkok");
    private final KpiService service;

    public KpiController(KpiService service) {
        this.service = service;
    }

    @GetMapping
    public Dashboard evaluate(@RequestParam(required = false) PeriodType periodType,
                              @RequestParam(required = false) Integer year,
                              @RequestParam(required = false) Integer period,
                              @RequestParam(required = false) String month,
                              @RequestParam(required = false) Long unitId) {
        if (month != null && !month.isBlank()) {
            YearMonth legacy = YearMonth.parse(month);
            return service.evaluate(PeriodType.MONTH, legacy.getYear(), legacy.getMonthValue(), unitId);
        }
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        PeriodType resolvedType = periodType == null ? PeriodType.MONTH : periodType;
        int resolvedYear = year == null ? today.getYear() : year;
        int resolvedPeriod = period == null ? defaultPeriod(resolvedType, today) : period;
        return service.evaluate(resolvedType, resolvedYear, resolvedPeriod, unitId);
    }

    @GetMapping("/metadata")
    public Metadata metadata() {
        return service.metadata();
    }

    private int defaultPeriod(PeriodType type, LocalDate today) {
        return switch (type) {
            case MONTH -> today.getMonthValue();
            case QUARTER -> (today.getMonthValue() - 1) / 3 + 1;
            case HALF_YEAR -> today.getMonthValue() <= 6 ? 1 : 2;
            case YEAR -> 1;
        };
    }
}
