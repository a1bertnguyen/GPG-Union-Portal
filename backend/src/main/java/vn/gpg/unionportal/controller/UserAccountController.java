package vn.gpg.unionportal.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.gpg.unionportal.service.UserAccountService;
import vn.gpg.unionportal.dto.UserAccountModels.UserAccountRequest;
import vn.gpg.unionportal.dto.UserAccountModels.UserAccountView;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class UserAccountController {
    private final UserAccountService service;

    public UserAccountController(UserAccountService service) {
        this.service = service;
    }

    @GetMapping
    public List<UserAccountView> list() {
        return service.list();
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
}
