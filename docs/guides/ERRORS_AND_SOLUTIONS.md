# SupremeAI - Issues & Solutions Guide

**Document Version:** 1.0  
**Created:** 2026-05-13  
**Purpose:** Track all identified issues with actionable solutions  
**Priority Legend:** 🔴 Critical | 🟠 High | 🟡 Medium | 🟢 Low

---

## 📋 QUICK REFERENCE TABLE

| # | Issue Category | Specific Problem | Severity | Module | Est. Effort | Status |
|---|---------------|------------------|----------|--------|-------------|--------|
| 1 | Security | Hardcoded Firebase API key | 🔴 Critical | Backend | 2h | ✅ |
| 2 | Security | Weak JWT secret in .env | 🔴 Critical | Backend | 1h | ✅ |
| 3 | Security | CORS wildcard (*) | 🔴 Critical | Backend | 1h | ✅ |
| 4 | Security | CSP unsafe-inline/eval | 🔴 Critical | Backend | 2h | ✅ |
| 5 | Security | Unauthenticated WebSocket | 🔴 Critical | Backend | 3h | ✅ |
| 6 | Security | Stored XSS in chat | 🔴 Critical | Backend+Frontend | 2h | ✅ |
| 7 | Security | Firestore isAdmin() anti-pattern | 🔴 Critical | Backend | 3h | ✅ |
| 8 | Testing | 49 integration tests disabled | 🔴 Critical | Backend | 2-3d | 🔄 |
| 9 | Testing | Dashboard 0% test coverage | 🔴 Critical | Dashboard | 1w | ❌ |
| 10 | Localization | Mobile 80+ hardcoded strings | 🔴 Critical | Mobile | 3-5d | ❌ |
| 11 | Security | API keys weak encryption | 🟠 High | Backend | 1d | ✅ |
| 12 | Performance | Rate limiting in-memory | 🟠 High | Backend | 2d | ✅ |
| 13 | Security | VS Code token in memory only | 🟠 High | VS Code | 1d | ❌ |
| 14 | Security | IntelliJ hardcoded secret | 🟠 High | IntelliJ | 2h | ❌ |
| 15 | Security | Mobile plaintext token storage | 🟠 High | Mobile | 1d | ❌ |
| 16 | Security | Functions auth bypass | 🟠 High | Functions | 1d | ❌ |
| 17 | Security | Browser automation SSRF | 🟠 High | Browser Auto | 2d | ❌ |
| 18 | Security | Smart chat no auth | 🟠 High | Smart Chat | 2d | ❌ |
| 19 | Architecture | Blocking in reactive chains | 🟡 Medium | Backend | 2d | ✅ |
| 20 | Architecture | Service fragmentation (100+) | 🟡 Medium | Backend | 1w | ❌ |
| 21 | Architecture | In-memory state loss | 🟡 Medium | Backend | 3d | ❌ |
| 22 | Architecture | Two divergent 3D systems | 🟡 Medium | Dashboard | 3d | ❌ |
| 23 | Performance | Polling sprawl | 🟡 Medium | Dashboard | 2d | ❌ |
| 24 | Performance | Prop drilling in Dashboard | 🟡 Medium | Dashboard | 1d | ❌ |
| 25 | UX | Flutter incomplete navigation | 🟡 Medium | Mobile | 1d | ❌ |
| 26 | Architecture | CLI backend controller missing | 🟡 Medium | CLI | 1w | ❌ |
| 27 | Security | No admin audit logging | 🟡 Medium | Backend | 2d | ✅ |
| 28 | Code Quality | No input validation on DTOs | 🟡 Medium | Backend | 1d | ✅ |
| 29 | Code Quality | Magic strings everywhere | 🟢 Low | Backend | 2d | ❌ |
| 30 | Architecture | Config sprawl (30+ classes) | 🟢 Low | Backend | 3d | ❌ |

**Total Count:** 30 tracked issues  
**Total Estimated Effort:** ~24-30 working days

---

## 🔴 CRITICAL ISSUES (Fix Immediately)

### **SEC-01: Hardcoded Firebase API Key**

**Severity:** 🔴 Critical  
**Module:** Backend  
**File:** `src/main/java/com/supremeai/config/ConfigController.java:17`

**Problem:**
```java
@Value("${firebase.api.key:AIzaSyCib1UPogwLoAshIWm9YQJB_RR0UxC07i8}")
private String apiKey;
```
Firebase API key is hardcoded as default value. Also found in `dashboard/.env` and compiled JS.

**Impact:**
- Project enumeration by attackers
- Quota exhaustion attacks
- Phishing using legitimate project ID
- Bypass Firebase App Check if misconfigured

**Solution Hints:**

1. **Remove hardcoded fallback:**
   ```java
   // Change from:
   @Value("${firebase.api.key:AIzaSyCib1UPogwLoAshIWm9YQJB_RR0UxC07i8}")
   // To:
   @Value("${firebase.api.key}")
   private String apiKey;
   ```

2. **Rotate compromised key:**
   - Go to Google Cloud Console → APIs & Services → Credentials
   - Delete/regenerate the exposed API key
   - Create new key and store in GCP Secret Manager
   - Update all environments with new key

3. **Purge from Git history:**
   ```bash
   git filter-branch --force --index-filter \
     "git rm --cached --ignore-unmatch dashboard/.env" \
     --prune-empty --tag-name-filter cat -- --all
   git push origin --force --all
   ```

**Effort:** 2 hours  
**Owner:** Backend + DevOps

---

### **SEC-02: Weak JWT Secret in Environment**

**Severity:** 🔴 Critical  
**Module:** Backend  
**File:** `.env` (line 3), possibly `application.properties`

**Problem:**
```
JWT_SECRET=supremeai_super_secret_key_change_in_production
```
This is a known placeholder secret that would allow trivial JWT forgery.

**Impact:** Complete authentication bypass. Attackers can forge admin tokens.

**Solution Hints:**

1. **Generate strong secret:**
   ```bash
   # Generate 256-bit (32-byte) base64 key
   openssl rand -base64 32
   # Example output: "k7V8nJ2mN5pQ9sXvB3wYzA1cE4gH6iL0rT2uO5w="
   ```

2. **Store securely:**
   - Add to GCP Secret Manager: `gcloud secrets create jwt-secret --data-file=-`
   - Reference in `application.yml`:
     ```yaml
     app:
       jwt-secret: ${JWT_SECRET:}
     ```
   - Never commit `.env` with real secret

3. **Enforce validation:**
   In `JwtUtil.java`, already checking length ≥ 32. Ensure startup fails if weak:
   ```java
   @PostConstruct
   public void validateSecret() {
       if (secret.length() < 32) {
           throw new IllegalStateException("JWT_SECRET too short");
       }
   }
   ```

4. **Add pre-commit hook:**
   Create `.git/hooks/pre-commit`:
   ```bash
   #!/bin/bash
   if grep -r "supremeai_super_secret_key" .; then
     echo "ERROR: Weak JWT secret detected in commit"
     exit 1
   fi
   ```

**Effort:** 1 hour  
**Owner:** DevOps + Backend

---

### **SEC-03: Overly Permissive CORS (Wildcard)**

**Severity:** 🔴 Critical  
**Module:** Backend  
**File:** `src/main/java/com/supremeai/config/SecurityConfig.java:146-147`

**Problem:**
```java
configuration.setAllowedOriginPatterns(List.of("*"));
configuration.setAllowCredentials(true);
```
Allows any origin with credentials → defeats same-origin policy.

**Impact:** CSRF attacks, session hijacking, malicious site can make authenticated API calls.

**Solution Hints:**

1. **Replace wildcard with explicit whitelist:**
   ```java
   @Value("${cors.allowed-origins:http://localhost:5173,https://supremeai-a.web.app}")
   private String[] allowedOrigins;
   
   // In configure():
   configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
   // OR for patterns:
   configuration.setAllowedOriginPatterns(
       Arrays.stream(allowedOrigins)
             .map(origin -> origin.replace("*", ".*"))
             .collect(Collectors.toList())
   );
   ```

2. **Environment-specific configs:**
   - `application-dev.yml`: `cors.allowed-origins: http://localhost:5173,http://localhost:3000`
   - `application-prod.yml`: `cors.allowed-origins: https://supremeai-a.web.app`

3. **WebSocket CORS separate:**
   ```java
   // WebSocketConfig.java
   registry.addEndpoint("/ws")
       .setAllowedOriginPatterns(allowedOrigins) // NOT "*"
       .withSockJS();
   ```

**Effort:** 1 hour  
**Owner:** Backend

---

### **SEC-04: CSP Contains 'unsafe-inline' and 'unsafe-eval'**

**Severity:** 🔴 Critical  
**Module:** Backend + Dashboard  
**File:** `SecurityConfig.java:48-56`

**Problem:**
```java
.policyDirectives(
  "default-src 'self'; " +
  "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://...; " +
  "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com;"
)
```

**Impact:** XSS attacks bypass CSP. Inline scripts can execute malicious code.

**Solution Hints:**

1. **Remove unsafe directives:**
   ```java
   .policyDirectives(
     "default-src 'self'; " +
     "script-src 'self' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com; " +
     "style-src 'self' 'nonce-${nonce}' https://fonts.googleapis.com; " +
     "font-src 'self' https://fonts.gstatic.com; " +
     "connect-src 'self' ws://localhost:8080 wss://supremeai-a.web.app; " +
     "img-src 'self' data: blob:;"
   )
   ```

