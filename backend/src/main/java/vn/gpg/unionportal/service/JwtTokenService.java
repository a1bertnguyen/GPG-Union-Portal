package vn.gpg.unionportal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import vn.gpg.unionportal.model.AdminUser;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class JwtTokenService {
    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final Duration accessTokenTtl;

    public JwtTokenService(JwtEncoder jwtEncoder,
                           @Value("${app.auth.issuer}") String issuer,
                           @Value("${app.auth.access-token-minutes:480}") long accessTokenMinutes) {
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.accessTokenTtl = Duration.ofMinutes(accessTokenMinutes);
    }

    public IssuedToken issue(AdminUser admin, String tokenId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(accessTokenTtl);
        var header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        var claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(admin.getUsername())
                .id(tokenId)
                .claim("name", admin.getFullName())
                .claim("roles", List.of(admin.getRole()));
        if (admin.getUnionUnit() != null) {
            claims.claim("unitId", admin.getUnionUnit().getId())
                    .claim("unitCode", admin.getUnionUnit().getCode());
        }
        String value = jwtEncoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
        return new IssuedToken(value, expiresAt);
    }

    public record IssuedToken(String value, Instant expiresAt) {
    }
}
