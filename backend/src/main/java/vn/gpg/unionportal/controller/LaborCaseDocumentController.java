package vn.gpg.unionportal.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.gpg.unionportal.dto.ApiModels.LaborCaseDocumentView;
import vn.gpg.unionportal.dto.ApiModels.PageResponse;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.service.LaborCaseDocumentService;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/case-documents")
public class LaborCaseDocumentController {
    private final LaborCaseDocumentService service;

    public LaborCaseDocumentController(LaborCaseDocumentService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<LaborCaseDocumentView> list(@RequestParam(required = false) Long caseId,
                                                    @ModelAttribute ListQuery query) {
        return query.fetchAll() ? PageResponse.ofAll(service.search(caseId)) : PageResponse.of(service.page(query, caseId));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public LaborCaseDocumentView upload(@RequestParam("caseId") Long caseId, @RequestPart("file") MultipartFile file) {
        return service.upload(caseId, file);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        var file = service.download(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.contentType()))
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
