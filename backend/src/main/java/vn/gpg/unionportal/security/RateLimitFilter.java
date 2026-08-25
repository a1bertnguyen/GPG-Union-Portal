package vn.gpg.unionportal.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.gpg.unionportal.config.RateLimitProperties;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class RateLimitFilter extends OncePerRequestFilter {
    public static final String LIMIT_HEADER = "X-RateLimit-Limit";
    public static final String REMAINING_HEADER = "X-RateLimit-Remaining";

    private final RaceSafeRateLimiter limiter;
    private final RateLimitProperties properties;

    public RateLimitFilter(RaceSafeRateLimiter limiter, RateLimitProperties properties) {
        this.limiter = limiter;
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.isEnabled()
                || "OPTIONS".equalsIgnoreCase(request.getMethod())
                || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Policy policy = policy(request.getRequestURI());
        String key = policy.key() + ':' + clientIdentity(request);
        var decision = limiter.tryAcquire(key, policy.limit(),
                Duration.ofSeconds(Math.max(1, properties.getWindowSeconds())));

        response.setHeader(LIMIT_HEADER, Integer.toString(decision.limit()));
        response.setHeader(REMAINING_HEADER, Integer.toString(decision.remaining()));
        if (!decision.allowed()) {
            reject(response, decision.retryAfterSeconds());
            return;
        }
        chain.doFilter(request, response);
    }

    private Policy policy(String path) {
        if ("/api/auth/login".equals(path)) {
            return new Policy("login", properties.getLoginRequests());
        }
        if ("/api/realtime/events".equals(path)) {
            return new Policy("realtime", properties.getRealtimeRequests());
        }
        return new Policy("default", properties.getDefaultRequests());
    }

    private String clientIdentity(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "user:" + authentication.getName();
        }
        String remoteAddress = request.getRemoteAddr();
        return "ip:" + (remoteAddress == null || remoteAddress.isBlank() ? "unknown" : remoteAddress);
    }

    private void reject(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        response.setStatus(429);
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"code\":\"RATE_LIMIT_EXCEEDED\","
                + "\"message\":\"Quá nhiều yêu cầu, vui lòng thử lại sau\","
                + "\"retryAfterSeconds\":" + retryAfterSeconds + '}');
    }

    private record Policy(String key, int limit) {
    }
}
