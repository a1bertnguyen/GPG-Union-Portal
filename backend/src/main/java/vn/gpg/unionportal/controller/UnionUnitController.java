package vn.gpg.unionportal.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.gpg.unionportal.dto.ApiModels.UnionUnitRequest;
import vn.gpg.unionportal.model.UnionUnit;
import vn.gpg.unionportal.service.UnionUnitService;

import java.util.List;

@RestController
@RequestMapping("/api/units")
public class UnionUnitController {
    private final UnionUnitService service;

    public UnionUnitController(UnionUnitService service) {
        this.service = service;
    }

    @GetMapping
    public List<UnionUnit> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UnionUnit create(@Valid @RequestBody UnionUnitRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public UnionUnit update(@PathVariable Long id, @Valid @RequestBody UnionUnitRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
