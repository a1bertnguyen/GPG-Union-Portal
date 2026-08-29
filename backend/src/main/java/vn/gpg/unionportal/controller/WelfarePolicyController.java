package vn.gpg.unionportal.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.gpg.unionportal.dto.ApiModels.ListFacets;
import vn.gpg.unionportal.dto.ApiModels.PageResponse;
import vn.gpg.unionportal.dto.ApiModels.WelfarePolicyRequest;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.model.WelfarePolicy;
import vn.gpg.unionportal.service.WelfarePolicyExcelService;
import vn.gpg.unionportal.service.WelfarePolicyService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/welfare-policies")
public class WelfarePolicyController {
    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final WelfarePolicyService service;
    private final WelfarePolicyExcelService excelService;

    public WelfarePolicyController(WelfarePolicyService service, WelfarePolicyExcelService excelService) {
        this.service = service;
        this.excelService = excelService;
    }

    @GetMapping
    public PageResponse<WelfarePolicy> list(@ModelAttribute ListQuery query) {
        return PageResponse.from(query, service::page, service::search);
    }

    @GetMapping("/facets")
    public ListFacets facets(@ModelAttribute ListQuery query) {
        return service.facets(query);
    }

    @GetMapping("/export.xlsx")
    public ResponseEntity<byte[]> export() {
        String fileName = "bang-chinh-sach-cham-lo.xlsx";
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=welfare-policies.xlsx; filename*=UTF-8''" + encoded)
                .body(excelService.exportWorkbook());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WelfarePolicy create(@Valid @RequestBody WelfarePolicyRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public WelfarePolicy update(@PathVariable Long id, @Valid @RequestBody WelfarePolicyRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
