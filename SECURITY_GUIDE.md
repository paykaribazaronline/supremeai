# 🔒 SECURITY GUIDE

**AI Multi-Agent App Generator - Security Architecture & Best Practices**

---

## 1. OVERVIEW

This document covers security implementation across the supremeai system. The architecture follows a **defense-in-depth** strategy with:

- **Layer 1:** Authentication & Authorization

- **Layer 2:** Secret Management & Encryption

- **Layer 3:** API Security & Rate Limiting

- **Layer 4:** Input Validation & Injection Prevention

- **Layer 5:** Audit Logging & Monitoring

- **Layer 6:** Network & Transport Security

---

## 2. SECRET MANAGEMENT

### 2.1 API Key Storage

**Problem:** Hardcoded API keys in source code are a critical security risk.

**Solution:** Google Cloud Secret Manager

```java
// Use SecretManager.java for all API keys
SecretManager secretManager = new SecretManager("supremeai");

// Retrieve secrets (cached with 1-hour TTL)
String deepseekKey = secretManager.getSecret("deepseek-api-key");
String groqKey = secretManager.getSecret("groq-api-key");
String anthropicKey = secretManager.getSecret("anthropic-api-key");
String openaiKey = secretManager.getSecret("openai-api-key");

```

### 2.2 Secret Storage

**Store these in Google Cloud Secret Manager:**

```

supremeai/deepseek-api-key
supremeai/groq-api-key
supremeai/anthropic-api-key
supremeai/openai-api-key
supremeai/firebase-service-account-key
supremeai/jwt-signing-key
supremeai/encryption-master-key

```

**Local Development (application-local.properties):**

```properties

# NEVER commit API keys

# Use environment variables for local development

dev.deepseek.key=${DEEPSEEK_KEY}
dev.groq.key=${GROQ_KEY}
dev.anthropic.key=${ANTHROPIC_KEY}
dev.openai.key=${OPENAI_KEY}

```

### 2.3 Key Rotation

**Recommended Schedule:**

- API keys: Every 90 days

- Database credentials: Every 30 days

- JWT signing keys: Every 180 days

- Master encryption key: Every 365 days

**Rotation Process:**

1. Create new secret version in Secret Manager

2. Update application configuration
3. Deploy to staging, verify works
4. Deploy to production
5. Monitor logs for old key usage
6. After 7 days, disable old key
7. After 30 days, delete old key

### 2.4 What NOT to Log

```java
// ❌ NEVER log these
System.out.println("API Key: " + apiKey);                    // WRONG

logger.info("Token: " + token);                              // WRONG

logger.debug("Password: " + password);                       // WRONG

System.out.println("response: " + json);                     // Risky

// ✅ DO THIS instead
AuditLogger.logAPICall("deepseek-api", "request-sent");      // Safe
logger.info("API call succeeded");                           // Safe
AuditLogger.logEvent("AUTH_SUCCESS", "user@example.com");    // Safe

```

---

## 3. AUTHENTICATION & AUTHORIZATION

### 3.1 Admin Authentication

**Current Implementation:**
The system has admin-only endpoints (e.g., create agents, approve requirements).

**Required Before Production:**

```java

// Add AdminAuthFilter.java
public class AdminAuthFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, 
                        FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        
        // Extract Bearer token
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(401);
            response.getWriter().write("Unauthorized");
            return;
        }
        
        String token = authHeader.substring(7);
        
        // Validate token
        if (!isValidToken(token)) {
            response.setStatus(403);
            response.getWriter().write("Forbidden");
            return;
        }
        
        // Token is valid, continue
        chain.doFilter(request, response);
    }
}

```

### 3.2 Token-Based Authentication

**Recommended:** Firebase Authentication

```java
// Already integrated in FirebaseService
private static final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

public void verifyAdminToken(String token) {
    try {
        FirebaseToken decodedToken = firebaseAuth.verifyIdToken(token);
        String uid = decodedToken.getUid();
        // User is authenticated
    } catch (FirebaseAuthException e) {
        throw new SecurityException("Invalid token");
    }
}

```

