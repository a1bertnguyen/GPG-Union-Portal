package vn.gpg.unionportal.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.gpg.unionportal.service.AuthService;
import vn.gpg.unionportal.dto.AuthModels.AdminProfile;
import vn.gpg.unionportal.dto.AuthModels.LoginRequest;
import vn.gpg.unionportal.dto.AuthModels.LoginResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public AdminProfile me(@AuthenticationPrincipal Jwt jwt) {
        String role = jwt.getClaimAsStringList("roles").stream().findFirst().orElse("ADMIN");
        Number unitId = jwt.getClaim("unitId");
        return new AdminProfile(null, jwt.getSubject(), jwt.getClaimAsString("name"), role,
                unitId == null ? null : unitId.longValue(), jwt.getClaimAsString("unitCode"), null);
    }
}
