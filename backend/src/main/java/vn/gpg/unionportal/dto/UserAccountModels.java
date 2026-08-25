package vn.gpg.unionportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class UserAccountModels {
    private UserAccountModels() {
    }

    public record UserAccountRequest(
            @NotBlank @Size(max = 80) String username,
            @NotBlank @Size(max = 150) String fullName,
            @NotBlank @Pattern(regexp = "ADMIN|USER") String role,
            Long unionUnitId,
            @NotNull Boolean active,
            @Size(min = 8, max = 200) String password) {
    }

    public record UserAccountView(
            Long id,
            String username,
            String fullName,
            String role,
            boolean active,
            Long unionUnitId,
            String unionUnitCode,
            String unionUnitName,
            Instant lastLoginAt,
            Instant createdAt) {
    }
}
