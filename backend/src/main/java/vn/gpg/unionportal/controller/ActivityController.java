package vn.gpg.unionportal.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.gpg.unionportal.dto.ApiModels.ActivityRequest;
import vn.gpg.unionportal.dto.ApiModels.ListFacets;
import vn.gpg.unionportal.dto.ApiModels.PageResponse;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.model.UnionActivity;
import vn.gpg.unionportal.service.ActivityService;

@RestController
@RequestMapping("/api/activities")
public class ActivityController {
    private final ActivityService service;

    public ActivityController(ActivityService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<UnionActivity> list(@ModelAttribute ListQuery query) {
        return PageResponse.from(query, service::page, service::search);
    }

    @GetMapping("/facets")
    public ListFacets facets(@ModelAttribute ListQuery query) {
        return service.facets(query);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UnionActivity create(@Valid @RequestBody ActivityRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public UnionActivity update(@PathVariable Long id, @Valid @RequestBody ActivityRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
