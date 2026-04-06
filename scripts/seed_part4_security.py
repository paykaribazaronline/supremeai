#!/usr/bin/env python3
"""
Part 4 — Security
Seeds SupremeAI Firebase with deep knowledge about:
  • OWASP Top 10 (2021) — vulnerabilities and mitigations
  • Authentication (JWT, OAuth2, OpenID Connect, session management)
  • Authorisation (RBAC, ABAC, Spring Security)
  • Encryption (symmetric, asymmetric, hashing, TLS)
  • Secrets management (GCP Secret Manager, Vault, environment variables)
  • API security (rate limiting, CORS, CSP, input validation)
  • Secure coding practices (SQL injection, XSS, CSRF prevention)

Collections written:
  • system_learning   (SystemLearning model records)
  • security_knowledge (rich topic documents)

Run:
  pip install firebase-admin
  python seed_part4_security.py [--dry-run]
"""

import sys
import os
sys.path.insert(0, os.path.dirname(__file__))

from seed_lib import _learning, run_part

# ============================================================================
# SYSTEM_LEARNING records
# ============================================================================

SYSTEM_LEARNINGS = {

    "security_sql_injection": _learning(
        type_="ERROR",
        category="SECURITY",
        content=(
            "SQL Injection: attacker inserts SQL code into a query via unsanitised input. "
            "Example: name = \"'; DROP TABLE users; --\" in a string-concatenated query. "
            "Impact: full database read/write/delete, authentication bypass, data exfiltration. "
            "OWASP A03:2021. "
            "Cause: building SQL queries by string concatenation of user input."
        ),
        solutions=[
            "Use parameterised queries / PreparedStatement — NEVER string-concatenate user input into SQL",
            "JPA @Query with named parameters: 'WHERE u.email = :email' — never string concat",
            "Spring Data JPA derived queries (findByEmail) are always parameterised",
            "Apply input validation: reject inputs that don't match expected format/length",
            "Use ORM (JPA/Hibernate) which escapes values by default",
            "Principle of least privilege: DB user should only have permissions it needs",
        ],
        severity="CRITICAL",
        confidence=0.99,
        error_count=0,
        resolved=True,
        resolution="Use parameterised queries exclusively — no string concatenation of user input ever",
        context={
            "owasp": "OWASP A03:2021 — Injection",
            "detection": "SAST tools: SonarQube, CodeQL; DAST: OWASP ZAP, Burp Suite",
        },
    ),

    "security_xss": _learning(
        type_="ERROR",
        category="SECURITY",
        content=(
            "Cross-Site Scripting (XSS): attacker injects malicious JavaScript into web pages "
            "viewed by other users. Types: "
            "Stored XSS: malicious script saved to DB, served to all users. "
            "Reflected XSS: script in URL parameter reflected in response. "
            "DOM-based XSS: script injected via client-side JavaScript. "
            "Impact: session hijacking, credential theft, defacement, malware distribution. "
            "OWASP A03:2021."
        ),
        solutions=[
            "React/Angular auto-escape output — never use dangerouslySetInnerHTML without sanitisation",
            "Set Content-Security-Policy (CSP) header to restrict script sources",
            "Use DOMPurify to sanitise any HTML that MUST be rendered (rich text editors)",
            "Set HttpOnly + Secure + SameSite=Strict on session cookies",
            "Encode output when inserting into HTML: use OWASP Java Encoder library",
            "Validate and sanitise all user input on both client and server side",
        ],
        severity="CRITICAL",
        confidence=0.98,
        error_count=0,
        resolved=True,
        resolution="Auto-escape output (React), set CSP header, HttpOnly cookies",
        context={"owasp": "OWASP A03:2021 — Injection (XSS is a type of injection)"},
    ),

    "security_jwt_implementation": _learning(
        type_="PATTERN",
        category="SECURITY",
        content=(
            "JWT (JSON Web Token) implementation best practices: "
            "Structure: header.payload.signature — base64url encoded, signature prevents tampering. "
            "Algorithm: HS256 (symmetric, shared secret) or RS256 (asymmetric, public/private key). "
            "Secret key: minimum 256 bits (32 bytes) for HS256; store in environment variable, never in code. "
            "Access token TTL: 15 minutes. Refresh token TTL: 7 days, stored HttpOnly cookie. "
            "Validate: algorithm, signature, expiry (exp), issuer (iss), audience (aud). "
            "Never store sensitive data in payload — it is base64-encoded, not encrypted."
        ),
        solutions=[
            "Use jjwt (Java JWT) library: Jwts.builder().signWith(key, HS256).compact()",
            "Store JWT secret in GCP Secret Manager or Kubernetes secret, not in application.properties",
            "Implement refresh token rotation: issue new refresh token on each use; revoke old one",
            "Add token revocation via a Redis blocklist for logout before expiry",
            "Use RS256 in microservices — services can validate with public key without sharing secret",
        ],
        severity="CRITICAL",
        confidence=0.97,
        times_applied=112,
        context={
            "spring_security": "Configure with SecurityFilterChain + JwtAuthenticationFilter",
            "library": "io.jsonwebtoken:jjwt-api:0.12.x",
        },
    ),

    "security_oauth2_spring": _learning(
        type_="PATTERN",
        category="SECURITY",
        content=(
            "OAuth2 + OpenID Connect (OIDC) flow in Spring Boot: "
            "Authorization Code Flow: "
            "(1) User clicks 'Login with Google'. "
            "(2) App redirects to Google with client_id, redirect_uri, scope. "
            "(3) User authenticates at Google, consents. "
            "(4) Google redirects to callback with code. "
            "(5) App exchanges code for access_token + id_token at token endpoint. "
            "(6) OIDC id_token contains user identity (sub, email, name). "
            "Spring Boot: add spring-boot-starter-oauth2-client, configure in application.yml."
        ),
        solutions=[
            "Add spring-boot-starter-oauth2-client dependency",
            "Configure: spring.security.oauth2.client.registration.google.client-id/secret",
            "Access user details: @AuthenticationPrincipal OidcUser user",
            "Use PKCE (Proof Key for Code Exchange) for public clients (mobile, SPA)",
            "Never implement custom auth flow when OAuth2/OIDC is available",
        ],
        severity="HIGH",
        confidence=0.95,
        times_applied=67,
        context={
            "providers": ["Google", "GitHub", "Microsoft (Azure AD)", "Keycloak", "Auth0", "Firebase Auth"],
            "spring_security": "spring-security-oauth2-client + spring-security-oauth2-jose",
        },
    ),

    "security_spring_security_config": _learning(
        type_="PATTERN",
        category="SECURITY",
        content=(
            "Spring Security 6 (Spring Boot 3) configuration: "
            "SecurityFilterChain @Bean replaces WebSecurityConfigurerAdapter (deprecated). "
            "Configuration: "
            "http.authorizeHttpRequests(auth -> auth "
            "  .requestMatchers('/api/public/**').permitAll() "
            "  .requestMatchers('/api/admin/**').hasRole('ADMIN') "
            "  .anyRequest().authenticated()); "
            "http.sessionManagement(s -> s.sessionCreationPolicy(STATELESS)); // for JWT APIs "
            "http.csrf(csrf -> csrf.disable()); // for REST APIs using JWT "
            "http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);"
        ),
        solutions=[
            "Use method security: @PreAuthorize('hasRole(ADMIN)') on service methods for fine-grained control",
            "Enable method security: @EnableMethodSecurity on @Configuration class",
            "Use SecurityContextHolder.getContext().getAuthentication() to get current user in any bean",
            "Configure password encoding: @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }",
            "Test security with @WithMockUser in Spring Security test slice",
        ],
        severity="HIGH",
        confidence=0.96,
        times_applied=134,
        context={
            "spring_boot_3_change": "WebSecurityConfigurerAdapter removed — use @Bean SecurityFilterChain",
            "csrf_note": "CSRF protection is mandatory for session-based apps; can disable for JWT stateless APIs",
        },
    ),

    "security_secrets_management": _learning(
        type_="PATTERN",
        category="SECURITY",
        content=(
            "Secrets management — never store secrets in code or source control: "
            "Local dev: .env file (add to .gitignore) + dotenv library. "
            "CI/CD: GitHub Actions Secrets (encrypted at rest, masked in logs). "
            "Cloud Run: GCP Secret Manager — secrets referenced as environment variables. "
            "Kubernetes: Kubernetes Secrets (base64-encoded) + External Secrets Operator for vault integration. "
            "HashiCorp Vault: enterprise-grade; dynamic secrets; audit log; automatic rotation."
        ),
        solutions=[
            "Add FIREBASE_CREDENTIALS_FILE, JWT_SECRET, DB_PASSWORD to GCP Secret Manager",
            "In Cloud Run: Settings → Variables & Secrets → reference secret versions",
            "Use spring-cloud-gcp-starter-secretmanager to inject secrets as @Value",
            "Rotate secrets regularly — at least quarterly for service account keys",
            "Audit secret access: GCP Cloud Audit Logs logs every Secret Manager access",
            "Never log secrets — review all log statements for potential secret exposure",
        ],
        severity="CRITICAL",
        confidence=0.98,
        times_applied=156,
        context={
            "gcp_command": "gcloud secrets create MY_SECRET --data-file=secret.txt",
            "spring_config": "spring.cloud.gcp.secretmanager.project-id=supremeai-a",
        },
    ),

    "security_cors_configuration": _learning(
        type_="ERROR",
        category="SECURITY",
        content=(
            "CORS (Cross-Origin Resource Sharing) misconfiguration is one of the most common "
            "API security issues. "
            "Access-Control-Allow-Origin: * allows any website to make cross-origin requests "
            "including with credentials if combined with allowCredentials=true — this is a "
            "security vulnerability. "
            "Correct config: whitelist specific origins, never use wildcard with credentials."
        ),
        solutions=[
            "In Spring Boot: CorsConfiguration.setAllowedOrigins(List.of('https://yourapp.com'))",
            "Never use setAllowedOrigins(['*']) with setAllowCredentials(true) — security hole",
            "For multiple envs: configure origins from environment variable (CORS_ALLOWED_ORIGINS)",
            "Use @CrossOrigin(origins='${app.cors.allowed-origins}') on controllers",
            "Set allowed methods, headers, and max-age explicitly",
        ],
        severity="HIGH",
        confidence=0.97,
        error_count=28,
        resolved=True,
        resolution="Whitelist specific origins via WebMvcConfigurer.addCorsMappings; never wildcard with credentials",
        context={
            "spring_boot": "Configure in WebMvcConfigurer bean or @CrossOrigin annotation",
            "preflight": "Browser sends OPTIONS preflight — Spring handles automatically with CORS config",
        },
    ),

    "security_input_validation": _learning(
        type_="PATTERN",
        category="SECURITY",
        content=(
            "Input validation is the first line of defence against injection attacks. "
            "Spring Boot: use Bean Validation (jakarta.validation) annotations on DTOs: "
            "@NotNull, @NotBlank, @Size(min=1, max=255), @Email, @Pattern(regexp=...). "
            "Enable with @Valid on @RequestBody parameter in controller. "
            "Server-side validation is mandatory even when client-side validation exists. "
            "Validate: type, length, format, range, allowed values. "
            "Reject early: return 400 Bad Request with field-level error messages."
        ),
        solutions=[
            "Add @Valid to all @RequestBody parameters in @RestController methods",
            "@NotBlank + @Size(max=255) on all String fields in request DTOs",
            "@Pattern(regexp) for fields with specific format requirements (phone, zip code)",
            "Use @RestControllerAdvice to handle MethodArgumentNotValidException globally",
            "Sanitise file uploads: check MIME type, extension, scan for malware",
        ],
        severity="HIGH",
        confidence=0.97,
        times_applied=189,
        context={
            "dependency": "spring-boot-starter-validation — adds hibernate-validator",
            "error_handling": "@ExceptionHandler(MethodArgumentNotValidException.class) for field error map",
        },
    ),

    "security_password_hashing": _learning(
        type_="PATTERN",
        category="SECURITY",
        content=(
            "Password hashing — never store plain text or reversibly encrypted passwords. "
            "Algorithms (best to worst): "
            "Argon2id: memory-hard, winner of Password Hashing Competition 2015 — best choice today. "
            "bcrypt: work factor parameter; strength=12 is good balance of security/speed. "
            "scrypt: memory-hard; good but more complex configuration. "
            "PBKDF2-HMAC-SHA256: NIST-approved; weaker than Argon2/bcrypt against GPU attacks. "
            "MD5, SHA1, SHA256 (without salt and iterations): NEVER use for passwords — too fast."
        ),
        solutions=[
            "Spring Security: BCryptPasswordEncoder(12) is the standard choice for Spring apps",
            "For new systems: Argon2PasswordEncoder from Spring Security 5.8+",
            "Always use the built-in encoder — never roll your own hashing",
            "On login: passwordEncoder.matches(rawPassword, storedHash) — constant-time comparison",
            "Hash pepper (server-side secret) + bcrypt for extra protection against DB breach",
        ],
        severity="CRITICAL",
        confidence=0.98,
        times_applied=134,
        context={
            "spring": "PasswordEncoder bean with BCryptPasswordEncoder(strength=12) or Argon2PasswordEncoder",
            "strength_12": "BCrypt strength=12 takes ~250ms per hash — too slow for brute force, fast enough for login",
        },
    ),

    "security_rate_limiting": _learning(
        type_="PATTERN",
        category="SECURITY",
        content=(
            "Rate limiting prevents brute force attacks, API abuse, and DDoS. "
            "Algorithms: "
            "Token Bucket: N requests per window; burst allowed; refills at fixed rate. "
            "Sliding Window: count requests in rolling window — accurate, more memory. "
            "Fixed Window: count per fixed minute/hour — susceptible to boundary bursts. "
            "Implementation: Bucket4j (in-process, no external dep), Redis + Lua script (distributed), "
            "API Gateway rate limiting (Kong, AWS API Gateway), Cloud Armor."
        ),
        solutions=[
            "Add Bucket4j rate limiter to Spring Boot with RateLimitingFilter",
            "Rate limit by: IP (unauthenticated), user ID (authenticated), API key",
            "Return 429 Too Many Requests with Retry-After header",
            "Apply strict limits on: /auth/login (5/min), /api/send-email (10/day), /api/** (100/min)",
            "Use Cloud Armor or API Gateway for distributed rate limiting before traffic hits Spring Boot",
        ],
        severity="HIGH",
        confidence=0.94,
        times_applied=67,
        context={
            "library": "Bucket4j for in-process; spring-cloud-gateway-ratelimiter for API gateway",
            "redis_key": "rate_limit:{userId}:{endpoint} with INCR + EXPIRE for distributed counter",
        },
    ),

    "security_https_tls": _learning(
        type_="PATTERN",
        category="SECURITY",
        content=(
            "TLS/HTTPS configuration for production: "
            "Cloud Run: TLS is handled automatically by Google — HTTP is redirected to HTTPS. "
            "Kubernetes: use cert-manager with Let's Encrypt to auto-provision TLS certificates. "
            "Spring Boot: configure server.ssl.* for embedded Tomcat TLS (rarely needed with Cloud Run). "
            "HSTS header: Strict-Transport-Security: max-age=31536000; includeSubDomains — "
            "tells browsers to always use HTTPS. "
            "TLS 1.2 minimum; TLS 1.3 preferred — disable SSLv3, TLS 1.0, TLS 1.1."
        ),
        solutions=[
            "Use Cloud Run managed TLS — zero configuration needed for supremeai-a",
            "Set HSTS header in Spring Boot: response.setHeader('Strict-Transport-Security', 'max-age=31536000')",
            "Use HttpsRedirectFilter in Spring Security for non-Cloud-Run deployments",
            "Check TLS configuration: SSL Labs server test (ssllabs.com/ssltest)",
            "Renew certificates 30 days before expiry; use cert-manager for automation",
        ],
        severity="CRITICAL",
        confidence=0.96,
        times_applied=89,
        context={
            "cloud_run": "Cloud Run terminates TLS at Google's edge — app receives HTTP internally",
            "spring": "server.ssl.enabled=true only when running outside Cloud Run/Kubernetes ingress",
        },
    ),

    "security_owasp_top10_summary": _learning(
        type_="PATTERN",
        category="SECURITY",
        content=(
            "OWASP Top 10 (2021) quick reference: "
            "A01 Broken Access Control: check authorisation on every request. "
            "A02 Cryptographic Failures: use TLS, bcrypt, no MD5/SHA1 for passwords. "
            "A03 Injection (SQL, XSS, LDAP): parameterised queries, output encoding. "
            "A04 Insecure Design: threat modelling, secure design patterns. "
            "A05 Security Misconfiguration: harden defaults, remove unused features. "
            "A06 Vulnerable Components: update dependencies regularly (Dependabot). "
            "A07 Auth Failures: MFA, rate limiting, secure session management. "
            "A08 Integrity Failures: verify software/data integrity (CI pipeline signing). "
            "A09 Logging Failures: log security events, never log credentials. "
            "A10 SSRF: validate and restrict URLs the server fetches."
        ),
        solutions=[
            "Run OWASP Dependency Check in CI pipeline to detect vulnerable libraries",
            "Enable GitHub Advanced Security CodeQL scanning for OWASP Top 10 detection",
            "Conduct quarterly penetration testing on public endpoints",
            "Train all developers on OWASP Top 10 annually",
            "Add security headers: CSP, X-Frame-Options, X-Content-Type-Options",
        ],
        severity="CRITICAL",
        confidence=0.96,
        times_applied=67,
        context={
            "reference": "owasp.org/www-project-top-ten/",
            "automation": "OWASP ZAP for DAST; SonarQube/CodeQL for SAST; Trivy for container scanning",
        },
    ),
}

