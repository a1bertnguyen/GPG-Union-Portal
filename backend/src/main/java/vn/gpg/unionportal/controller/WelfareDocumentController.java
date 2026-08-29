package vn.gpg.unionportal.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.gpg.unionportal.dto.ApiModels.WelfareDocumentView;
import vn.gpg.unionportal.model.DomainEnums.WelfareDocumentType;
import vn.gpg.unionportal.service.WelfareDocumentService;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/welfare-documents")
public class WelfareDocumentController {
    private final WelfareDocumentService service;

    public WelfareDocumentController(WelfareDocumentService service) {
        this.service = service;
    }

    @GetMapping
    public List<WelfareDocumentView> list(@RequestParam Long welfareRecordId) {
        return service.list(welfareRecordId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public WelfareDocumentView upload(@RequestParam Long welfareRecordId,
                                      @RequestParam WelfareDocumentType documentType,
                                      @RequestPart("file") MultipartFile file) {
        return service.upload(welfareRecordId, documentType, file);
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
