package vn.gpg.unionportal.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.gpg.unionportal.dto.ApiModels.UnitKpiView;
import vn.gpg.unionportal.service.KpiService;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/kpi")
public class KpiController {
    private final KpiService service;

    public KpiController(KpiService service) {
        this.service = service;
    }

    @GetMapping
    public List<UnitKpiView> evaluate(@RequestParam String month,
                                      @RequestParam(required = false) Long unitId) {
        return service.evaluate(YearMonth.parse(month), unitId);
    }
}