2. **Implement nonce-based CSP for scripts:**
   ```java
   @Bean
   public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
       http
           .headers(headers -> headers
               .contentSecurityPolicy(csp -> csp
                   .policyDirectives(directives -> {
                       String nonce = Base64.getEncoder().encodeToString(
                           SecureRandom.getSeed(16)
                       );
                       directives = directives.replace("${nonce}", nonce);
                   })
               )
           );
   }
   ```

3. **For development only, conditionally allow eval:**
   ```java
   @Profile("dev")
   public SecurityFilterChain devChain() {
       // Allow unsafe-eval only in dev profile
   }
   ```

**Effort:** 2 hours (may need frontend adjustments)  
**Owner:** Backend + Frontend

---

### **SEC-05: WebSocket Endpoint Lacks Authentication**

**Severity:** 🔴 Critical  
**Module:** Backend  
**Files:** `WebSocketConfig.java:21-23`, `WebSocketController.java`

**Problem:**
```java
registry.addEndpoint("/ws")
    .setAllowedOriginPatterns("*")  // No restriction
    .withSockJS();

@MessageMapping("/dashboard/subscribe")
public Map<String, Object> subscribeToDashboard() { ... } // No @PreAuthorize
```

**Impact:** Unauthorized users can subscribe to `/topic/dashboard`, `/topic/quota` exposing user emails, quotas, system metrics.

**Solution Hints:**

1. **Add JWT handshake interceptor:**
   ```java
   @Configuration
   public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
       
       @Override
       public void registerStompEndpoints(StompEndpointRegistry registry) {
           registry.addEndpoint("/ws")
               .setAllowedOriginPatterns(getAllowedOrigins())
               .withSockJS()
               .setHandshakeHandler(new JwtHandshakeHandler()); // Custom
       }
   }
   
   public class JwtHandshakeHandler extends DefaultHandshakeHandler {
       @Override
       protected Principal determineUser(ServerHttpRequest request, 
                                         WebSocketHandler wsHandler,
                                         Map<String, Object> attributes) {
           String token = extractToken(request);
           if (token != null && jwtUtil.validateToken(token)) {
               return new StompPrincipal(jwtUtil.getUsername(token));
           }
           return null; // Reject connection
       }
   }
   ```

2. **Add method-level security:**
   ```java
   @MessageMapping("/dashboard/subscribe")
   @SendToUser("/topic/dashboard")  // User-specific, not broadcast
   @PreAuthorize("hasRole('USER')")
   public Map<String, Object> subscribeToDashboard(Principal user) { ... }
   ```

3. **Validate before broadcasting sensitive data:**
   ```java
   @EventListener
   public void handleMetricsUpdate(MetricUpdateEvent event) {
       // Only broadcast aggregated metrics, not per-user data
       if (event.isUserSpecific()) {
           messagingTemplate.convertAndSendToUser(
               event.getUserId(), "/topic/quota", event.getData()
           );
       } else {
           messagingTemplate.convertAndSend("/topic/dashboard", event.getData());
       }
   }
   ```

**Effort:** 3 hours  
**Owner:** Backend

---

### **SEC-06: Stored XSS via Unsanitized Chat Messages**

**Severity:** 🔴 Critical  
**Module:** Backend + Dashboard  
**Files:** `ChatProcessingService.java:47`, chat message rendering in React

**Problem:**
```java
ChatMessage chatMsg = new ChatMessage(userId, message, isAdmin);
chatHistoryRepository.save(chatMsg); // No sanitization
```
Frontend likely renders `{message}` with `dangerouslySetInnerHTML` or unescaped.

**Impact:** Persistent XSS in admin dashboard chat. Session hijacking, phishing, arbitrary JS execution.

**Solution Hints:**

1. **Sanitize on input (backend):**
   ```java
   import org.jsoup.Jsoup;
   import org.jsoup.safety.Safelist;
   
   String sanitized = Jsoup.clean(message, Safelist.basic()
       .addTags("br", "p", "strong", "em", "code") // Allow safe formatting
       .addProtocols("a", "href", "https")
   );
   ChatMessage chatMsg = new ChatMessage(userId, sanitized, isAdmin);
   ```

2. **Encode output (frontend):**
   ```tsx
   // ✅ SAFE - React auto-escapes
   <div>{message}</div>
   
   // ❌ UNSAFE - Only use if you sanitized beforehand
   <div dangerouslySetInnerHTML={{ __html: message }} />
   ```

3. **Add CSP script-src 'self'** (already present but fix unsafe-inline first)

**Effort:** 2 hours (backend + frontend)  
**Owner:** Backend + Frontend

---

### **SEC-07: Firestore isAdmin() Reads Database Per Request**

**Severity:** 🔴 Critical (Logic flaw + performance)  
**Module:** Backend (Firestore Rules)  
**File:** `firestore.rules` (line 10-12, path not confirmed but referenced)

**Problem:**
```javascript
function isAdmin() {
  return isAuthenticated() && 
    get(/databases/$(database)/documents/users/$(request.auth.uid)).data.tier == 'ADMIN';
}
```
Every auth check queries Firestore → slow, race condition if admin revoked.

**Impact:** Stolen admin token valid for full 1-hour Firebase TTL; N+1 query performance hit.

**Solution Hints:**

1. **Use Firebase Custom Claims (recommended):**
   ```javascript
   // Admin sets claim via Firebase Admin SDK:
   admin.auth().setCustomUserClaims(uid, { admin: true });
   
   // In rules:
   function isAdmin() {
     return isAuthenticated() && request.auth.token.admin == true;
   }
   ```

2. **Backend-side admin check with cache:**
   ```java
   @Service
   public class AdminValidationService {
       @Cacheable(value = "adminStatus", key = "#userId", ttl = 300000) // 5min TTL
       public boolean isAdmin(String userId) {
           return userRepository.findByUserId(userId)
               .map(user -> "ADMIN".equals(user.getTier()))
               .orElse(false);
       }
   }
   ```

3. **Immediate fix:** Add caching to isAdmin() to reduce Firestore reads:
   ```javascript
   // Use a caching layer like Redis or in-memory with TTL
   ```

**Effort:** 3 hours  
**Owner:** Backend + DevOps (Firebase setup)

---

### **SEC-08: Integration Tests Disabled (49 Tests)**

**Severity:** 🔴 Critical (violates 10% coverage requirement indirectly)  
**Module:** Backend  
**Files:** Multiple `*IntegrationTest.java` marked `@Disabled`

**Problem:** Integration tests for auth, database, app lifecycle are permanently skipped because Firestore/Redis emulators not available in CI.

**Impact:** Critical workflows untested; production deployments risky; no confidence in infrastructure changes.

**Solution Hints:**

1. **Add emulators to GitHub Actions:**
   ```yaml
   # .github/workflows/supreme_unified.yml
   jobs:
     backend:
       services:
         redis:
           image: redis:7-alpine
           ports: ["6379:6379"]
         firestore:
           image: firebase/firestore-emulator
           ports: ["8080:8080"]
           env:
             FIRESTORE_EMULATOR_HOST: localhost:8080
   ```

2. **Use Testcontainers approach:**
   ```java
   @Testcontainers
   class IntegrationTest {
       @Container
       static RedisContainer redis = new RedisContainer("redis:7-alpine");
       
       @DynamicPropertySource
       static void configure(DynamicPropertyRegistry registry) {
           registry.add("spring.data.redis.host", redis::getHost);
       }
   }
   ```

3. **Gradually enable tests:**
   - Remove `@Disabled` from one integration test
   - Fix emulator setup issues
   - Rinse repeat until all 49 enabled

**Effort:** 2-3 days  
**Owner:** Backend + DevOps

---

### **DASH-09: Dashboard 0% Test Coverage**

**Severity:** 🔴 Critical  
**Module:** Dashboard (React)  
**Location:** `dashboard/src/` (109 files, 26,611 LOC)

**Problem:** No testing framework installed. Zero unit, component, or E2E tests for complex UI with 3D visualization, real-time WebSocket, auth flows.

**Impact:** High regression risk, UI bugs slip to production, 3D visualization fragile.

**Solution Hints:**

1. **Add Vitest + React Testing Library:**
   ```bash
   cd dashboard
   npm install -D vitest @testing-library/react @testing-library/jest-dom @testing-library/user-event jsdom
   ```

2. **Create `vitest.config.ts`:**
   ```typescript
   import { defineConfig } from 'vitest/config';
   import react from '@vitejs/plugin-react';
   
   export default defineConfig({
     plugins: [react()],
     test: {
       environment: 'jsdom',
       setupFiles: ['./src/test/setup.ts'],
       globals: true,
     },
   });
   ```

3. **Write first tests (priority):**
   - `LoginPage.test.tsx` - auth flow, error states
   - `ModernAdminDashboard.test.tsx` - role gating, child components render
   - `ChatWithAI.test.tsx` - message input, streaming state
   - `MainVisualizer.test.tsx` - 3D component mounts without crashing
   - `AuthWrapper.test.tsx` - token refresh logic

4. **Add test script to package.json:**
   ```json
   "scripts": {
     "test": "vitest",
     "test:ui": "vitest --ui",
     "test:coverage": "vitest --coverage"
   }
   ```

