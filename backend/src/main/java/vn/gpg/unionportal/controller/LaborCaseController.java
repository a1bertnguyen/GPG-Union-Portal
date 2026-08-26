package vn.gpg.unionportal.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.gpg.unionportal.dto.ApiModels.CaseGroupCount;
import vn.gpg.unionportal.dto.ApiModels.LaborCaseRequest;
import vn.gpg.unionportal.dto.ApiModels.ListFacets;
import vn.gpg.unionportal.dto.ApiModels.PageResponse;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.model.LaborCase;
import vn.gpg.unionportal.service.LaborCaseService;

import java.util.List;

@RestController
@RequestMapping("/api/cases")
public class LaborCaseController {
    private final LaborCaseService service;

    public LaborCaseController(LaborCaseService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<LaborCase> list(@ModelAttribute ListQuery query) {
        return PageResponse.from(query, service::page, service::search);
    }

    @GetMapping("/facets")
    public ListFacets facets(@ModelAttribute ListQuery query) {
        return service.facets(query);
    }

    /** Per-issue-group rollup for the analytics bars, which summarise the whole filtered set. */
    @GetMapping("/issue-groups")
    public List<CaseGroupCount> issueGroups(@ModelAttribute ListQuery query) {
        return service.issueGroups(query);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LaborCase create(@Valid @RequestBody LaborCaseRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public LaborCase update(@PathVariable Long id, @Valid @RequestBody LaborCaseRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
