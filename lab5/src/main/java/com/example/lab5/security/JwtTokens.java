package com.example.lab5.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

@Component
public class JwtTokens {
    private static final String ISSUER = "https://recycling.lab5.local";

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final Clock clock;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtTokens(Clock clock, @Value("${app.jwt.secret}") String encodedKey,
                     @Value("${app.jwt.access-ttl}") Duration accessTtl,
                     @Value("${app.jwt.refresh-ttl}") Duration refreshTtl) {
        this.clock = clock;
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
        if (accessTtl.toSeconds() < 1 || refreshTtl.compareTo(accessTtl) <= 0
                || accessTtl.getNano() != 0 || refreshTtl.getNano() != 0) {
            throw new IllegalArgumentException("JWT TTL: целые секунды, 0 < access < refresh");
        }

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(encodedKey);
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("JWT_SECRET должен быть ключом в Base64");
        }
        if (bytes.length < 32) {
            throw new IllegalArgumentException("JWT_SECRET должен содержать не менее 32 байт до Base64");
        }

        SecretKey key = new SecretKeySpec(bytes, "HmacSHA256");
        encoder = NimbusJwtEncoder.withSecretKey(key).algorithm(MacAlgorithm.HS256).build();
        NimbusJwtDecoder verifier = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256).build();
        // Проверяем claims ниже по тому же Clock, который используется при выдаче.
        verifier.setJwtValidator(jwt -> OAuth2TokenValidatorResult.success());
        decoder = verifier;
    }

    public Jwt issueAccess(String username, long userId, Instant now) {
        return issue(username, userId, now, accessTtl, "access");
    }

    public Jwt issueRefresh(String username, long userId, Instant now) {
        return issue(username, userId, now, refreshTtl, "refresh");
    }

    public Jwt readAccess(String token) {
        return verify(token, "access");
    }

    public Jwt readRefresh(String token) {
        return verify(token, "refresh");
    }

    public long userId(Jwt jwt) {
        return Long.parseLong(jwt.getClaimAsString("uid"));
    }

    private Jwt issue(String username, long userId, Instant now, Duration ttl, String type) {
        Instant issuedAt = now.truncatedTo(ChronoUnit.SECONDS);
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(ISSUER)
                .subject(username).issuedAt(issuedAt).expiresAt(issuedAt.plus(ttl))
                .id(UUID.randomUUID().toString()).claim("type", type)
                .claim("uid", Long.toString(userId)).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims));
    }

    private Jwt verify(String token, String expectedType) {
        Jwt jwt = decoder.decode(token);
        Instant now = clock.instant();
        Instant issued = jwt.getIssuedAt();
        Instant expires = jwt.getExpiresAt();
        if (!ISSUER.equals(jwt.getClaimAsString("iss"))
                || !expectedType.equals(jwt.getClaims().get("type"))
                || jwt.getSubject() == null || jwt.getSubject().isBlank()
                || issued == null || issued.isAfter(now)
                || expires == null || !expires.isAfter(now) || !expires.isAfter(issued)
                || (jwt.getNotBefore() != null && jwt.getNotBefore().isAfter(now))
                || !validId(jwt)) {
            throw new BadJwtException("Недействительный токен");
        }
        return jwt;
    }

    private boolean validId(Jwt jwt) {
        if (jwt.getId() == null || !(jwt.getClaims().get("uid") instanceof String)) {
            return false;
        }
        try {
            return UUID.fromString(jwt.getId()).toString().equals(jwt.getId()) && userId(jwt) > 0;
        } catch (IllegalArgumentException error) {
            return false;
        }
    }
}