5. **CI integration:**
   ```yaml
   # .github/workflows/frontend.yml
   - name: Run tests
     run: |
       cd dashboard
       npm ci
       npm run test -- --run --coverage
   ```

**Effort:** 3-4 days for initial setup + 50 tests; ongoing 1-2 days/week  
**Owner:** Frontend Team

---

### **MOB-10: Mobile App 80+ Hardcoded Strings**

**Severity:** 🔴 Critical  
**Module:** Mobile (Flutter)  
**Files:** `alerts_screen.dart`, `settings_screen.dart`, `learning_screen.dart`, `projects_list_screen.dart`, `ai_providers_screen.dart`, `notifications_screen.dart`, `resilience_screen.dart`, `home_screen.dart`

**Problem:**
```dart
// Example - 8+ hardcoded strings per screen:
AppBar(title: Text('System Alerts')), // Should be 'alerts.title'.tr()
ListTile(title: Text('API Rate Limit Hit')),
Text('OpenAI API quota exceeded...'),
```

**Impact:** Bengali users see English UI in most screens; violates localization requirement.

**Solution Hints:**

1. **Extraction workflow (per screen):**
   - Step 1: Identify all `Text('...')` widgets in screen
   - Step 2: Add keys to `supremeai/assets/i18n/en.json`:
     ```json
     {
       "alerts": {
         "title": "System Alerts",
         "rate_limit": "API Rate Limit Hit",
         "quota_exceeded": "OpenAI API quota exceeded..."
       }
     }
     ```
   - Step 3: Translate to Bengali in `bn.json` (use existing translation files as reference)
   - Step 4: Replace:
     ```dart
     Text('System Alerts') → Text('alerts.title'.tr())
     Text('API Rate Limit Hit') → Text('alerts.rate_limit'.tr())
     ```

2. **Use IntelliJ/VS Code multi-cursor to replace efficiently**

3. **Add lint rule to prevent future hardcoding:**
   ```yaml
   # analysis_options.yaml
   linter:
     rules:
       - prefer_relative_imports_for_schemes
       - custom_lint:prefer_i18n  # Write custom lint that flags Text(' literal ')
   ```

4. **Fix existing LocalizationService to fallback to English:**
   ```dart
   class LocalizationService {
     Map<String, dynamic>? _localizedStrings;
     Map<String, dynamic>? _englishStrings;
     
     Future<void> load(Locale locale) async {
       _localizedStrings = await _loadFromJson(locale);
       if (locale.languageCode != 'en') {
         _englishStrings = await _loadFromJson(Locale('en'));
       }
     }
     
     String translate(String key) {
       if (_localizedStrings?[key] != null) return _localizedStrings![key];
       return _englishStrings?[key] ?? key; // Fallback to English
     }
   }
   ```

**Effort:** 3-5 days (all 8 screens)  
**Owner:** Mobile Team

---

## 🟠 HIGH PRIORITY ISSUES

### **SEC-11: API Keys Stored with Weak/Undefined Encryption**

**Severity:** 🟠 High  
**Module:** Backend  
**File:** `EncryptionService.java:46-49`

**Problem:**
```java
if (base64Key == null || base64Key.isBlank()) {
    log.warn("API_ENCRYPTION_KEY not set! Using temporary in-memory key.");
    secretKey = generateTempKey(); // Lost on restart or decryptable by attacker
}
```

**Impact:** If `API_ENCRYPTION_KEY` not set (common in dev), user-provided API keys become unrecoverable after restart OR attacker with code execution can decrypt all keys.

**Solution Hints:**

1. **Enforce API_ENCRYPTION_KEY in production:**
   ```java
   @Configuration
   @ConditionalOnProperty(name = "spring.profiles.active", havingValue = "prod")
   public class EncryptionConfig {
       
       @Bean
       @ConfigurationProperties(prefix = "app.encryption")
       public EncryptionConfigProperties encryptionProps() {
           EncryptionConfigProperties props = new EncryptionConfigProperties();
           if (props.getKey() == null || props.getKey().length() < 32) {
               throw new IllegalStateException(
                   "API_ENCRYPTION_KEY must be set (32+ chars) in production"
               );
           }
           return props;
       }
   }
   ```

2. **Store key in GCP Secret Manager:**
   ```bash
   echo -n "your-32-byte-base64-key" | gcloud secrets create api-encryption-key --data-file=-
   ```

   Reference in `application-prod.yml`:
   ```yaml
   app:
     encryption:
       key: ${API_ENCRYPTION_KEY:}
   ```

3. **Rotate all existing user API keys after fix:**
   - Notify users to re-enter their OpenAI/Gemini keys
   - Bulk-delete old encrypted keys from Firestore

**Effort:** 1 day (includes rotation communication)  
**Owner:** Backend + DevOps

---

### **SEC-12: Rate Limiting Uses In-Memory Map (Not Distributed)**

**Severity:** 🟠 High  
**Module:** Backend  
**File:** `RateLimiterService.java:19`

**Problem:**
```java
private final ConcurrentHashMap<String, Integer> requestCounts = new ConcurrentHashMap<>();
```
In-memory rate limiting ineffective in K8s multi-pod deployment.

**Impact:** Rate limits bypassed across pods; DoS protection ineffective; inconsistent rate experiences.

**Solution Hints:**

1. **Integrate Bucket4j Redis extension:**
   ```xml
   <dependency>
       <groupId>com.bucket4j</groupId>
       <artifactId>bucket4j-redis-extension</artifactId>
       <version>8.10.0</version>
   </dependency>
   ```

2. **Rewrite RateLimiterService:**
   ```java
   @Service
   public class DistributedRateLimiter {
       
       @Autowired
       private RedisTemplate<String, String> redisTemplate;
       
       public boolean allowRequest(String key, int limit, int durationSeconds) {
           String redisKey = "rate_limit:" + key;
           Long count = redisTemplate.opsForValue().increment(redisKey);
           
           if (count == 1) {
               redisTemplate.expire(redisKey, durationSeconds, TimeUnit.SECONDS);
           }
           
           return count <= limit;
       }
   }
   ```

3. **Alternative: Use Spring Cloud Gateway rate limiting** if moving to API Gateway pattern.

4. **Update existing rate limit filter to use new distributed service:**
   ```java
   @Component
   public class RateLimitFilter extends OncePerRequestFilter {
       @Autowired
       private DistributedRateLimiter rateLimiter;
       
       @Override
       protected void doFilterInternal(...) {
           String key = extractKey(request);
           if (!rateLimiter.allowRequest(key, 100, 60)) {
               response.sendError(429, "Rate limit exceeded");
               return;
           }
           chain.doFilter(...);
       }
   }
   ```

**Effort:** 2 days (testing + deployment)  
**Owner:** Backend + DevOps

---

### **SEC-13: VS Code Extension Token Stored In Memory Only**

**Severity:** 🟠 High  
**Module:** VS Code Extension  
**File:** `src/auth/AuthService.ts:8`

**Problem:**
```typescript
private token: string | null = null; // In-memory only, visible in dev console
```
Token lost on extension reload, accessible via VS Code Developer Tools console.

**Impact:** Token theft via dev console; no persistence across reloads; no secure storage.

**Solution Hints:**

1. **Use VS Code SecretStorage API:**
   ```typescript
   import * as vscode from 'vscode';
   
   export class AuthService {
       private readonly TOKEN_KEY = 'supremeai.authToken';
       
       async storeToken(token: string): Promise<void> {
           await vscode.extensions.getExtension('supremeai.supremeai-vscode')!
               .exports.secrets.store(this.TOKEN_KEY, token);
       }
       
       async getToken(): Promise<string | null> {
           return await vscode.extensions.getExtension('supremeai.supremeai-vscode')!
               .exports.secrets.get(this.TOKEN_KEY);
       }
       
       async clearToken(): Promise<void> {
           await vscode.extensions.getExtension('supremeai.supremeai-vscode')!
               .exports.secrets.delete(this.TOKEN_KEY);
       }
   }
   ```

2. **Add token refresh logic:**
   - Store expiry timestamp alongside token
   - If token expires in <5min, call refresh endpoint
   - Auto-revalidate before each API call

3. **Context secret storage in activation:**
   ```typescript
   export function activate(context: vscode.ExtensionContext) {
       const authService = new AuthService(context.secrets);
       context.subscriptions.push(disposable);
   }
   ```

**Effort:** 1 day  
**Owner:** VS Code Extension Team

---

### **SEC-14: IntelliJ Plugin Hardcoded API Secret**

**Severity:** 🟠 High  
**Module:** IntelliJ Plugin  
**File:** `src/main/kotlin/com/supremeai/SupremeAILearningClient.kt`

**Problem:**
```kotlin
private val secretKey = "supreme-ai-secret-key" // Hardcoded in source
```

**Impact:** Secret key exposed in compiled bytecode; anyone decompiling plugin can extract key → access backend APIs with elevated privileges.

**Solution Hints:**

1. **Move to plugin properties:**
   ```kotlin
   // In plugin.xml:
   <application-components>
     <component>
       <implementation-class>com.supremeai.SupremeAIConfig</implementation-class>
       <applicationListeners>
         <listener class="com.supremeai.SupremeAILearningClient" />
       </applicationListeners>
     </component>
   </application-components>
   
   // SupremeAIConfig.kt:
   class SupremeAIConfig : PersistentStateComponent<SupremeAIConfig.State> {
       var secretKey: String = ""
       
       override fun getState(): State = State(secretKey)
       override fun loadState(state: State) { secretKey = state.secretKey }
   }
   
   // Usage:
   private val secretKey = project.service<SupremeAIConfig>().secretKey
   ```

