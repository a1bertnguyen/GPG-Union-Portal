package vn.gpg.unionportal.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.gpg.unionportal.service.SpreadsheetImportService;
import vn.gpg.unionportal.service.WelfarePolicyExcelService;
import vn.gpg.unionportal.dto.ApiModels.SpreadsheetImportResult;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/spreadsheets")
public class SpreadsheetController {
    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final SpreadsheetImportService service;
    private final WelfarePolicyExcelService policyExcelService;

    public SpreadsheetController(SpreadsheetImportService service, WelfarePolicyExcelService policyExcelService) {
        this.service = service;
        this.policyExcelService = policyExcelService;
    }

    @GetMapping(value = "/{resource}/template.xlsx")
    public ResponseEntity<byte[]> template(@PathVariable String resource) {
        if (policyExcelService.supports(resource)) {
            return ResponseEntity.ok()
                    .contentType(XLSX)
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(policyExcelService.fileName()))
                    .body(policyExcelService.exportWorkbook());
        }
        String fileName = service.templateFileName(resource);
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(fileName))
                .body(service.createTemplate(resource));
    }

    @GetMapping(value = "/reports/export.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportReports(@RequestParam String month,
                                                @RequestParam(required = false) Long unitId) {
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition("bao-cao-thang-" + month + ".xlsx"))
                .body(service.exportReports(month, unitId));
    }

    @PostMapping(value = "/{resource}/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SpreadsheetImportResult importWorkbook(@PathVariable String resource,
                                                   @RequestPart("file") MultipartFile file) {
        if (policyExcelService.supports(resource)) return policyExcelService.importWorkbook(file);
        return service.importWorkbook(resource, file);
    }

    private String contentDisposition(String fileName) {
        String encoded = java.net.URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename=template.xlsx; filename*=UTF-8''" + encoded;
    }
}
