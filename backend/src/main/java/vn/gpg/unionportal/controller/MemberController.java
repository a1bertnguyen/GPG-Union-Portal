package vn.gpg.unionportal.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.gpg.unionportal.dto.ApiModels.ListFacets;
import vn.gpg.unionportal.dto.ApiModels.MemberRequest;
import vn.gpg.unionportal.dto.ApiModels.PageResponse;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.model.Member;
import vn.gpg.unionportal.service.MemberService;
import vn.gpg.unionportal.service.MemberExcelService;

@RestController
@RequestMapping("/api/members")
public class MemberController {
    private final MemberService service;
    private final MemberExcelService excelService;

    public MemberController(MemberService service, MemberExcelService excelService) {
        this.service = service;
        this.excelService = excelService;
    }

    @GetMapping(value = "/export.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportExcel(@ModelAttribute ListQuery query) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=doan-vien.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelService.exportMembers(query));
    }

    @GetMapping
    public PageResponse<Member> list(@ModelAttribute ListQuery query) {
        return PageResponse.from(query, service::page, service::search);
    }

    /**
     * Whole-dataset numbers behind the metric cards and the status dropdown.
     * Named {@code /facets} rather than {@code /summary} because some modules already expose a
     * {@code /summary} endpoint with a different meaning (see {@code FinanceController}).
     */
    @GetMapping("/facets")
    public ListFacets facets(@ModelAttribute ListQuery query) {
        return service.facets(query);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Member create(@Valid @RequestBody MemberRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public Member update(@PathVariable Long id, @Valid @RequestBody MemberRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
