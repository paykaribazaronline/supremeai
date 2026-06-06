# MERMD - Security System

## Overview

The Security feature provides authentication, authorization, rate limiting, and protection against common attacks.

## How It Works

### Architecture Flow

```
Request → AuthenticationFilter → JwtAuthFilter → RateLimiter → Controller
```

### Key Components

| Component                     | File                                        | Purpose                       |
| ----------------------------- | ------------------------------------------- | ----------------------------- |
| `SecurityConfig`              | `config/SecurityConfig.java`                | Spring Security configuration |
| `AuthenticationFilter`        | `filter/AuthenticationFilter.java`          | Firebase auth filter          |
| `JwtAuthFilter`               | `security/JwtAuthFilter.java`               | JWT token validation          |
| `RateLimitingFilter`          | `filter/RateLimitingFilter.java`            | Rate limiting                 |
| `JwtUtil`                     | `security/JwtUtil.java`                     | JWT utilities                 |
| `EncryptionService`           | `security/EncryptionService.java`           | Data encryption               |
| `BruteForceProtectionService` | `security/BruteForceProtectionService.java` | Brute force protection        |

### Authentication Flow

1. **Firebase Authentication**
   - Client signs in with Firebase
   - Firebase ID token sent to backend
   - `AuthenticationFilter` validates token

2. **JWT Token Generation**
   - Validated Firebase token exchanged for JWT
   - JWT contains user roles (ADMIN, USER)
   - Token returned to client

3. **Request Authentication**
   - Client sends JWT in Authorization header
   - `JwtAuthFilter` validates token
   - Principal set in SecurityContext

### Rate Limiting

#### Implementation

| Type       | File                       | Description               |
| ---------- | -------------------------- | ------------------------- |
| Interface  | `RateLimiter.java`         | Contract                  |
| Redis      | `RedisRateLimiter.java`    | Distributed rate limiting |
| In-Memory  | `InMemoryRateLimiter.java` | Local rate limiting       |
| Properties | `RateLimitProperties.java` | Configuration             |

#### RateLimitingService

- Manages rate limiting strategies
- Configurable limits per endpoint
- Returns 429 when exceeded

### Authorization

#### Role-Based Access Control

- `ROLE_ADMIN` - Full system access
- `ROLE_USER` - Standard user access

#### Endpoint Security

```java
// Admin endpoints
.requestMatchers("/api/admin/**").hasRole("ADMIN")

// Authenticated endpoints
.anyRequest().authenticated()
```

### Security Features

| Feature          | Implementation               |
| ---------------- | ---------------------------- |
| CSRF Protection  | CookieCsrfTokenRepository    |
| CORS             | Configurable allowed origins |
| Security Headers | CSP, HSTS, X-Frame-Options   |
| JWT Validation   | Signature + expiration check |

### Services

#### SecretManagerService

- Manages secrets in production
- Integrates with GCP Secret Manager

#### UnifiedSecretsService

- Unified interface for secrets
- Fallback mechanisms

#### FirebaseSecretsService

- Firebase-specific secrets
- Project configuration

### Integration Points

- `Application.java` - JWT secret validation
- `SecurityConfig.java` - Security chain setup
- Filter chain ordering
