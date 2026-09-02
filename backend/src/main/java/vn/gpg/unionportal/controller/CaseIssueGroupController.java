package vn.gpg.unionportal.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.gpg.unionportal.dto.ApiModels.CaseIssueGroupRequest;
import vn.gpg.unionportal.dto.ApiModels.ListFacets;
import vn.gpg.unionportal.dto.ApiModels.PageResponse;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.model.CaseIssueGroup;
import vn.gpg.unionportal.service.CaseIssueGroupService;

@RestController
@RequestMapping("/api/case-issue-groups")
public class CaseIssueGroupController {
    private final CaseIssueGroupService service;

    public CaseIssueGroupController(CaseIssueGroupService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<CaseIssueGroup> list(@ModelAttribute ListQuery query) {
        return PageResponse.from(query, service::page, service::search);
    }

    @GetMapping("/facets")
    public ListFacets facets(@ModelAttribute ListQuery query) {
        return service.facets(query);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CaseIssueGroup create(@Valid @RequestBody CaseIssueGroupRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public CaseIssueGroup update(@PathVariable Long id, @Valid @RequestBody CaseIssueGroupRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
