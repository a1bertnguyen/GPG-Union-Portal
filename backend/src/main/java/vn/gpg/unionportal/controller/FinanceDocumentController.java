package vn.gpg.unionportal.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.gpg.unionportal.dto.ApiModels.FinanceDocumentView;
import vn.gpg.unionportal.service.FinanceDocumentService;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/finance-documents")
public class FinanceDocumentController {
    private final FinanceDocumentService service;

    public FinanceDocumentController(FinanceDocumentService service) {
        this.service = service;
    }

    @GetMapping
    public List<FinanceDocumentView> list(@RequestParam Long financeEntryId) {
        return service.list(financeEntryId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FinanceDocumentView upload(@RequestParam Long financeEntryId,
                                      @RequestPart("file") MultipartFile file) {
        return service.upload(financeEntryId, file);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        var file = service.download(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.fileName(), StandardCharsets.UTF_8).build().toString())
                .body(file.data());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
