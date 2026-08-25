package vn.gpg.unionportal.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    public boolean isAdmin() {
        Authentication authentication = authentication();
        return authentication == null || authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    public Long scopedUnitId(Long requestedUnitId) {
        if (isAdmin()) return requestedUnitId;
        return requiredUserUnitId();
    }

    public void requireUnitAccess(Long unitId) {
        if (!isAdmin() && !requiredUserUnitId().equals(unitId)) {
            throw new AccessDeniedException("Tài khoản chỉ được truy cập dữ liệu thuộc CĐCS của mình");
        }
    }

    public String username() {
        Authentication authentication = authentication();
        return authentication == null ? "system" : authentication.getName();
    }

    private Long requiredUserUnitId() {
        Authentication authentication = authentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException("JWT của tài khoản USER thiếu phạm vi CĐCS");
        }
        Number unitId = jwt.getClaim("unitId");
        if (unitId == null) throw new AccessDeniedException("Tài khoản USER chưa được gán CĐCS");
        return unitId.longValue();
    }

    private Authentication authentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null || !authentication.isAuthenticated() ? null : authentication;
    }
}