# ============================================================================
# SECURITY_KNOWLEDGE rich topic documents
# ============================================================================

SECURITY_KNOWLEDGE_DOCS = {

    "owasp_top10_detailed": {
        "topic": "OWASP Top 10 (2021) — Detailed Mitigation Guide",
        "category": "OWASP",
        "description": "The industry-standard awareness document for web application security risks.",
        "vulnerabilities": {
            "A01_Broken_Access_Control": {
                "description": "Access control enforces policy so users cannot act outside their intended permissions",
                "examples": ["Accessing /api/users/123 when logged in as user 456", "IDOR (Insecure Direct Object Reference)", "Privilege escalation by changing role parameter"],
                "mitigations": ["Enforce access control on every request server-side", "@PreAuthorize in Spring Security", "Deny by default — whitelist what is allowed", "Log access control failures and alert on high frequency"],
                "spring_impl": "@PreAuthorize(\"#id == authentication.name or hasRole('ADMIN')\")",
            },
            "A02_Cryptographic_Failures": {
                "description": "Failures related to cryptography — often leads to data exposure",
                "examples": ["Storing passwords in plain text or MD5", "Transmitting data over HTTP", "Using weak encryption (DES, RC4)"],
                "mitigations": ["Use bcrypt/Argon2 for passwords", "TLS 1.2+ for all data in transit", "AES-256-GCM for data at rest", "Never implement custom cryptography"],
            },
            "A03_Injection": {
                "description": "User-supplied data is sent to an interpreter as part of a command",
                "examples": ["SQL injection", "XSS", "LDAP injection", "OS command injection"],
                "mitigations": ["Parameterised queries exclusively", "Input validation + output encoding", "Use safe API (JPA, prepared statements)", "SAST scanning in CI pipeline"],
            },
            "A05_Security_Misconfiguration": {
                "description": "Insecure default configurations, unnecessary features enabled",
                "examples": ["Default credentials unchanged", "Stack traces in production errors", "Unnecessary HTTP methods enabled", "Missing security headers"],
                "mitigations": ["Disable Spring Boot Actuator /actuator/env in production", "Return generic error messages; log details server-side", "Add security headers: CSP, HSTS, X-Frame-Options", "Remove unused endpoints and features"],
            },
            "A07_Auth_Failures": {
                "description": "Authentication and session management implemented incorrectly",
                "examples": ["Weak passwords allowed", "No MFA on sensitive operations", "JWT with 'none' algorithm accepted", "Session not invalidated on logout"],
                "mitigations": ["Enforce password complexity + MFA for admin accounts", "Validate JWT algorithm field — reject 'none'", "Implement token revocation on logout", "Rate limit login attempts (5/min per IP)"],
            },
        },
        "security_headers_checklist": {
            "Strict-Transport-Security": "max-age=31536000; includeSubDomains",
            "Content-Security-Policy": "default-src 'self'; script-src 'self' 'nonce-{random}'",
            "X-Frame-Options": "DENY",
            "X-Content-Type-Options": "nosniff",
            "Referrer-Policy": "strict-origin-when-cross-origin",
            "Permissions-Policy": "camera=(), microphone=(), geolocation=()",
        },
        "confidence": 0.96,
    },

    "authentication_guide": {
        "topic": "Authentication — Complete Implementation Guide",
        "category": "AUTHENTICATION",
        "description": "Authentication verifies WHO the user is. Authorisation verifies WHAT they can do.",
        "authentication_methods": {
            "Username_Password": {
                "security": "Medium — password reuse, phishing, brute force risks",
                "best_practices": ["bcrypt hashing", "rate limiting on login", "account lockout after N failures", "MFA for sensitive accounts"],
            },
            "JWT_Stateless": {
                "security": "High if implemented correctly",
                "flow": "Login → server issues JWT → client sends JWT in Authorization header → server validates",
                "best_practices": ["Short expiry (15 min)", "Refresh token rotation", "RS256 for microservices", "Revocation via Redis blocklist"],
            },
            "OAuth2_OIDC": {
                "security": "High — delegates to proven identity providers",
                "providers": ["Google", "GitHub", "Microsoft", "Firebase Auth"],
                "use_when": "User already has account with provider; social login; enterprise SSO",
            },
            "API_Key": {
                "security": "Medium — simple but no expiry/rotation by default",
                "use_when": "Server-to-server API calls; developer API access",
                "best_practices": ["Hash API keys in DB (SHA-256)", "Rate limit per key", "Scope keys to minimum permissions", "Rotate keys regularly"],
            },
            "MFA": {
                "types": ["TOTP (Google Authenticator)", "SMS OTP (weaker)", "Hardware key (FIDO2/WebAuthn)", "Push notification"],
                "required_for": ["Admin accounts", "Sensitive operations (payment, delete)", "Privilege escalation"],
            },
        },
        "session_management": {
            "stateless_JWT": "Store in memory or HttpOnly cookie; no server state",
            "stateful_session": "Session ID in HttpOnly cookie; session data in Redis",
            "cookie_attributes": {
                "HttpOnly": "Prevents JavaScript access — stops XSS session theft",
                "Secure": "Only sent over HTTPS",
                "SameSite=Strict": "Prevents CSRF — cookie not sent cross-origin",
                "Max-Age": "Explicit expiry — don't rely on session cookies that expire on browser close",
            },
        },
        "confidence": 0.96,
    },

    "authorisation_guide": {
        "topic": "Authorisation — RBAC, ABAC, Spring Security",
        "category": "AUTHORISATION",
        "description": "Authorisation controls WHAT an authenticated user can do.",
        "models": {
            "RBAC": {
                "name": "Role-Based Access Control",
                "description": "Permissions assigned to roles; users assigned to roles",
                "example": "ADMIN can delete users; USER can read their own profile; MODERATOR can edit content",
                "spring_impl": "Spring Security roles: hasRole('ADMIN'), @PreAuthorize, @RolesAllowed",
                "pros": "Simple, easy to audit, scales well",
                "cons": "Role explosion for complex permissions; not fine-grained enough",
            },
            "ABAC": {
                "name": "Attribute-Based Access Control",
                "description": "Policies based on subject, object, action, and environment attributes",
                "example": "User can edit document if user.department == document.department AND document.status != 'LOCKED'",
                "spring_impl": "Spring Security expressions: @PreAuthorize('#document.owner == authentication.name')",
                "pros": "Very fine-grained; context-aware",
                "cons": "Complex to manage; harder to audit",
            },
            "ReBAC": {
                "name": "Relationship-Based Access Control",
                "description": "Permissions based on relationships between entities (Google Zanzibar model)",
                "example": "User can view document if they are in a group that has viewer permission on the document",
                "tools": ["OpenFGA", "Permify", "Ory Keto"],
                "use_when": "Complex sharing scenarios (Google Docs, Slack workspace permissions)",
            },
        },
        "spring_security_method_security": {
            "PreAuthorize": "@PreAuthorize(\"hasRole('ADMIN') or #userId == authentication.name\")",
            "PostAuthorize": "@PostAuthorize(\"returnObject.owner == authentication.name\")",
            "PreFilter": "@PreFilter(\"filterObject.active == true\")",
            "PostFilter": "@PostFilter(\"filterObject.owner == authentication.name\")",
            "enable": "@EnableMethodSecurity on @Configuration class",
        },
        "firebase_authorisation": {
            "security_rules": "Firestore Security Rules evaluated server-side on every request",
            "admin_check": "request.auth.token.admin == true",
            "owner_check": "request.auth.uid == resource.data.userId",
            "role_check": "get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'ADMIN'",
        },
        "confidence": 0.95,
    },

    "encryption_guide": {
        "topic": "Encryption — Symmetric, Asymmetric, and Hashing",
        "category": "CRYPTOGRAPHY",
        "description": "Cryptographic primitives and when to use each.",
        "symmetric_encryption": {
            "AES_256_GCM": {
                "description": "Authenticated encryption — provides confidentiality + integrity",
                "key_size": "256 bits (32 bytes)",
                "use_when": "Encrypting data at rest (files, DB fields, backups)",
                "java": "Cipher.getInstance('AES/GCM/NoPadding')",
            },
            "ChaCha20_Poly1305": {
                "description": "Alternative to AES; faster on devices without AES hardware acceleration",
                "use_when": "Mobile devices, IoT",
            },
        },
        "asymmetric_encryption": {
            "RSA_2048_4096": {
                "description": "Public key encrypts, private key decrypts",
                "use_when": "Key exchange, digital signatures, JWT RS256",
                "key_size": "2048 min (4096 for long-term security)",
            },
            "ECDSA_P256": {
                "description": "Elliptic curve — shorter keys, same security as RSA",
                "use_when": "JWT ES256, TLS certificates",
            },
        },
        "hashing": {
            "SHA_256_SHA_3": "General purpose integrity verification; NOT for passwords",
            "HMAC_SHA256": "Message authentication code — verify data integrity with a shared key",
            "bcrypt_Argon2": "Password hashing — slow by design; never use SHA for passwords",
        },
        "key_management": [
            "Store keys in GCP Secret Manager, AWS KMS, or HashiCorp Vault",
            "Rotate encryption keys annually; use key versioning for decryption of old data",
            "Derive encryption keys from a master key using HKDF — never reuse keys across contexts",
            "Use envelope encryption: data encrypted with DEK; DEK encrypted with KEK stored in KMS",
        ],
        "confidence": 0.94,
    },

    "api_security_guide": {
        "topic": "API Security — Complete Guide",
        "category": "API_SECURITY",
        "description": "Security practices specific to REST API design and implementation.",
        "authentication_for_apis": {
            "JWT_Bearer": "Authorization: Bearer <token> header — stateless, scalable",
            "API_Key": "X-API-Key header — for server-to-server; hash in DB",
            "OAuth2_Client_Credentials": "For machine-to-machine; client_credentials grant",
            "mTLS": "Mutual TLS — certificate-based auth for high-security service mesh",
        },
        "security_checklist": [
            "Authenticate all non-public endpoints",
            "Authorise every action — check user has permission for the specific resource",
            "Validate all input with Bean Validation annotations",
            "Return 404 Not Found for resources user cannot see (not 403 — prevents enumeration)",
            "Rate limit all endpoints (especially auth, registration, password reset)",
            "Log all authentication attempts with outcome, IP, user agent",
            "Never expose internal IDs that reveal system size (use UUIDs)",
            "Use HTTPS everywhere; set HSTS header",
        ],
        "error_response_security": {
            "do": "Return generic error messages: {error: 'Invalid credentials'}",
            "dont": "Return specific messages: 'User not found' or 'Wrong password' (enables user enumeration)",
            "status_codes": {
                "400": "Bad request — invalid input",
                "401": "Unauthenticated — missing or invalid token",
                "403": "Unauthorised — authenticated but lacks permission",
                "404": "Not found — use for resource the user cannot see (prevents enumeration)",
                "429": "Too many requests — rate limit exceeded",
            },
        },
        "spring_boot_security_config_template": (
            "@Bean SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {\n"
            "    return http\n"
            "        .authorizeHttpRequests(auth -> auth\n"
            "            .requestMatchers('/api/public/**', '/auth/**').permitAll()\n"
            "            .requestMatchers('/api/admin/**').hasRole('ADMIN')\n"
            "            .anyRequest().authenticated())\n"
            "        .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))\n"
            "        .csrf(csrf -> csrf.disable()) // stateless JWT API\n"
            "        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)\n"
            "        .build();\n"
            "}"
        ),
        "confidence": 0.96,
    },

    "secrets_management_guide": {
        "topic": "Secrets Management — Production Guide",
        "category": "SECRETS",
        "description": "How to safely store, rotate, and access application secrets.",
        "tiers_by_environment": {
            "local_dev": {
                "tool": ".env file loaded by dotenv or spring-dotenv",
                "rules": "Add .env to .gitignore; never commit; use .env.example as template",
            },
            "ci_cd": {
                "tool": "GitHub Actions Secrets / GitLab CI Variables",
                "rules": "Secrets masked in logs; stored encrypted; accessed via ${{ secrets.NAME }}",
            },
            "cloud_run_gcp": {
                "tool": "GCP Secret Manager",
                "rules": "Mount as environment variable or volume; access via Secret Manager API",
                "command": "gcloud secrets create SECRET_NAME --data-file=secret.txt",
            },
            "kubernetes": {
                "tool": "Kubernetes Secrets + External Secrets Operator",
                "rules": "Enable encryption at rest for etcd; use ESO to sync from Vault/GCP SM",
            },
            "enterprise": {
                "tool": "HashiCorp Vault",
                "features": "Dynamic secrets, automatic rotation, fine-grained policies, audit log",
            },
        },
        "secret_types_and_storage": {
            "database_passwords": "GCP Secret Manager — mount as env var DATABASE_PASSWORD",
            "jwt_secret": "GCP Secret Manager — mount as env var JWT_SECRET (min 32 chars)",
            "api_keys": "GCP Secret Manager — per service, per environment",
            "service_account_keys": "Avoid JSON key files — use Workload Identity in GCP instead",
            "firebase_credentials": "Cloud Run: attach service account; no JSON file needed",
        },
        "rotation_policy": {
            "service_account_keys": "Rotate every 90 days; use Workload Identity to eliminate keys",
            "database_passwords": "Rotate every 180 days; use Vault dynamic secrets for zero-touch",
            "jwt_secrets": "Rotate with zero-downtime: support old + new key during transition",
            "api_keys": "Rotate immediately on suspected compromise; quarterly otherwise",
        },
        "confidence": 0.97,
    },
}

# ============================================================================
# ENTRY POINT
# ============================================================================

if __name__ == "__main__":
    run_part(
        part_name="Part 4 — Security",
        collections={
            "system_learning": SYSTEM_LEARNINGS,
            "security_knowledge": SECURITY_KNOWLEDGE_DOCS,
        },
    )
