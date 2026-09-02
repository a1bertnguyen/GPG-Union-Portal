package vn.gpg.unionportal.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import vn.gpg.unionportal.repository.AdminUserRepository;
import vn.gpg.unionportal.security.RaceSafeRateLimiter;
import vn.gpg.unionportal.security.RateLimitFilter;
import vn.gpg.unionportal.security.ActiveSessionFilter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationConverter jwtConverter,
                                                   RaceSafeRateLimiter rateLimiter,
                                                   RateLimitProperties rateLimitProperties,
                                                   AdminUserRepository repository) throws Exception {
        var activeSessionFilter = new ActiveSessionFilter(repository);
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/auth/login", "/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/auth/me", "/api/dashboard", "/api/units", "/api/members", "/api/members/export.xlsx",
                                "/api/member-changes", "/api/member-documents", "/api/member-documents/*/download",
                                "/api/activity-media", "/api/activity-media/*/download",
                                "/api/document-library", "/api/document-library/*/download",
                                "/api/welfare-documents", "/api/welfare-documents/*/download",
                                "/api/finance-documents", "/api/finance-documents/*/download",
                                "/api/welfare", "/api/welfare-policies", "/api/welfare-policies/export.xlsx",
                                "/api/cases", "/api/activities", "/api/finance",
                                "/api/finance/summary", "/api/surveys", "/api/engagement",
                                "/api/reports", "/api/reports/monthly", "/api/kpi", "/api/kpi/metadata",
                                "/api/kpi/evidence/*/*",
                                "/api/realtime/events")
                                .hasAnyRole("ADMIN", "USER")
                        // Whole-dataset numbers behind the metric cards, status dropdowns and analytics
                        // bars on the same screens as the list endpoints above.
                        .requestMatchers(HttpMethod.GET,
                                "/api/meta/enum-labels",
                                "/api/units/facets", "/api/members/facets", "/api/welfare/facets", "/api/welfare-policies/facets",
                                "/api/cases/facets", "/api/cases/issue-groups", "/api/activities/facets",
                                "/api/finance/facets", "/api/surveys/facets",
                                "/api/member-changes/facets", "/api/member-documents/facets",
                                "/api/member-documents/compliance", "/api/member-documents/compliance/facets",
                                "/api/activity-media/facets")
                                .hasAnyRole("ADMIN", "USER")
                        .requestMatchers("/api/spreadsheets/units/**", "/api/spreadsheets/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/spreadsheets/**").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.POST,
                                "/api/spreadsheets/welfare/import", "/api/spreadsheets/welfare-policies/import")
                                .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/spreadsheets/**").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.POST, "/api/surveys/*/responses").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.POST, "/api/welfare/*/approve").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/cases/*/approve").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/reports/*/approve").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/cases/*/submit-approval").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.POST, "/api/member-changes", "/api/member-documents", "/api/activity-media", "/api/welfare-documents", "/api/finance-documents").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.POST, "/api/document-library", "/api/welfare-policies").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/reports").hasRole("USER")
                        .requestMatchers(HttpMethod.POST,
                                "/api/members", "/api/welfare", "/api/cases", "/api/activities",
                                "/api/finance", "/api/surveys").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.PUT, "/api/reports/*").hasRole("USER")
                        .requestMatchers(HttpMethod.PUT, "/api/welfare-policies/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/members/*", "/api/welfare/*", "/api/cases/*", "/api/activities/*",
                                "/api/finance/*", "/api/surveys/*").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/document-library/*", "/api/welfare-policies/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/members/*", "/api/welfare/*", "/api/cases/*", "/api/activities/*",
                                "/api/finance/*", "/api/reports/*", "/api/surveys/*",
                                "/api/member-documents/*", "/api/activity-media/*", "/api/welfare-documents/*", "/api/finance-documents/*").hasAnyRole("ADMIN", "USER")
                        .anyRequest().hasRole("ADMIN"))
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
                        .authenticationEntryPoint((request, response, exception) -> unauthorized(response)))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> unauthorized(response))
                        .accessDeniedHandler((request, response, exception) -> forbidden(response)))
                .addFilterAfter(activeSessionFilter, BearerTokenAuthenticationFilter.class)
                .addFilterAfter(new RateLimitFilter(rateLimiter, rateLimitProperties), ActiveSessionFilter.class);
        return http.build();
    }

    @Bean
    public RaceSafeRateLimiter rateLimiter() {
        return new RaceSafeRateLimiter();
    }

    @Bean
    public UserDetailsService userDetailsService(AdminUserRepository repository) {
        return username -> repository.findByUsernameIgnoreCase(username)
                .map(admin -> User.withUsername(admin.getUsername())
                        .password(admin.getPasswordHash())
                        .roles(admin.getRole())
                        .disabled(!admin.getActive())
                        .build())
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("Tài khoản không tồn tại"));
    }

    @Bean
    public PasswordEncoder passwordEncoder(@Value("${app.auth.bcrypt-strength:12}") int strength) {
        return new BCryptPasswordEncoder(strength);
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        var provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public JwtEncoder jwtEncoder(@Value("${app.auth.jwt-secret}") String secret) {
        return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(secretKey(secret)));
    }

    @Bean
    public JwtDecoder jwtDecoder(@Value("${app.auth.jwt-secret}") String secret,
                                 @Value("${app.auth.issuer}") String issuer) {
        var decoder = NimbusJwtDecoder.withSecretKey(secretKey(secret)).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        var authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    private SecretKey secretKey(String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) throw new IllegalStateException("JWT_SECRET phải có tối thiểu 32 byte");
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    private void unauthorized(HttpServletResponse response) throws java.io.IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"Phiên đăng nhập không hợp lệ hoặc đã hết hạn\"}");
    }

    private void forbidden(HttpServletResponse response) throws java.io.IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"code\":\"FORBIDDEN\",\"message\":\"Tài khoản không có quyền thực hiện thao tác này\"}");
    }
}
