package vn.gpg.unionportal.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.exception.ResourceNotFoundException;
import vn.gpg.unionportal.repository.AdminUserRepository;
import vn.gpg.unionportal.dto.AuthModels.AdminProfile;
import vn.gpg.unionportal.dto.AuthModels.LoginRequest;
import vn.gpg.unionportal.dto.AuthModels.LoginResponse;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final AdminUserRepository repository;
    private final JwtTokenService tokenService;

    public AuthService(AuthenticationManager authenticationManager,
                       AdminUserRepository repository,
                       JwtTokenService tokenService) {
        this.authenticationManager = authenticationManager;
        this.repository = repository;
        this.tokenService = tokenService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(username, request.password()));
        var admin = repository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản quản trị"));
        String tokenId = UUID.randomUUID().toString();
        admin.setLastLoginAt(Instant.now());
        admin.setActiveTokenId(tokenId);
        repository.save(admin);
        var token = tokenService.issue(admin, tokenId);
        var unit = admin.getUnionUnit();
        return new LoginResponse(token.value(), "Bearer", token.expiresAt(),
                new AdminProfile(admin.getId(), admin.getUsername(), admin.getFullName(), admin.getRole(),
                        unit == null ? null : unit.getId(),
                        unit == null ? null : unit.getCode(),
                        unit == null ? null : unit.getName()));
    }
}
