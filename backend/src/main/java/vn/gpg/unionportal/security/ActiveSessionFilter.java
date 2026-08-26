package vn.gpg.unionportal.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.gpg.unionportal.repository.AdminUserRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class ActiveSessionFilter extends OncePerRequestFilter {
    private static final String REPLACED_MESSAGE =
            "Tài khoản đã được đăng nhập ở thiết bị khác. Phiên hiện tại đã bị đăng xuất.";

    private final AdminUserRepository repository;

    public ActiveSessionFilter(AdminUserRepository repository) {
        this.repository = repository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            String username = jwtAuthentication.getToken().getSubject();
            String tokenId = jwtAuthentication.getToken().getId();
            String activeTokenId = repository.findActiveTokenId(username).orElse(null);
            if (!Objects.equals(tokenId, activeTokenId)) {
                SecurityContextHolder.clearContext();
                rejectReplacedSession(response);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private void rejectReplacedSession(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"code\":\"SESSION_REPLACED\",\"message\":\"" + REPLACED_MESSAGE + "\"}");
    }
}