2. **Or use IntelliJ CredentialStore:**
   ```kotlin
   import com.intellij.credentialStore.*
   
   fun storeSecret(key: String) {
       val credentials = Credentials("supremeai", key.toCharArray())
       CredentialStore.getInstance().set(credentials)
   }
   
   fun getSecret(): String {
       val credentials = CredentialStore.getInstance().get("supremeai")
       return credentials?.password?.toString() ?: ""
   }
   ```

3. **Falling back:** If no stored secret, prompt user to enter in settings UI.

**Effort:** 2 hours + testing  
**Owner:** IntelliJ Plugin Team

---

### **SEC-15: Mobile Token Storage Plaintext SharedPreferences**

**Severity:** 🟠 High  
**Module:** Mobile (Flutter)  
**File:** `lib/providers/auth_provider.dart:81`

**Problem:**
```dart
final prefs = await SharedPreferences.getInstance();
await prefs.setString('auth_token', _token!); // Unencrypted
```

**Impact:** Rooted/jailbroken devices can extract token → full account access until token expires (7 days).

**Solution Hints:**

1. **Replace SharedPreferences with flutter_secure_storage:**
   ```yaml
   # pubspec.yaml
   dependencies:
     flutter_secure_storage: ^9.0.0
   ```

2. **Update AuthProvider:**
   ```dart
   import 'package:flutter_secure_storage/flutter_secure_storage.dart';
   
   class AuthProvider with ChangeNotifier {
       final _storage = const FlutterSecureStorage(
         aOptions: AndroidOptions(
           encryptedSharedPreferences: true,  // Encrypted on Android
         ),
         iOptions: IOSOptions(
           accessibility: KeychainAccessibility.first_unlock,
         ),
       );
       
       Future<void> setToken(String token) async {
           await _storage.write(key: 'auth_token', value: token);
       }
       
       Future<String?> getToken() async {
           return await _storage.read(key: 'auth_token');
       }
   }
   ```

3. **Shorten token TTL on backend:** Reduce refresh token to 1 day, force silent re-auth.

**Effort:** 1 day (test on both platforms)  
**Owner:** Mobile Team

---

### **SEC-16: Firebase Functions Auth Bypass**

**Severity:** 🟠 High  
**Module:** Firebase Functions  
**File:** `functions/src/index.js` (multiple routes)

**Problem:**
```javascript
app.post('/api/admin/config', async (req, res) => {
  if (req.headers['x-secret'] === process.env.ADMIN_SECRET) {
    // Bypasses Firebase Auth entirely
    return updateConfig(req.body);
  }
  res.status(403).send('Forbidden');
});
```

**Impact:** Any attacker knowing secret (or guessing weak secret) can modify system configuration.

**Solution Hints:**

1. **Remove secret-based auth entirely; use Firebase Admin SDK:**
   ```javascript
   const admin = require('firebase-admin');
   admin.initializeApp();
   
   app.post('/api/admin/config', async (req, res) => {
     const authHeader = req.headers.authorization;
     if (!authHeader || !authHeader.startsWith('Bearer ')) {
       return res.status(401).send('Unauthorized');
     }
     
     const idToken = authHeader.split('Bearer ')[1];
     try {
       const decodedToken = await admin.auth().verifyIdToken(idToken);
       // Check custom claim:
       if (!decodedToken.admin) {
         return res.status(403).send('Admin only');
       }
       return updateConfig(req.body);
     } catch (error) {
       return res.status(401).send('Invalid token');
     }
   });
   ```

2. **If secret needed for service-to-service, move to Secret Manager:**
   ```javascript
   const { SecretManagerServiceClient } = require('@google-cloud/secret-manager');
   const client = new SecretManagerServiceClient();
   const [version] = await client.accessSecretVersion({
     name: 'projects/supremeai/secrets/admin-api-key/versions/latest',
   });
   const secret = version.payload.data.toString('utf8');
   if (req.headers['x-api-key'] !== secret) { ... }
   ```

3. **Migrate from `functions.config()` to Secret Manager** (also fixes DEPR-01):
   ```bash
   gcloud secrets create supremeai-admin-key --data-file=-
   firebase functions:config:set secret.name="projects/supremeai/secrets/supremeai-admin-key"
   ```

**Effort:** 1 day per function endpoint  
**Owner:** Backend/Functions Team

---

### **SEC-17: Browser Automation Accepts Arbitrary URLs (SSRF)**

**Severity:** 🟠 High  
**Module:** Browser Automation Tool  
**File:** `browser-automation-tool/src/scraper.js` or similar

**Problem:**
```javascript
app.post('/scrape', async (req, res) => {
  const targetUrl = req.body.url; // No validation
  await page.goto(targetUrl); // Can be http://169.254.169.254 (AWS metadata)
});
```

**Impact:** SSRF attacks probe internal network, access cloud metadata endpoints, exfiltrate data.

**Solution Hints:**

1. **Validate URL scheme & whitelist:**
   ```javascript
   const ALLOWED_DOMAINS = ['example.com', 'trusted.org']; // Configure via env
   
   function isValidUrl(urlString) {
     try {
       const url = new URL(urlString);
       if (url.protocol !== 'https:') {
         return { valid: false, reason: 'Only HTTPS allowed' };
       }
       if (!ALLOWED_DOMAINS.some(domain => url.hostname.endsWith(domain))) {
         return { valid: false, reason: 'Domain not whitelisted' };
       }
       return { valid: true };
     } catch (e) {
       return { valid: false, reason: 'Invalid URL' };
     }
   }
   
   app.post('/scrape', (req, res) => {
     const validation = isValidUrl(req.body.url);
     if (!validation.valid) {
       return res.status(400).json({ error: validation.reason });
     }
     // Proceed...
   });
   ```

2. **Block private IP ranges:**
   ```javascript
   const net = require('net');
   
   function isPrivateIP(hostname) {
     return dns.promises.resolve4(hostname).then(ips => 
       ips.some(ip => 
         ip.startsWith('10.') || ip.startsWith('192.168.') ||
         ip.startsWith('172.16.') || ip === '127.0.0.1' ||
         ip === '169.254.169.254' // AWS metadata
       )
     );
   }
   ```

3. **Run browser in isolated network namespace** (K8s pod with network policies).

**Effort:** 2 days (testing needed)  
**Owner:** Backend/Browser Automation Team

---

### **SEC-18: Smart Chat System No Authentication**

**Severity:** 🟠 High  
**Module:** Smart Chat System  
**Files:** `smart_chat_system/app.py`, `smart_chat_system/routes.py`

**Problem:** Admin panel routes accessible without auth.

**Impact:** Unauthenticated users can access admin features, send chat commands, view system status.

**Solution Hints:**

1. **Add Firebase Auth middleware:**
   ```python
   from firebase_admin import auth as firebase_auth
   from functools import wraps
   
   def require_auth(f):
       @wraps(f)
       def decorated(*args, **kwargs):
           id_token = request.headers.get('Authorization', '').split('Bearer ')[-1]
           try:
               decoded_token = firebase_auth.verify_id_token(id_token)
               request.user = decoded_token
           except Exception as e:
               return jsonify({'error': 'Unauthorized'}), 401
           return f(*args, **kwargs)
       return decorated
   
   @app.route('/api/admin/config', methods=['POST'])
   @require_auth
   def update_config():
       if not request.user.get('admin'):
           return jsonify({'error': 'Admin only'}), 403
       ...
   ```

2. **Migrate JSON file DB to Firestore:**
   ```python
   import firebase_admin
   from firebase_admin import firestore
   
   db = firestore.client()
   
   # Replace file I/O with Firestore operations
   def save_chat_message(message):
       db.collection('chat_messages').add({
           'text': message,
           'timestamp': firestore.SERVER_TIMESTAMP,
       })
   ```

3. **Add rate limiting per user:**
   ```python
   from flask_limiter import Limiter
   
   limiter = Limiter(app, key_func=lambda: request.user['uid'])
   @app.route('/chat')
   @limiter.limit("100/minute")
   def chat():
       ...
   ```

**Effort:** 2 days  
**Owner:** Smart Chat Team / Backend

---

## 🟡 MEDIUM PRIORITY ISSUES

### **ARCH-19: Blocking Calls Inside Reactive Streams**

**Severity:** 🟡 Medium  
**Module:** Backend  
**Files:** `EnhancedLearningService.java:282`, `MultiAIVotingService.java:353-354`

**Problem:**
```java
public Mono<Map> getLearningStats() {
    return repository.findAll()
        .collectList()
        .block(); // ❌ Blocking inside reactive chain!
}
```

**Impact:** Thread starvation, performance degradation, defeats Project Reactor's purpose.

**Solution Hints:**

1. **Refactor to fully reactive:**
   ```java
   public Mono<Map<String, Object>> getLearningStats() {
       return repository.findAll()
           .collectList()
           .map(list -> {
               Map<String, Object> stats = new HashMap<>();
               stats.put("total", list.size());
               // compute stats...
               return stats;
           });
   }
   ```

