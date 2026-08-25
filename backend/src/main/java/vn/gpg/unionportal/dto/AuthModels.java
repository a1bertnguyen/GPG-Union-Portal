package vn.gpg.unionportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class AuthModels {
    private AuthModels() {
    }

    public record LoginRequest(
            @NotBlank @Size(max = 80) String username,
            @NotBlank @Size(max = 200) String password) {
    }

    public record AdminProfile(
            Long id,
            String username,
            String fullName,
            String role,
            Long unionUnitId,
            String unionUnitCode,
            String unionUnitName) {
    }

    public record LoginResponse(
            String accessToken,
            String tokenType,
            Instant expiresAt,
            AdminProfile user) {
    }
}
