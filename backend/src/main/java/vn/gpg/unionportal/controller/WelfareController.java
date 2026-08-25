package vn.gpg.unionportal.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.gpg.unionportal.dto.ApiModels.WelfareRequest;
import vn.gpg.unionportal.model.WelfareRecord;
import vn.gpg.unionportal.service.WelfareService;

import java.util.List;

@RestController
@RequestMapping("/api/welfare")
public class WelfareController {
    private final WelfareService service;

    public WelfareController(WelfareService service) {
        this.service = service;
    }

    @GetMapping
    public List<WelfareRecord> list(@RequestParam(required = false) Long unitId) {
        return service.list(unitId);
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

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
