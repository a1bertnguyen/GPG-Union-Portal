package vn.gpg.unionportal.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.gpg.unionportal.dto.ApiModels.ListFacets;
import vn.gpg.unionportal.dto.ApiModels.PageResponse;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.service.UserAccountService;
import vn.gpg.unionportal.dto.UserAccountModels.UserAccountRequest;
import vn.gpg.unionportal.dto.UserAccountModels.UserAccountView;

@RestController
@RequestMapping("/api/admin/users")
public class UserAccountController {
    private final UserAccountService service;

    public UserAccountController(UserAccountService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<UserAccountView> list(@ModelAttribute ListQuery query) {
        if (query.fetchAll()) return PageResponse.ofAll(service.search(query));
        return PageResponse.of(service.page(query)).map(service::view);
    }

    @GetMapping("/facets")
    public ListFacets facets(@ModelAttribute ListQuery query) {
        return service.facets(query);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserAccountView create(@Valid @RequestBody UserAccountRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public UserAccountView update(@PathVariable Long id, @Valid @RequestBody UserAccountRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
