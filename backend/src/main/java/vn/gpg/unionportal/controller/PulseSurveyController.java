package vn.gpg.unionportal.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.gpg.unionportal.dto.ApiModels.PulseSurveyRequest;
import vn.gpg.unionportal.dto.ApiModels.PulseSurveyResponseRequest;
import vn.gpg.unionportal.dto.ApiModels.PulseSurveyView;
import vn.gpg.unionportal.model.DomainEnums.SurveyStatus;
import vn.gpg.unionportal.model.PulseSurveyResponse;
import vn.gpg.unionportal.service.PulseSurveyService;

import java.util.List;

@RestController
@RequestMapping("/api/surveys")
public class PulseSurveyController {
    private final PulseSurveyService service;

    public PulseSurveyController(PulseSurveyService service) {
        this.service = service;
    }

    @GetMapping
    public List<PulseSurveyView> list(@RequestParam(required = false) Long unitId,
                                      @RequestParam(required = false) SurveyStatus status) {
        return service.list(unitId, status);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PulseSurveyView create(@Valid @RequestBody PulseSurveyRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public PulseSurveyView update(@PathVariable Long id, @Valid @RequestBody PulseSurveyRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/{id}/responses")
    public List<PulseSurveyResponse> responses(@PathVariable Long id) {
        return service.responses(id);
    }

    @PostMapping("/{id}/responses")
    @ResponseStatus(HttpStatus.CREATED)
    public PulseSurveyResponse respond(@PathVariable Long id,
                                       @Valid @RequestBody PulseSurveyResponseRequest request) {
        return service.respond(id, request);
    }
}
