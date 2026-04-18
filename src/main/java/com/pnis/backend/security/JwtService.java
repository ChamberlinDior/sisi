package com.pnis.backend.security;

import com.pnis.backend.common.config.AppProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
public class JwtService {

    private final AppProperties appProperties;

    public JwtService(
            AppProperties appProperties) {
        this.appProperties = appProperties;
    }


    private SecretKey getSigningKey() {
        byte[] keyBytes = appProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ===== ACCESS TOKEN =====

    public String generateAccessToken(String username, Long tenantId, String roles) {
        return Jwts.builder()
                .subject(username)
                .claim("tenantId", tenantId)
                .claim("roles", roles)
                .claim("type", "ACCESS")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + appProperties.getJwt().getAccessTokenExpirationMs()))
                .signWith(getSigningKey())
                .compact();
    }

    // ===== REFRESH TOKEN =====

    public String generateRefreshToken(String username) {
        return Jwts.builder()
                .subject(username)
                .claim("type", "REFRESH")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + appProperties.getJwt().getRefreshTokenExpirationMs()))
                .signWith(getSigningKey())
                .compact();
    }

    // ===== MFA TOKEN (courte durée) =====

    public String generateMfaToken(String username) {
        return Jwts.builder()
                .subject(username)
                .claim("type", "MFA_PENDING")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + appProperties.getJwt().getMfaTokenExpirationMs()))
                .signWith(getSigningKey())
                .compact();
    }

    // ===== EXTRACTION =====

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractTenantId(String token) {
        return extractClaim(token, claims -> claims.get("tenantId", Long.class));
    }

    public String extractRoles(String token) {
        return extractClaim(token, claims -> claims.get("roles", String.class));
    }

    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token, String username) {
        try {
            return extractUsername(token).equals(username) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT invalide : {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    public boolean isAccessToken(String token) {
        return "ACCESS".equals(extractTokenType(token));
    }

    public boolean isRefreshToken(String token) {
        return "REFRESH".equals(extractTokenType(token));
    }

    public boolean isMfaPendingToken(String token) {
        return "MFA_PENDING".equals(extractTokenType(token));
    }
}
