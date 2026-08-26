package vn.gpg.unionportal.controller;

import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.gpg.unionportal.dto.ApiModels.*;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.model.DomainEnums.MemberDocumentType;
import vn.gpg.unionportal.service.MemberWorkspaceService;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api")
public class MemberWorkspaceController {
    private final MemberWorkspaceService service;

    public MemberWorkspaceController(MemberWorkspaceService service) {
        this.service = service;
    }

    @GetMapping("/member-changes")
    public PageResponse<MemberChangeView> changes(@RequestParam(required = false) Long memberId,
                                                  @ModelAttribute ListQuery query) {
        return query.fetchAll()
                ? PageResponse.ofAll(service.searchChanges(query, memberId))
                : PageResponse.of(service.pageChanges(query, memberId));
    }

    @GetMapping("/member-changes/facets")
    public ListFacets changeFacets(@RequestParam(required = false) Long memberId,
                                   @ModelAttribute ListQuery query) {
        return service.changeFacets(query, memberId);
    }

    @PostMapping("/member-changes")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberChangeView createChange(@Valid @RequestBody MemberChangeRequest request) {
        return service.createChange(request);
    }

    @GetMapping("/member-documents")
    public PageResponse<MemberDocumentView> documents(@RequestParam(required = false) Long memberId,
                                                      @ModelAttribute ListQuery query) {
        return query.fetchAll()
                ? PageResponse.ofAll(service.searchDocuments(query, memberId))
                : PageResponse.of(service.pageDocuments(query, memberId));
    }

    /**
     * A page of members with their required-document status. The compliance grid used to build this
     * by cross-joining every member against every document in the browser, which could not be paged.
     */
    @GetMapping("/member-documents/compliance")
    public PageResponse<MemberComplianceView> compliance(@ModelAttribute ListQuery query) {
        return PageResponse.from(query, service::pageCompliance, service::searchCompliance);
    }

    @GetMapping("/member-documents/compliance/facets")
    public ListFacets complianceFacets(@ModelAttribute ListQuery query) {
        return service.complianceFacets(query);
    }

    @PostMapping(value = "/member-documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MemberDocumentView upload(@RequestParam("memberId") Long memberId,
                                     @RequestParam("documentType") MemberDocumentType documentType,
                                     @RequestPart("file") MultipartFile file) {
        return service.uploadDocument(memberId, documentType, file);
    }

    @GetMapping("/member-documents/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        var file = service.downloadDocument(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.fileName(), StandardCharsets.UTF_8).build().toString())
                .body(file.data());
    }

    @DeleteMapping("/member-documents/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.deleteDocument(id);
    }
}
