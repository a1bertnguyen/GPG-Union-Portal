package vn.gpg.unionportal.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.gpg.unionportal.dto.ApiModels.ListFacets;
import vn.gpg.unionportal.dto.ApiModels.PageResponse;
import vn.gpg.unionportal.dto.ApiModels.WelfareRequest;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.model.WelfareRecord;
import vn.gpg.unionportal.service.WelfareService;

@RestController
@RequestMapping("/api/welfare")
public class WelfareController {
    private final WelfareService service;

    public WelfareController(WelfareService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<WelfareRecord> list(@ModelAttribute ListQuery query) {
        return PageResponse.from(query, service::page, service::search);
    }

    @GetMapping("/facets")
    public ListFacets facets(@ModelAttribute ListQuery query) {
        return service.facets(query);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WelfareRecord create(@Valid @RequestBody WelfareRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public WelfareRecord update(@PathVariable Long id, @Valid @RequestBody WelfareRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/approve")
    public WelfareRecord approve(@PathVariable Long id) {
        return service.approve(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
