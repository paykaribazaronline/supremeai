package com.supremeai.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

@Component
public class JwtUtil {

    private static final long TOKEN_TTL_MS = 1000L * 60 * 60 * 10;

    @Value("${JWT_SECRET}")
    private String secret;

    @Value("${JWT_ISSUER:supremeai}")
    private String issuer;

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String getUsername(String token) {
        return getClaim(token, Claims::getSubject);
    }

    public String getRole(String token) {
        return getClaim(token, claims -> {
            String role = (String) claims.get("role");
            if (role == null || role.isBlank()) {
                throw new JwtException("JWT role claim is missing");
            }
            return normalizeRole(role);
        });
    }

    public boolean validateToken(String token) {
        Claims claims = parseClaims(token);
        String subject = claims.getSubject();
        String role = (String) claims.get("role");

        if (subject == null || subject.isBlank()) {
            throw new JwtException("JWT subject is missing");
        }
        if (role == null || role.isBlank()) {
            throw new JwtException("JWT role is missing");
        }
        if (claims.getExpiration() == null || claims.getExpiration().before(new Date())) {
            throw new JwtException("JWT token is expired");
        }
        if (!Objects.equals(issuer, claims.getIssuer())) {
            throw new JwtException("JWT issuer is invalid");
        }

        return true;
    }

    public String generateToken(String username, String role) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Role is mandatory for token generation");
        }
        Date issuedAt = new Date();
        return Jwts.builder()
                .subject(username)
                .issuer(issuer)
                .issuedAt(issuedAt)
                .expiration(new Date(issuedAt.getTime() + TOKEN_TTL_MS))
                .claim("role", normalizeRole(role))
                .signWith(getSigningKey())
                .compact();
    }

    private <T> T getClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = parseClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        return role.trim().toUpperCase(Locale.ROOT);
    }
}