2. **If must block, shift to bounded elastic:**
   ```java
   public Mono<Map> getLearningStats() {
       return Mono.fromCallable(() -> {
           // Blocking code
           return repository.findAll().collectList().block();
       }).subscribeOn(Schedulers.boundedElastic());
   }
   ```

3. **Use `blockHound` to detect violations:**
   ```java
   @Test
   public void shouldNotBlock() {
       BlockHound.install();
       // Test reactive methods - will fail if blocking
   }
   ```

**Effort:** 2 days (audit all .block() calls)  
**Owner:** Backend

---

### **ARCH-20: Service Fragmentation (100+ Services)**

**Severity:** 🟡 Medium  
**Module:** Backend

**Problem:** Duplicate logic between `MultiAIConsensusService` and `MultiAIVotingService`; unclear service boundaries; Single Responsibility Principle violated.

**Impact:** Maintenance burden, inconsistent behavior, hard to debug, duplicated code.

**Solution Hints:**

1. **Consolidate AI voting services into `AIConsensusService`:**
   - Move all voting strategies (ensemble, approval, decision) into single service
   - Define clear API: `ConsensusResult getConsensus(Question, Strategy)`
   - Deprecate `MultiAIConsensusService` and `MultiAIVotingService` separately

2. **Apply Domain-Driven Design bounded contexts:**
   ```
   ┌─────────────────┬─────────────────┬────────────────┐
   │   AI Engine     │   Learning      │   Generation   │
   │ • Providers     │ • NLP           │ • AppGen       │
   │ • Voting        │ • Multimodal    │ • CodeFlow     │
   │ • Consensus     │ • Predictive    │ • Healing      │
   └─────────────────┴─────────────────┴────────────────┘
   ```
   Refactor package structure accordingly: `ai.engine`, `learning.core`, `generation.orchestration`

3. **Remove dead code:**
   - `AgentOrchestrationHub.java` is empty placeholder → delete or implement
   - Commented-out code throughout

4. **Use interface segregation:**
   ```java
   public interface VotingStrategy {
       VotingResult conductVote(Question q, List<Provider> providers);
   }
   @Service
   public class EnsembleVotingStrategy implements VotingStrategy { ... }
   @Service
   public class ApprovalVotingStrategy implements VotingStrategy { ... }
   ```

**Effort:** 1 week (refactor + test)  
**Owner:** Backend Architecture Team

---

### **ARCH-21: In-Memory State Lost on Restart**

**Severity:** 🟡 Medium  
**Module:** Backend

**Problem:**
- `MultiAIVotingService.consensusHistory` - in-memory list
- `AIProviderFactory.providerHealthCache` - in-memory map
- `GlobalKnowledgeBase.globalMemory` - lazy-loaded from Firestore but modifications not persisted automatically

**Impact:** Learning and history lost on pod restart; no continuity; poor performance after cold start.

**Solution Hints:**

1. **Persist consensus history:**
   ```java
   @Document(collectionName = "consensus_history")
   public class ConsensusVoteRecord {
       @Id String id;
       String question;
       String consensusResult;
       LocalDateTime timestamp;
       List<String> providerResponses;
   }
   
   // In MultiAIVotingService:
   @Autowired
   private ConsensusVoteRepository voteRepository;
   
   public VotingResult askConsensus(...) {
       VotingResult result = ...;
       voteRepository.save(new ConsensusVoteRecord(
           question, result, LocalDateTime.now(), responses
       ));
       // Also load recent history into memory for context
       return result;
   }
   
   @PostConstruct
   public void warmCache() {
       voteRepository.findTop100ByOrderByTimestampDesc()
           .forEach(record -> consensusHistory.add(record.toVotingResult()));
   }
   ```

2. **Persist provider health metrics:**
   ```java
   @Document
   public class ProviderHealthMetrics {
       @Id String id;
       String providerName;
       double successRate;
       double avgLatencyMs;
       LocalDateTime lastUpdated;
   }
   
   // Update after each call in AIProviderFactory
   ```

3. **Warm caches on startup:**
   ```java
   @Component
   public class CacheWarmer {
       @PostConstruct
       public void warmAllCaches() {
           // Load recent consensus into memory
           // Load top solutions into GlobalKnowledgeBase
           // Load provider health into AIProfiler
       }
   }
   ```

**Effort:** 3 days  
**Owner:** Backend

---

### **DASH-22: Two Divergent 3D Implementations**

**Severity:** 🟡 Medium  
**Module:** Dashboard  
**Files:** `MainVisualizer.tsx` (R3F), `ThreeDashboard.tsx` (imperative)

**Problem:** Both `/visualizer` and `/unified` render different Three.js implementations doing similar things.

**Impact:** Doubled maintenance, inconsistent UX, confusion over which is "main".

**Solution Hints:**

1. **Choose R3F (declarative) as canonical implementation** (more React-friendly)

2. **Migrate imperative features to R3F:**
   - Port custom WebSocket data feeding from ThreeDashboard to MainVisualizer
   - Port HUD overlay (FPS, connection status) as React component over Canvas (not 3D)
   - Port keyboard navigation (arrow keys) using `@react-three/drei`'s `KeyboardControls`

3. **Deprecate ThreeDashboard:**
   ```tsx
   // routes.tsx
   {
     path: '/unified',
     element: <DeprecatedNotice message="Use /visualizer instead. This page will be removed in v7." />
   }
   ```

4. **Merge after migration:**
   - Delete `ThreeDashboard.tsx` and `.css`
   - Update all internal links to `/visualizer`
   - Update documentation

**Effort:** 3 days (careful testing of 3D features)  
**Owner:** Frontend + 3D Specialist

---

### **DASH-23: Polling Sprawl (Multiple Intervals)**

**Severity:** 🟡 Medium  
**Module:** Dashboard

**Problem:** Components set their own intervals:
- AdminMonitoring: 5s
- SystemHealthMatrix: 10s
- ChatWithAI: 15s
- KnowledgeHub: 60s

**Impact:** Browser performance degradation, server load spikes, inconsistent data freshness.

**Solution Hints:**

1. **Create centralized polling hook:**
   ```typescript
   // hooks/usePolling.ts
   export function usePolling<T>(
     fetchFn: () => Promise<T>,
     interval: number,
     options: { enabled?: boolean; visibilityRequired?: boolean }
   ) 
   {
     const [data, setData] = useState<T>();
     const [isVisible, setIsVisible] = useState(true);
     
     useEffect(() => {
       const handleVisibility = () => {
         setIsVisible(!document.hidden);
       };
       document.addEventListener('visibilitychange', handleVisibility);
       
       const intervalId = setInterval(async () => {
         if (isVisible || !options.visibilityRequired) {
           const result = await fetchFn();
           setData(result);
         }
       }, interval);
       
       return () => {
         clearInterval(intervalId);
         document.removeEventListener('visibilitychange', handleVisibility);
       };
     }, [fetchFn, interval, isVisible]);
     
     return data;
   }
   ```

2. **Migrate components to use central hook:**
   ```tsx
   // Before:
   useEffect(() => {
     const id = setInterval(fetchSystemHealth, 5000);
     return () => clearInterval(id);
   }, []);
   
   // After:
   const health = usePolling(fetchSystemHealth, 5000, { 
     visibilityRequired: true 
   });
   ```

3. **Migrate most polling to WebSocket push:**
   - Already have STOMP configured (`useSystemWebSocket` hook)
   - Server-side: `MessagingTemplate.convertAndSend("/topic/system-health", data)`
   - Client-side: subscribe once, receive updates in real-time → eliminate polling

**Effort:** 2 days  
**Owner:** Frontend

---

### **DASH-24: Prop Drilling (darkMode, chatFont)**

**Severity:** 🟡 Medium  
**Module:** Dashboard

**Problem:** Props like `darkMode`, `chatFont` passed through 5+ component layers unnecessarily.

**Impact:** Unnecessary re-renders, cascading updates, tight coupling.

**Solution Hints:**

1. **Introduce Zustand for global UI state:**
   ```typescript
   // stores/uiStore.ts
   import { create } from 'zustand';
   
   interface UIState {
     darkMode: boolean;
     chatFont: string;
     setDarkMode: (mode: boolean) => void;
     setChatFont: (font: string) => void;
   }
   
   export const useUIStore = create<UIState>((set) => ({
     darkMode: localStorage.getItem('darkMode') === 'true',
     chatFont: localStorage.getItem('chatFont') || 'default',
     setDarkMode: (mode) => {
       localStorage.setItem('darkMode', String(mode));
       set({ darkMode: mode });
     },
     setChatFont: (font) => {
       localStorage.setItem('chatFont', font);
       set({ chatFont: font });
     },
   }));
   ```

2. **Refactor components to consume store directly:**
   ```tsx
   // Before (5 layers deep):
   <PageA darkMode={darkMode} chatFont={chatFont}>
     <PageB darkMode={darkMode} chatFont={chatFont}>
       <PageC darkMode={darkMode} chatFont={chatFont}>
         
   // After:
   const darkMode = useUIStore(s => s.darkMode);
   ```

3. **Persist to localStorage via zustand middleware:**
   ```typescript
   import { persist } from 'zustand/middleware';
   export const useUIStore = create<UIState>()(
     persist(
       (set) => ({ ... }),
       { name: 'ui-storage' }
     )
   );
   ```

**Effort:** 1 day  
**Owner:** Frontend

