package vn.gpg.unionportal.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.gpg.unionportal.dto.KpiModels.EvidenceRecord;
import vn.gpg.unionportal.service.kpi.KpiEvidenceService;

@RestController
@RequestMapping("/api/kpi/evidence")
public class KpiEvidenceController {
    private final KpiEvidenceService service;

    public KpiEvidenceController(KpiEvidenceService service) {
        this.service = service;
    }

    @GetMapping("/{resourceType}/{recordId}")
    public EvidenceRecord read(@PathVariable String resourceType, @PathVariable String recordId) {
        return service.read(resourceType, recordId);
    }
}
