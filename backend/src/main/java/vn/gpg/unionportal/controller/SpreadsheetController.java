package vn.gpg.unionportal.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.gpg.unionportal.service.SpreadsheetImportService;
import vn.gpg.unionportal.dto.ApiModels.SpreadsheetImportResult;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/spreadsheets")
public class SpreadsheetController {
    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final SpreadsheetImportService service;

    public SpreadsheetController(SpreadsheetImportService service) {
        this.service = service;
    }

    @GetMapping(value = "/{resource}/template.xlsx")
    public ResponseEntity<byte[]> template(@PathVariable String resource) {
        String fileName = service.templateFileName(resource);
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(fileName))
                .body(service.createTemplate(resource));
    }

    @PostMapping(value = "/{resource}/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SpreadsheetImportResult importWorkbook(@PathVariable String resource,
                                                   @RequestPart("file") MultipartFile file) {
        return service.importWorkbook(resource, file);
    }

    private String contentDisposition(String fileName) {
        String encoded = java.net.URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename=template.xlsx; filename*=UTF-8''" + encoded;
    }
}
