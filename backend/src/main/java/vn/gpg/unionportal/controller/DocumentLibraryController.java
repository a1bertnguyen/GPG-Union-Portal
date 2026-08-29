package vn.gpg.unionportal.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.gpg.unionportal.dto.ApiModels.DocumentLibraryView;
import vn.gpg.unionportal.dto.ApiModels.PageResponse;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.service.DocumentLibraryService;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/document-library")
public class DocumentLibraryController {
    private final DocumentLibraryService service;

    public DocumentLibraryController(DocumentLibraryService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<DocumentLibraryView> list(@ModelAttribute ListQuery query) {
        return PageResponse.from(query, service::page, service::search);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentLibraryView upload(@RequestParam Long unionUnitId,
                                      @RequestParam String category,
                                      @RequestParam String title,
                                      @RequestParam(required = false) String description,
                                      @RequestPart("file") MultipartFile file) {
        return service.upload(unionUnitId, category, title, description, file);
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
