package vn.gpg.unionportal.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.gpg.unionportal.dto.ApiModels.ListFacets;
import vn.gpg.unionportal.dto.ApiModels.PageResponse;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.model.IntegrationRun;
import vn.gpg.unionportal.service.CurrentUserService;
import vn.gpg.unionportal.service.DataIntegrationService;
import vn.gpg.unionportal.dto.ApiModels.IntegrationImportResult;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/integrations")
public class IntegrationController {
    private final DataIntegrationService integrationService;
    private final CurrentUserService currentUser;

    public IntegrationController(DataIntegrationService integrationService, CurrentUserService currentUser) {
        this.integrationService = integrationService;
        this.currentUser = currentUser;
    }

    @GetMapping("/runs")
    public PageResponse<IntegrationRun> runs(@ModelAttribute ListQuery query) {
        return PageResponse.from(query, integrationService::pageRuns, integrationService::searchRuns);
    }

    @GetMapping("/runs/facets")
    public ListFacets runFacets(@ModelAttribute ListQuery query) {
        return integrationService.runFacets(query);
    }

    @PostMapping(value = "/finance/import.csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public IntegrationImportResult importFinance(@RequestPart("file") MultipartFile file) {
        return integrationService.importFinance(file, currentUser.username());
    }

    @GetMapping(value = "/finance/export.csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> exportFinance(@RequestParam(required = false) String month,
                                                @RequestParam(required = false) Long unitId) {
        YearMonth selectedMonth = month == null || month.isBlank() ? null : YearMonth.parse(month);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tai-chinh-noi-bo.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(integrationService.exportFinance(selectedMonth, unitId));
    }
}