### 3.3 Role-Based Access Control (RBAC)

```java
public enum AdminRole {
    SUPER_ADMIN("Can do anything"),
    ADMIN("Can approve requirements"),
    OPERATOR("Can view logs only"),
    READONLY("Read-only access");
    
    private final String description;
    AdminRole(String description) {
        this.description = description;
    }
}

// Check roles before operations
if (!hasRole(userId, AdminRole.ADMIN)) {
    throw new UnauthorizedException("Insufficient permissions");
}

```

---

## 4. API SECURITY

### 4.1 Rate Limiting

**Current Implementation:**
RateLimitingService.java uses token bucket algorithm.

**Configuration (application.properties):**

```properties

# Rate limiting

ratelimit.user.tokens.per_minute=100
ratelimit.project.tokens.per_hour=1000
ratelimit.admin.tokens.per_minute=500

```

**Usage:**

```java

RateLimitingService rateLimiter = new RateLimitingService(100, 1000);

if (!rateLimiter.allowUserRequest(userId)) {
    return HttpStatus.TOO_MANY_REQUESTS; // 429
}

```

### 4.2 DDoS Protection

**Network Level (GCP):**

- Enable Cloud Armor

- Set rate-based rules

- Geo-blocking if applicable

- Bot detection

**Application Level:**

```properties

# Aggressive rate limiting for suspicious behavior

ratelimit.initial_requests_per_minute=10
ratelimit.escalating_factor=1.5
ratelimit.max_memory_per_user=10mb

```

### 4.3 CORS Configuration

```java
// Enable CORS only for trusted domains
@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins("https://example.com", "https://app.example.com")
                    .allowedMethods("GET", "POST", "PUT", "DELETE")
                    .allowedHeaders("*")
                    .allowCredentials(true)
                    .maxAge(3600);
            }
        };
    }
}

```

---

## 5. INPUT VALIDATION & INJECTION PREVENTION

### 5.1 Input Validation Required

Create `src/main/java/org/example/validation/InputValidator.java`:

```java
public class InputValidator {
    
    // Requirement description: 10-5000 chars, no URLs
    public static void validateRequirementDescription(String description) {
        if (description == null || description.length() < 10) {
            throw new IllegalArgumentException("Description too short");
        }
        if (description.length() > 5000) {
            throw new IllegalArgumentException("Description too long");
        }
        if (description.contains("http://") || description.contains("https://")) {
            throw new IllegalArgumentException("URLs not allowed");
        }
    }
    
    // Email validation
    public static void validateEmail(String email) {
        String regex = "^[A-Za-z0-9+_.-]+@(.+)$";
        if (!email.matches(regex)) {
            throw new IllegalArgumentException("Invalid email");
        }
    }
    
    // Project ID: alphanumeric + hyphens only
    public static void validateProjectId(String projectId) {
        if (!projectId.matches("^[a-zA-Z0-9-]+$")) {
            throw new IllegalArgumentException("Invalid project ID");
        }
    }
    
    // No injection patterns
    public static void checkForInjectionPatterns(String input) {
        String[] injectionPatterns = {
            "<script>", "javascript:", "onclick=", "onerror=",
            "'; DROP TABLE", "1=1", "OR 1=1", "--", "/*", "*/"
        };
        
        String lowerInput = input.toLowerCase();
        for (String pattern : injectionPatterns) {
            if (lowerInput.contains(pattern.toLowerCase())) {
                throw new SecurityException("Potential injection detected");
            }
        }
    }
}

```

### 5.2 Usage in Services

```java
// In RequirementClassifier.java before processing
public RequirementSize classify(String description) {
    InputValidator.validateRequirementDescription(description);
    InputValidator.checkForInjectionPatterns(description);
    // ... continue processing
}

// In ApprovalManager.java before approval
public void approveRequirement(String requirementId, String approverEmail) {
    InputValidator.validateEmail(approverEmail);
    // ... continue
}

```