---

### **MOB-25: Incomplete Navigation in Flutter App**

**Severity:** 🟡 Medium  
**Module:** Mobile  
**File:** `lib/home_screen.dart`

**Problem:** Only 2 tabs (Chat, Settings). 11 other screens exist but orphaned with no navigation paths.

**Impact:** Users cannot access features like Projects, Providers, Analytics, Learning, etc.

**Solution Hints:**

1. **Implement Drawer navigation:**
   ```dart
   // home_screen.dart
   Drawer(
     child: ListView(
       children: [
         DrawerHeader(child: Text('SupremeAI')),
         ListTile(
           leading: Icon(Icons.chat),
           title: Text('Chat'.tr()),
           onTap: () => navigateTo(ChatScreen()),
         ),
         ListTile(
           leading: Icon(Icons.folder),
           title: Text('Projects'.tr()),
           onTap: () => navigateTo(ProjectsListScreen()),
         ),
         // Add all 13 screens...
       ],
     ),
   )
   ```

2. **Orimplement NavigationRail for tablets:**
   ```dart
   NavigationRail(
     destinations: [
       NavigationRailDestination(icon: Icon(Icons.chat), label: Text('Chat'.tr())),
       NavigationRailDestination(icon: Icon(Icons.folder), label: Text('Projects'.tr())),
       // ...
     ],
     selectedIndex: _selectedIndex,
     onDestinationSelected: (index) => setState(() => _selectedIndex = index),
   )
   ```

3. **Add routing with `go_router` or `auto_route`:**
   ```dart
   final GoRouter _router = GoRouter(
     routes: [
       GoRoute(path: '/', builder: (context, state) => HomeScreen()),
       GoRoute(path: '/chat', builder: (context, state) => ChatScreen()),
       GoRoute(path: '/projects', builder: (context, state) => ProjectsListScreen()),
       // ...
     ],
   );
   ```

**Effort:** 1 day  
**Owner:** Mobile Team

---

### **ARCH-26: CLI Backend Controller Missing**

**Severity:** 🟡 Medium  
**Module:** CLI + Backend

**Problem:** Python CLI expects `/api/commands/execute`, `/api/commands/list`, but Spring Boot has no `CommandController`.

**Impact:** CLI non-functional until backend endpoint implemented.

**Solution Hints:**

1. **Build CommandController in Spring Boot:**
   ```java
   @RestController
   @RequestMapping("/api/commands")
   public class CommandController {
       
       @Autowired
       private CommandExecutorService commandExecutor;
       
       @PostMapping("/execute")
       public ResponseEntity<CommandResult> executeCommand(
           @Valid @RequestBody CommandRequest request
       ) {
           CommandResult result = commandExecutor.execute(
               request.getCommand(), 
               request.getArgs()
           );
           return ResponseEntity.ok(result);
       }
       
       @GetMapping("/list")
       public ResponseEntity<List<CommandDefinition>> listCommands(
           @AuthenticationPrincipal User user
       ) {
           List<CommandDefinition> commands = 
               commandExecutor.getAvailableCommands(user.getRole());
           return ResponseEntity.ok(commands);
       }
   }
   ```

2. **Implement CommandExecutorService:**
   - Define command interface: `interface Command { String getName(); CommandResult execute(String[] args); }`
   - Implement commands: `GenerateProjectCommand`, `AnalyzeCodeCommand`, `DeployCommand`
   - Add permission checks per command

3. **Or integrate CLI with existing mechanism:**
   - Maybe CLI should call existing orchestration endpoints instead?
   - Evaluate if `CommandHub` concept maps to current agent orchestration

**Effort:** 1 week (includes command implementations)  
**Owner:** Backend + CLI Team

---

### **SEC-27: No Admin Audit Logging**

**Severity:** 🟡 Medium  
**Module:** Backend

**Problem:** `ActivityLogRepository` exists but not comprehensively used. Admin actions (config changes, user management, provider updates) not audited.

**Impact:** Cannot trace who changed what; forensics impossible after breach or configuration error.

**Solution Hints:**

1. **Create `@Audited` aspect (AOP):**
   ```java
   @Aspect
   @Component
   public class AuditLoggingAspect {
       
       @Autowired
       private ActivityLogRepository activityLogRepository;
       
       @Before("execution(* com.supremeai.controller.admin..*(..)) && @annotation(auditable)")
       public void logAdminAction(JoinPoint jp, Auditable auditable) {
           String username = SecurityContextHolder.getContext().getAuthentication().getName();
           String method = jp.getSignature().getName();
           String args = Arrays.toString(jp.getArgs());
           
           ActivityLog log = new ActivityLog();
           log.setUsername(username);
           log.setAction(method);
           log.setDetails(args);
           log.setTimestamp(Instant.now());
           log.setResource(auditable.resource());
           
           activityLogRepository.save(log);
       }
   }
   ```

2. **Add `@Auditable(resource = "system_config")` to admin controllers:**
   ```java
   @RestController
   @RequestMapping("/api/admin")
   public class AdminConfigController {
       
       @Auditable(resource = "system_config")
       @PutMapping("/config")
       public ResponseEntity<?> updateConfig(@RequestBody ConfigUpdate update) { ... }
   }
   ```

3. **Create audit log viewer in dashboard:**
   - New admin page: `/admin/audit-logs`
   - Table: timestamp, user, action, resource, IP address, user agent
   - Filter by date range, user, action type

**Effort:** 2 days (backend + UI)  
**Owner:** Backend + Frontend

---

## 🟢 LOW PRIORITY / TECHNICAL DEBT

### **CODE-28: No Input Validation on DTOs**

**Severity:** 🟡 Medium (actually HIGH - security)  
**Module:** Backend  
**Files:** All DTOs in `src/main/java/com/supremeai/dto/`

**Problem:** Controllers use `@Valid @RequestBody` but DTOs lack Bean Validation annotations. Example `ChatRequest.java` has no `@NotBlank` on `message`.

**Impact:** Null/empty strings pass validation → NPEs downstream, business logic bypass.

**Solution Hints:**

1. **Add standard validation to all DTOs:**
   ```java
   public class ChatRequest {
       @NotBlank(message = "Message is required")
       @Size(max = 5000, message = "Message too long (max 5000 chars)")
       private String message;
       
       @NotNull
       private String sessionId;
       
       @Min(1)
       @Max(25)
       private Integer maxProviders = 5;
       
       // getters/setters
   }
   ```

2. **Add `@Validated` on controller classes:**
   ```java
   @RestController
   @Validated  // Enables method-level validation
   public class ChatController {
       
       @PostMapping("/chat")
       public ResponseEntity<ChatResponse> chat(
           @Valid @RequestBody ChatRequest request
       ) { ... }
   }
   ```

3. **Add global exception handler for `ConstraintViolationException`:**
   ```java
   @ControllerAdvice
   public class GlobalExceptionHandler {
       
       @ExceptionHandler(MethodArgumentNotValidException.class)
       public ResponseEntity<ApiResponse> handleValidation(MethodArgumentNotValidException ex) {
           List<String> errors = ex.getBindingResult()
               .getFieldErrors()
               .stream()
               .map(err -> err.getField() + ": " + err.getDefaultMessage())
               .collect(Collectors.toList());
           return ResponseEntity.badRequest().body(ApiResponse.error("Validation failed", errors));
       }
   }
   ```

**Effort:** 1 day (iterate through all DTOs)  
**Owner:** Backend

---

### **CODE-29: Magic Strings & Hardcoded Provider Names**

**Severity:** 🟢 Low  
**Module:** Backend

**Problem:** Provider names as raw strings scattered throughout codebase.

**Impact:** Typos, no IDE autocomplete, inconsistent naming, refactoring difficulty.

**Solution Hints:**

1. **Use `AIProviderType` enum consistently:**
   ```java
   public enum AIProviderType {
       OPENAI, ANTHROPIC, GEMINI, GROQ, DEEPSEEK, OLLAMA,
       HF_MISTRAL, HF_LLAMA, HF_CODELLAMA, HF_PHI, HF_PHI_VISION,
       RENDER_TINYLLAMA, RENDER_PHI3, RENDER_PHI2, RENDER_QWEN,
       CODEGEEX4, // ...
       ;
       
       public String getIdentifier() {
           return name().toLowerCase();
       }
   }
   ```

2. **Replace string literals:**
   ```java
   // Before:
   provider = AIProviderFactory.getProvider("openai");
   
   // After:
   provider = AIProviderFactory.getProvider(AIProviderType.OPENAI);
   ```

3. **Centralize provider registry:**
   ```java
   @Component
   public class ProviderRegistry {
       private final Map<String, AIProvider> providers = ConcurrentHashMap<>();
       
       public void register(AIProviderType type, AIProvider provider) {
           providers.put(type.getIdentifier(), provider);
       }
       
       public AIProvider get(AIProviderType type) {
           return providers.get(type.getIdentifier());
       }
   }
   ```

**Effort:** 2 days (find-and-replace across 50+ files)  
**Owner:** Backend

---

### **CONFIG-30: Configuration Sprawl (30+ Config Classes)**

**Severity:** 🟢 Low  
**Module:** Backend

**Problem:** `com.supremeai.config` package has 30+ separate `@Configuration` classes.

**Impact:** Hard to understand system configuration; scattered `@Value` injections; duplicate concerns.

**Solution Hints:**

