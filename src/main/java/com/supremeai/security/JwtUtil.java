package com.supremeai.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT utility using cached {@link SecretKey} and {@link JwtParser}.
 *
 * <p>Cache lifecycle:
 *
 * <ul>
 *   <li>Cold-start: {@link #init()} via {@code @PostConstruct} pre-builds both objects.
 *   <li>Field-injection (tests): {@code #init()} is never called, so callers that hit {@link
 *       #parseClaims} or {@link #generateToken} are served by a thin lazy-init proxy that rebuilds
 *       from whatever values are currently injected.
 * </ul>
 */
@Component
public class JwtUtil {

  private static final long ACCESS_TOKEN_TTL_MS = 1000L * 60 * 60 * 48; // 48 h
  private static final long REFRESH_TOKEN_TTL_MS = 1000L * 60 * 60 * 24 * 7; // 7 d

  @Value("${jwt.secret}")
  private String secret;

  @Value("${JWT_ISSUER:supremeai}")
  private String issuer;

  // Lazy-init cache: rebuilt from the live secret whenever this is null.
  // Matches lifecycle of both @PostConstruct-init and test-field-set patterns.
  private SecretKey cachedKey;
  private JwtParser cachedParser;

  /** Pre-builds cache at cold-start. No-op in tests that set fields via reflection. */
  @PostConstruct
  public void init() {
    if (secret == null || secret.length() < 32) {
      throw new IllegalStateException(
          "JWT_SECRET must be at least 32 characters for HS256 algorithm");
    }
    cachedKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    cachedParser = Jwts.parser().verifyWith(cachedKey).build();
  }

  /**
   * Returns the signing key, lazily building it from the live {@link #secret} field if the cache
   * has not yet been populated (covers both cold-start and test scenarios).
   */
  private SecretKey getSigningKey() {
    if (cachedKey == null || cachedParser == null) {
      cachedKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
      cachedParser = Jwts.parser().verifyWith(cachedKey).build();
    }
    return cachedKey;
  }

  /**
   * Returns the JWT parser consistent with the current {@link #secret}. Always up-to-date with
   * whatever key was last built.
   */
  private JwtParser getCachedParser() {
    getSigningKey(); // no-op when cache already warm
    return cachedParser;
  }

  public String getUsername(String token) {
    return getClaim(token, Claims::getSubject);
  }

  public String getRole(String token) {
    return getClaim(
        token,
        claims -> {
          String role = (String) claims.get("role");
          if (role == null || role.isBlank()) {
            throw new JwtException("JWT role claim is missing");
          }
          return normalizeRole(role);
        });
  }

  public boolean validateToken(String token) {
    Claims claims = parseClaims(token);
    Objects.requireNonNull(claims.getSubject(), "subject");
    Objects.requireNonNull(claims.get("role"), "role");

    if (claims.getExpiration() == null || claims.getExpiration().before(new Date())) {
      throw new JwtException("JWT token is expired");
    }
    if (!Objects.equals(issuer, claims.getIssuer())) {
      throw new JwtException("JWT issuer is invalid");
    }
    return true;
  }

  public String generateAccessToken(String username, String role) {
    return generateToken(username, role, ACCESS_TOKEN_TTL_MS);
  }

  public String generateRefreshToken(String username, String role) {
    return generateToken(username, role, REFRESH_TOKEN_TTL_MS);
  }

  /** Backward-compatible: generates access token (48 h TTL). */
  public String generateToken(String username, String role) {
    return generateAccessToken(username, role);
  }

  private String generateToken(String username, String role, long ttlMs) {
    if (role == null || role.isBlank()) {
      throw new IllegalArgumentException("Role is mandatory for token generation");
    }
    Date issuedAt = new Date();
    return Jwts.builder()
        .subject(username)
        .issuer(issuer)
        .issuedAt(issuedAt)
        .expiration(new Date(issuedAt.getTime() + ttlMs))
        .claim("role", normalizeRole(role))
        .signWith(getSigningKey())
        .compact();
  }

  private <T> T getClaim(String token, Function<Claims, T> claimsResolver) {
    return claimsResolver.apply(parseClaims(token));
  }

  private Claims parseClaims(String token) {
    return getCachedParser().parseSignedClaims(token).getPayload();
  }

  private String normalizeRole(String role) {
    if (role == null || role.isBlank()) return null;
    return role.trim().toUpperCase(Locale.ROOT);
  }
}