---

## 6. AUDIT LOGGING

### 6.1 What to Audit

AuditLogger.java currently logs:

- Requirement classification decisions

- Approval/rejection events

- Agent assignments

- API calls to external services

- Configuration changes

- Authentication attempts

- Rate limit violations

- Security events

### 6.2 Audit Log Format

Each entry contains:

```json
{
    "timestamp": "2026-03-26T15:30:45.123Z",
    "event_type": "APPROVAL_GRANTED",
    "user_id": "user@example.com",
    "resource_id": "req-12345",
    "action": "APPROVED",
    "outcome": "SUCCESS",
    "ip_address": "192.168.*.* [masked]",
    "correlation_id": "trace-id-12345",
    "details": {
        "requirement_id": "req-12345",
        "agent_id": "agent-deepseek-1",
        "approval_time_ms": 45
    }
}

```

### 6.3 Audit Log Retention

**Configuration (logback.xml):**

```xml

<!-- Audit logs: keep for 90 days -->

<rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
    <fileNamePattern>logs/audit-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
    <maxHistory>90</maxHistory>  <!-- 90 days -->
    <maxFileSize>100MB</maxFileSize>
</rollingPolicy>

```

---

## 7. ERROR HANDLING SECURITY

### 7.1 What to Reveal

**✅ Safe to show users:**

```

"An error occurred while processing your request. Please try again later."
"Service temporarily unavailable. Please retry in 5 minutes."
"Too many requests. Please slow down."

```

**❌ Never show users:**

```

"NullPointerException at line 45"
"Database connection timeout"
"API key expired"
"Stack trace with code paths"

```

### 7.2 Implementation in APIErrorHandler

```java
public String executeWithResilience(Supplier<String> operation, String operationName) {
    try {
        return circuitBreaker.executeSupplier(operation);
    } catch (TransientException e) {
        logger.warn("Transient error: {}", operationName); // Safe log
        throw new APIException("Service temporarily unavailable"); // Safe message
    } catch (PermanentException e) {
        logger.error("Permanent error: {}", operationName); // Safe log
        throw new APIException("Request invalid"); // Safe message
    } catch (Exception e) {
        logger.error("Unexpected error: {}", operationName); // Safe log
        throw new APIException("Internal server error"); // Safe message
    }
}

```

---

## 8. ENCRYPTION

### 8.1 Data at Rest

**Firebase Firestore:**

- Automatically encrypted by Google (encryption at rest)

- Use Secret Manager for master keys

- Consider field-level encryption for sensitive data

**Recommended:** Encrypt sensitive fields using Tink

```java
// For truly sensitive data (passwords, etc.)
String encrypted = encryptionService.encrypt(sensitiveData);
firebaseService.saveEncrypted(requirementId, encrypted);
String decrypted = encryptionService.decrypt(encrypted);

```

### 8.2 Data in Transit

**All Communication:**

- Use HTTPS/TLS 1.3

- Pin certificates for external APIs

- Verify SSL certificates

```java
// In OkHttpClient configuration
OkHttpClient client = new OkHttpClient.Builder()
    .sslSocketFactory(createSSLSocketFactory())
    .hostnameVerifier(OkHostnameVerifier.INSTANCE)
    .build();

```

---

## 9. DEPENDENCY SECURITY

### 9.1 Keep Dependencies Updated

**Current secure versions (from build.gradle.kts):**

```

Firebase Admin: 9.2.0+  ✅

OkHttp: 4.12.0+        ✅

Jackson: 2.15.2+       ✅

Logback: 1.4.7+        ✅

Resilience4j: 2.1.0+   ✅

```

**Check for updates monthly:**

```bash

./gradlew dependencyUpdates

```

### 9.2 Remove Unused Dependencies

```bash
./gradlew dependencies | grep "^.*unresolvedDependencies"

```

### 9.3 CVE Scanning

Use OWASP Dependency Check:

```bash
./gradlew dependencyCheckAggregate

```

---