1. **Group related configs:**
   ```
   config/
   ├── AppConfig.java                    (main, common beans)
   ├── datasource/                       (DatabaseConfig, FirestoreConfig, RedisConfig)
   ├── security/                         (SecurityConfig, JwtConfig, AuthConfig)
   ├── ai/                               (ProviderConfig, AIFallbackConfig, VotingConfig)
   ├── resilience/                       (RateLimitConfig, CircuitBreakerConfig, BulkheadConfig)
   ├── monitoring/                       (MetricsConfig, TracingConfig, HealthConfig)
   └── infrastructure/                   (ThreadPoolConfig, GracefulShutdownConfig, JVMOptionsConfig)
   ```

2. **Use `@ConfigurationProperties`:**
   ```java
   @ConfigurationProperties(prefix = "app.ai")
   @Data
   @Component
   public class AIConfigProperties {
       private int defaultTimeout = 30;
       private int maxRetries = 3;
       private List<String> fallbackOrder = List.of("openai", "gemini");
       // getters/setters
   }
   
   // application.yml:
   app:
     ai:
       default-timeout: 30
       max-retries: 3
       fallback-order: [openai, gemini, groq]
   ```

3. **Remove `BeanConfiguration.java`** - move bean definitions to appropriate config classes.

**Effort:** 3 days (refactor + test)  
**Owner:** Backend Architecture Team

---

## ✅ LOCALIZATION FIXES (Bengali)

### **LOC-01: Dashboard Missing 12 Keys**

**Severity:** 🔴 Critical  
**Module:** Dashboard  
**File:** `dashboard/src/i18n/bn.json`

**Missing Keys:**
```json
// Add to bn.json:
{
  "desc": {
    "system_health": "AI এজেন্ট ও পারফরম্যান্স মনিটর করুন",
    "project_status": "অ্যাপ জেনারেশন স্ট্যাটাস ট্র্যাক করুন",
    "api_keys": "আপনার AI প্রোভাইডার kys ম্যানেজ করুন",
    "admin_controls": "AI সিদ্ধান্তের অনুমোদন/প্রত্যাখ্যান করুন",
    "king_mode": "AUTO / WAIT / FORCE_STOP নিয়ন্ত্রণ",
    "admin_approval": "Execution-এর আগে AI সিদ্ধান্ত রিভিউ করুন",
    "api_encryption": "আপনার kys শিপheastern এ ncryptেড আ卧",
    "rate_limiting": "অব্যবহার থেকে রক্ষা"
  },
  "dashboard": {
    "role_communication": "Communication Role",
    "role_execution": "Execution Role",
    "role_voting": "Voting Role",
    "pillar_registry": "Registry"
  },
  "settings": {
    "save": "সেভ করুন"
  }
}
```

**Effort:** 1 hour  
**Owner:** Frontend + i18n Specialist

---

### **LOC-02: Dashboard bn.json Typos**

**File:** `dashboard/src/i18n/bn.json`

**Fix:**
- Line 154: `"দৃশ্যমান শ ¬রavigation কমান"` → `"দৃশ্যমান navigation কমান"`  
  (Actually meant: "Visible navigation items" - translate properly: `"দৃশ্যমান নেভিগেশন আইটেম"`)

- Line 224: `"নিষেধitted URLs"` → `"নিষিদ্ধ URLs"`

**Effort:** 30 min  
**Owner:** Frontend

---

### **LOC-03: Dashboard Hardcoded English in OnboardingWizard**

**Module:** Dashboard  
**File:** `pages/OnboardingWizard.tsx`

**Replace:**
```tsx
// Lines 159, 162, etc.
<Button>Skip Tour</Button>  →  <Button>{t('btn.skip')}</Button>
<Button>Next</Button>       →  <Button>{t('btn.next')}</Button>
<Button>Get Started</Button>→  <Button>{t('btn.get_started')}</Button>
```

Keys already exist in `en.json`/`bn.json`.

**Effort:** 30 min  
**Owner:** Frontend

---

### **LOC-04: Dashboard Hardcoded Bengali (Ad-hoc)**

**Module:** Dashboard  
**Files:** `ModernAdminDashboard.tsx`, `AdminAnalytics.tsx`, `AdminLearning.tsx`, `AdminSimulator.tsx`, `ChatWithAI.tsx`

**Problem:** Bengali text written directly instead of using i18n:
```tsx
<h1>অ্যাক্টিভ এজেন্ট</h1>  →  <h1>{t('agent.active')}</h1>
<p>ড্যাশবোর্ড...</p>      →  <p>{t('dashboard.welcome')}</p>
```

**Solution:** Create keys in `en.json` and `bn.json` for all ad-hoc Bengali strings, migrate to `t()` calls.

**Effort:** 2 hours  
**Owner:** Frontend

---

### **LOC-05: Flutter Hardcoded Strings (80+)**

**Module:** Mobile  
**Files:** `alerts_screen.dart`, `settings_screen.dart`, `learning_screen.dart`, `projects_list_screen.dart`, `ai_providers_screen.dart`, `notifications_screen.dart`, `resilience_screen.dart`

**Solution Pattern:**
For each hardcoded `Text('...')`:
1. Add key to `supremeai/assets/i18n/en.json`:
   ```json
   {
     "alerts": {
       "title": "System Alerts",
       "rate_limit": "API Rate Limit Hit",
       "quota_exceeded": "OpenAI API quota exceeded..." }
   }
   ```
2. Translate to Bengali in `bn.json`
3. Replace: `Text('API Rate Limit Hit')` → `Text('alerts.rate_limit'.tr())`

**Effort:** 3-5 days  
**Owner:** Mobile Team

---

### **LOC-06: Flutter LocalizationService No Fallback**

**Module:** Mobile  
**File:** `lib/services/localization_service.dart`

**Problem:** Returns key if missing instead of falling back to English.

**Solution:**
```dart
class LocalizationService {
  Map<String, dynamic>? _currentLocale;
  Map<String, dynamic>? _englishLocale;
  
  Future<void> load(Locale locale) async {
    _currentLocale = await _loadJson(locale);
    if (locale.languageCode != 'en') {
      _englishLocale = await _loadJson(const Locale('en'));
    }
  }
  
  String translate(String key) {
    // 1. Try current locale
    if (_currentLocale?[key] != null) return _currentLocale![key];
    // 2. Fallback to English
    if (_englishLocale?[key] != null) return _englishLocale![key];
    // 3. Return key as last resort
    return key;
  }
}
```

**Effort:** 1 hour  
**Owner:** Mobile Team

---

### **LOC-07: Backend Runtime i18n Inactive**

**Module:** Backend

**Problem:** `messages_bn.properties` exists but not wired; controllers return hardcoded English.

**Solution Hints:**

1. **Create MessageSource bean:**
   ```java
   @Configuration
   public class I18nConfig {
       
       @Bean
       public MessageSource messageSource() {
           ReloadableResourceBundleMessageSource messageSource = 
               new ReloadableResourceBundleMessageSource();
           messageSource.setBasenames(
               "classpath:messages", 
               "classpath:messages_bn"
           );
           messageSource.setDefaultEncoding("UTF-8");
           messageSource.setFallbackToSystemLocale(false);
           return messageSource;
       }
       
       @Bean
       public LocaleResolver localeResolver() {
           AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
           resolver.setDefaultLocale(Locale.ENGLISH);
           return resolver;
       }
   }
   ```

2. **Inject and use in controllers:**
   ```java
   @RestController
   public class ChatController {
       
       @Autowired
       private MessageSource messageSource;
       
       private String getMessage(String key, Locale locale, Object... args) {
           return messageSource.getMessage(key, args, locale);
       }
       
       @PostMapping("/chat")
       public ResponseEntity<ChatResponse> chat(
           HttpServletRequest request,
           @Valid @RequestBody ChatRequest req
       ) {
           Locale locale = request.getLocale(); // From Accept-Language header
           String errorMsg = getMessage("error.chat.empty", locale);
           // ...
       }
   }
   ```

3. **User language preference integration:**
   - Read user's language from Firebase token claim or User document
   - Override `Accept-Language` with user preference

**Effort:** 2 days (migrate all controller messages)  
**Owner:** Backend

---

### **LOC-08: VS Code Extension No Runtime i18n**

**Module:** VS Code Extension  
**File:** `package.nls.bn.json` exists but unused

**Problem:** Extension uses hardcoded English strings in `showInformationMessage`, etc.

**Solution Hints:**

1. **Use VS Code's vscode.l10n API (since 1.74):**
   ```typescript
   import * as vscode from 'vscode';
   
   // In extension.ts:
   export function activate(context: vscode.ExtensionContext) {
       const localize = vscode.l10n.localize.bind(vscode.l10n);
       
       // Example usage:
       vscode.window.showInformationMessage(
           localize('message.analysisComplete', 'Code analysis complete')
       );
   }
   ```

2. **Structure translation files:**
   ```
   supremeai-vscode-extension/
   ├── package.json (declares localizations)
   ├── package.nls.json (English)
   ├── package.nls.bn.json (Bengali)
   └── src/
       └── locales/
           ├── en.json
           └── bn.json
   ```

3. **Migrate all hardcoded strings:**
   Search for all `showInformationMessage('...')`, `showErrorMessage('...')` and replace with `localize('key', 'default English')`.