## 10. COMPLIANCE & REGULATIONS

### 10.1 GDPR Compliance

If processing EU user data:

- [ ] Data retention policy documented (90-day audit log)

- [ ] User consent for tracking

- [ ] Right to be forgotten implemented

- [ ] Data breach notification process

- [ ] Privacy policy updated

### 10.2 SOC 2 Compliance

For enterprise customers:

- [ ] Access control (role-based)

- [ ] Audit logging (implemented)

- [ ] Encryption (in transit + at rest)

- [ ] Change management (documented)

- [ ] Risk assessment (quarterly)

### 10.3 Data Residency

If required:

```properties

# GCP Firestore region

firebase.database.region=us-central1  # or required region

firebase.storage.region=US

```

---

## 11. NETWORK SECURITY

### 11.1 Firewall Rules

**Inbound:**

- Allow: HTTPS (443)

- Allow: HTTP (80) - redirect to HTTPS

- Deny: All other ports

**Outbound:**

- Allow: API provider IPs (whitelist)

- Allow: GCP services (Secret Manager, etc.)

- Deny: All other destinations

### 11.2 VPC Configuration

```properties

# Cloud Run via VPC connector (if applicable)

cloud.run.vpc.connector=supremeai-connector

```

---

## 12. MONITORING & ALERTING

### 12.1 Security Metrics

Monitor these metrics:

```

- authentication_failures_per_minute

- rate_limit_violations_per_hour

- api_calls_with_errors

- circuit_breaker_state_changes

- unauthorized_access_attempts

- database_transaction_failures

```

### 12.2 Security Alerts

Set up alerts for:

```

IF authentication_failures > 5 in 1 minute → ALERT
IF rate_limit_violations > 10 in 1 hour → ALERT
IF circuit_breaker_open for > 5 minutes → ALERT
IF unauthorized_access_attempts > 0 → ALERT (immediate escalation)

```

---

## 13. INCIDENT RESPONSE

### 13.1 Security Incident Checklist

When an incident occurs:

1. [ ] Immediately stop access (revoke tokens, block IPs)
2. [ ] Isolate affected systems
3. [ ] Preserve evidence and logs
4. [ ] Notify security team
5. [ ] Begin investigation
6. [ ] Patch vulnerabilities
7. [ ] Deploy fix to staging
8. [ ] Test thoroughly
9. [ ] Deploy to production
10. [ ] Monitor closely
11. [ ] Post-mortem and lessons learned

### 13.2 Breach Notification

```properties

# Contacts for security incidents

security.contact.email=security@example.com
security.contact.phone=+1-xxx-xxx-xxxx
incident.escalation.time.minutes=15

```

---

## 14. SECURITY CHECKLIST

- [x] API keys in Secret Manager

- [x] Structured logging (no secrets)

- [x] Audit logging enabled

- [x] Rate limiting enabled

- [ ] Input validation implemented

- [ ] Admin authentication required

- [ ] CORS configured

- [ ] TLS for all connections

- [ ] Token expiration configured

- [ ] HTTPS enforced

- [ ] Dependencies updated

- [ ] CVE scanning active

- [ ] Monitoring configured

- [ ] Incident response plan documented

---

## 15. SECURITY RESOURCES

### Documentation

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)

- [Google Cloud Security Best Practices](https://cloud.google.com/security/best-practices)

- [Firebase Security Rules](https://firebase.google.com/docs/firestore/security/start)

### Tools

- [OWASP Dependency Check](https://owasp.org/www-project-dependency-check/)

- [Snyk](https://snyk.io/) - Vulnerability scanning

- [Twistlock](https://www.paloaltonetworks.com/) - Container security

### Training

- OWASP Secure Coding

- Google Cloud Security Fundamentals

- Firebase Security course

---

**Last Updated:** March 26, 2026  
**Status:** Phase 1 Security Implementation Complete

**Next Steps:**

1. Implement InputValidator.java

2. Add AdminAuthFilter
3. Deploy to staging
4. Run security audit
5. Get compliance review