4. **Register in package.json:**
   ```json
   {
     "localizations": [
       {
         "language": "en",
         "translations": "src/locales/en.json"
       },
       {
         "language": "bn",
         "translations": "src/locales/bn.json"
       }
     ]
   }
   ```

**Effort:** 2-3 days (35+ strings)  
**Owner:** VS Code Extension Team

---

## 📊 PRIORITY SCHEDULE

### **Week 1 (Critical - 5 Days)**

| Day | Focus Area | Tasks | Est. |
|-----|-----------|-------|------|
| Mon | Security P0 | Rotate Firebase key, set strong JWT secret, lock CORS | 4h |
| Tue | Security P0 | Fix CSP, implement WebSocket auth | 5h |
| Wed | Security P0 | Sanitize chat XSS, fix Firestore isAdmin | 5h |
| Thu | Testing | Enable integration tests in CI (emulators) | 7h |
| Fri | Localization | Dashboard: add 12 keys, fix typos, migrate hardcoded | 7h |

**Week 1 Total:** ~28 hours  
**Week 1 Goal:** Eliminate ALL critical security exposures; begin localization compliance

---

### **Week 2-3 (High Priority - 10 Days)**

| Day | Focus Area | Tasks |
|-----|-----------|-------|
| 1-2 | Testing | Add Vitest to Dashboard; write 50 component tests |
| 3-4 | Mobile | Replace 80+ hardcoded Flutter strings with i18n |
| 5 | Mobile | Fix Flutter localizationService fallback |
| 6 | Security | Implement distributed rate limiting (Redis) |
| 7 | Dashboard | Migrate imperative ThreeDashboard → R3F |
| 8 | Dashboard | Centralize polling (visibility-aware hook) |
| 9 | Mobile | Add Drawer navigation (expose all 13 screens) |
| 10 | Backend | Wire runtime i18n (MessageSource bean) |

**Week 2-3 Total:** ~70 hours

---

### **Week 4-6 (Medium Priority - 15 Days)**

| Week | Focus | Tasks |
|------|-------|-------|
| 4 | Architecture | Consolidate AI voting services (2→1) |
| 4 | Backend | Persist in-memory state (consensus, health metrics) |
| 5 | Security | Firebase Functions: auth, migrate from functions.config() |
| 5 | Security | Browser automation SSRF fix + auth |
| 5 | Security | Smart chat auth + Firestore migration |
| 5 | Backend | Fix blocking in reactive chains |
| 6 | Admin | Add audit logging (AOP) |
| 6 | Code Quality | Add input validation to all DTOs |
| 6 | Mobile | Flutter: implement proper navigation with go_router |

**Week 4-6 Total:** ~105 hours

---

### **Month 2-3 (Low Priority / Polish)**

- Configuration sprawl consolidation (3d)
- Magic strings → enums (2d)
- VS Code extension i18n (2d)
- IntelliJ plugin i18n verification (1d)
- Add React Query/Zustand to Dashboard (2d)
- Lazy route loading for Dashboard (1d)
- Add React.memo optimizations (1d)
- WAF deployment (Cloudflare) (2d)
- MFA enforcement (1d)
- Penetration testing planning (1d)

**Total Long-term:** ~13 days

---

## 🛠️ QUICK FIX SCRIPTS

### **Script 1: Find Hardcoded Secrets**
```bash
#!/bin/bash
# find_secrets.sh - Scan for hardcoded credentials
echo "Scanning for hardcoded secrets..."

# Firebase API keys (AIza...)
grep -r "AIzaSy" --include="*.java" --include="*.ts" --include="*.js" --include="*.json" . 2>/dev/null

# JWT weak secrets
grep -r "supremeai_super_secret" --include="*.properties" --include="*.yml" --include="*.yaml" . 2>/dev/null

# Generic secret patterns
grep -rE "(password|secret|key|token)\s*=\s*['\"][^'\"]{8,}['\"]" --include="*.java" --include="*.ts" --include="*.env" . 2>/dev/null | head -20

echo "Done. Review hits carefully."
```

### **Script 2: Find Hardcoded Strings in Flutter**
```bash
#!/bin/bash
# find_flutter_hardcoded.sh
find lib/screens -name "*.dart" -exec grep -H "Text('.*')" {} \; | grep -v ".tr()"
```

### **Script 3: Find Missing i18n Keys (Dashboard)**
```bash
#!/bin/bash
# check_i18n.sh
cd dashboard/src

# Extract all t('...') calls
grep -r "t('.*')" --include="*.tsx" --include="*.ts" . | \
  sed "s/.*t('\([^']*\)').*/\1/" | sort > /tmp/used_keys.txt

# Extract all keys from en.json
grep -o '"[^"]*":' src/i18n/en.json | sed 's/":$//' | sed 's/^"//' | sort > /tmp/en_keys.txt

# Find missing keys
comm -23 /tmp/used_keys.txt /tmp/en_keys.txt > missing_keys.txt
echo "Missing keys:"
cat missing_keys.txt
```

---

## 📞 ESCALATION MATRIX

| Issue Category | Primary Owner | Secondary | Escalation To |
|----------------|---------------|-----------|---------------|
| Security P0 | Security Team | Backend Lead | CTO |
| Backend Bugs | Backend Team | - | Engineering Manager |
| Frontend | Frontend Team | - | Engineering Manager |
| Mobile | Mobile Team | - | Engineering Manager |
| IDE Extensions | Developer Experience | - | Product Manager |
| AWS/GCP Infra | DevOps | Backend | CTO |
| Testing Gaps | QA Lead | All Teams | Engineering Manager |

---

## 📌 TRACKING SPREADSHEET COLUMNS

When creating tracking issue (GitHub Issues, Jira, etc.), include columns:

1. **ID** - SEC-01, DASH-09, etc.
2. **Title** - Short descriptive title
3. **Severity** - 🔴 Critical / 🟠 High / 🟡 Medium / 🟢 Low
4. **Module** - Backend, Dashboard, Mobile, etc.
5. **File Path** - Specific file(s)
6. **Line Numbers** - if applicable
7. **Description** - What's wrong
8. **Impact** - Business/security impact
9. **Solution Hint** - High-level approach (full details in this doc)
10. **Implementation Details** - Code snippets, specific changes
11. **Effort Estimate** - hours/days
12. **Assigned To** - team/individual
13. **Status** - ❌ Not Started / 🟡 In Progress / ✅ Done / ⚠️ Blocked
14. **PR #** - Link to pull request
15. **Deployed** - ✅ Staging / ✅ Production / ❌ Not Deployed
16. **Verified By** - QA sign-off
17. **Notes** - Observations, blockers

---

## 🔄 REVIEW CHECKLIST

After implementing fixes, verify:

- [ ] All CRITICAL (🔴) issues addressed
- [ ] No hardcoded secrets remain in codebase (`git grep AIzaSy`, `git grep supremeai_super_secret`)
- [ ] CORS restrict to whitelist (test with curl from different origin)
- [ ] CSP verified with securityheaders.com (score A+)
- [ ] WebSocket handshake rejects unauthenticated connections (test)
- [ ] XSS payload sanitized in chat (test with `<script>alert()</script>`)
- [ ] Integration tests passing in CI (check GitHub Actions)
- [ ] Dashboard tests running (vitest coverage report)
- [ ] All Flutter hardcoded strings replaced (run grep for `Text('.*')` without `.tr()`)
- [ ] Bengali translations display correctly (QA check in UI)
- [ ] Rate limiting distributed (test with concurrent requests across pods)
- [ ] No blocking calls in reactive streams (run integration test with BlockHound)
- [ ] Admin audit logging capturing all admin actions
- [ ] Input validation rejecting invalid DTOs (send malformed requests)
- [ ] JWT secret ≥ 32 bytes, stored in Secret Manager
- [ ] API encryption key enforced in production
- [ ] Firebase functions auth validated (no x-secret bypass)
- [ ] Browser automation validates URLs, blocks private IPs
- [ ] Smart chat requires Firebase auth
- [ ] VS Code token in SecretStorage (check via Developer Tools)
- [ ] IntelliJ secret in CredentialStore (check via KeePass/OS keychain)

---

## 📚 RELATED DOCUMENTS

- Full Review Report: `kilo_review_2026-05-13.md`
- Security Audit: `SECURITY_AUDIT_2026-05-13.md`
- Testing Assessment: `TESTING_ASSESSMENT_2026-05-13.md`
- Localization Audit: `LOCALIZATION_AUDIT_2026-05-13.md`
- Architecture Diagrams: `ARCHITECTURE.md`
- Backend API Spec: `docs/API.md` (if exists)

---

## 🏁 NEXT STEPS

1. **Create GitHub Issues** for each of the 30 issues above using this document
2. **Assign owners** per module (Backend, Frontend, Mobile, DevOps)
3. **Set sprint goals:**
   - Sprint 1: Fix all CRITICAL security issues (1-10)
   - Sprint 2: Enable integration tests + Dashboard testing
   - Sprint 3: Mobile i18n completion + navigation
   - Sprint 4: Architecture improvements (state, polling, 3D)
4. **Daily standup** updates on P0 items until resolved
5. **Security re-audit** after all CRITICAL and HIGH fixes deployed
6. **Penetration test** before production launch

---

**Document Maintainer:** Kilo AI  
**Last Updated:** 2026-05-13  
**Next Review:** After initial fixes implemented
