# SimulatorController - Technical Implementation Specification

**Version:** 1.0  
**Date:** 2026-04-20  
**Author:** SupremeAI Engineering  
**Related Epic:** Phase 6 - Visualization & Auto-Iteration  

---

## 1. ARCHITECTURE OVERVIEW

```
┌─────────────────┐     ┌────────────────────┐     ┌──────────────────┐
│   Frontend UI   │────▶│ SimulatorController│────▶│ SimulatorService │
│ (Dashboard)     │     │   (REST API)       │     │   (Business      │
│                 │◀────│                    │◀────│    Logic)        │
└─────────────────┘     └────────────────────┘     └────────┬─────────┘
                                                             │
                ┌────────────────────────────────────────────┘
                ▼
        ┌─────────────────────┐
        │ SimulatorProfileRepo│
        │  (Firestore)        │
        └──────────┬──────────┘
                   │
        ┌──────────┴──────────┐
        ▼                     ▼
┌──────────────┐      ┌──────────────┐
│AppDeployment │      │ AuditLogRepo │
│Service       │      │ (Firestore)  │
└──────┬───────┘      └──────────────┘
       │
       ▼
┌──────────────┐      ┌──────────────┐
│CloudRun      │      │WebSocket     │
│Deployer      │◀─────│Handler       │
│(Preview Env) │      │(Real-time)   │
└──────────────┘      └──────────────┘
```

**Flow:**  

1. Frontend calls `POST /api/simulator/install`  
2. Controller → SimulatorService (authenticated user)  
3. Service validates quota via Firestore transaction  
4. Service deploys app to Cloud Run "preview" environment  
5. Service updates profile in Firestore (atomic transaction)  
6. Service logs audit event  
7. WebSocket notifies frontend of install completion  
8. Frontend shows "Launch" button with preview URL  

---

## 2. DATA MODEL SPECIFICATION

### 2.1 Entity Relationship Diagram

```
User (1) ──► (1) UserSimulatorProfile ──► (0..N) SimulatorApp
                                   │
                                   ├──► (0..1) SimulatorDevice
                                   │
                                   ├──► (0..1) SimulatorSession
                                   │
                                   └──► (N) QuotaHistory[]
```

### 2.2 Firestore Collection Schemas

#### Collection: `simulator_profiles`

**Document ID:** `{userId}` (matches Firebase Auth UID)

**Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `userId` | string | YES | Firebase UID |
| `installQuota` | integer | YES | Max concurrent apps (min: 1, max: 20) |
| `activeInstalls` | integer | YES | Current installed count (derived) |
| `installedApps` | array | YES | Array of `SimulatorApp` objects |
| `device` | object | YES | `SimulatorDevice` configuration |
| `currentSession` | object | NO | Active session if running |
| `lastActiveAt` | timestamp | YES | Last activity |
| `quotaHistory` | array | NO | Monthly usage tracking |
| `createdAt` | timestamp | YES | Profile creation |
| `updatedAt` | timestamp | YES | Last update |

**Indexes:**

- `lastActiveAt` (descending) - Query active users
- Composite: `[userId, installedApps.appId]` - Unique constraint per app

#### Sub-object: SimulatorApp

```json
{
  "appId": "string (UUID)",
  "appName": "string",
  "version": "string (semver)",
  "deployedUrl": "string (HTTPS URL)",
  "installedAt": "timestamp",
  "launchCount": "integer",
  "lastLaunchedAt": "timestamp (nullable)",
  "status": "string (INSTALLED/RUNNING/ERROR/EXPIRED)",
  "failureReason": "string (nullable)"
}
```

#### Sub-object: SimulatorDevice

```json
{
  "type": "string (PIXEL_6|IPHONE_15|SAMSUNG_S24|CUSTOM)",
  "osVersion": "string (Android 14|iOS 17.4)",
  "screenResolution": "string (widthxheight)",
  "densityDpi": "integer",
  "hasGooglePlayServices": "boolean",
  "customProperties": "map<string,string>"
}
```

#### Sub-object: SimulatorSession

```json
{
  "sessionId": "string (UUID)",
  "activeAppId": "string",
  "sessionUrl": "string (wss://...)",
  "startedAt": "timestamp",
  "lastHeartbeat": "timestamp",
  "state": "string (ACTIVE|PAUSED|TERMINATED|EXPIRED)",
  "metadata": "map<string,string>"
}
```

---

## 3. SERVICE LAYER DESIGN

### 3.1 Core Services

#### SimulatorProfileService

```java
@Service
@Slf4j
public class SimulatorProfileService {
    
    @Autowired
    private SimulatorProfileRepository profileRepository;
    
    @Autowired
    private SimulatorQuotaService quotaService;
    
    @Autowired
    private AuditLogService auditLogService;
    
    /**
     * Get or create profile for user
     */
    @Transactional
    public UserSimulatorProfile getOrCreateProfile(String userId) {
        return profileRepository.findByUserId(userId)
            .orElseGet(() -> {
                UserSimulatorProfile profile = new UserSimulatorProfile();
                profile.setUserId(userId);
                profile.setInstallQuota(5); // default
                profile.setActiveInstalls(0);
                profile.setInstalledApps(new ArrayList<>());
                profile.setDevice(SimulatorDevice.defaultDevice());
                profile.setLastActiveAt(LocalDateTime.now());
                return profileRepository.save(profile);
            });
    }
    
    /**
     * Install app to simulator - ATOMIC OPERATION
     */
    @Transactional
    public SimulatorInstallResult installApp(String userId, String appId, String deviceType) {
        UserSimulatorProfile profile = getOrCreateProfile(userId);
        
        // Pre-checks
        quotaService.validateCanInstall(profile, appId);
        
        // Deploy app (external)
        String previewUrl = deploymentService.deployToSimulator(appId, deviceType);
        
        // Create installed app entry
        SimulatorApp installedApp = SimulatorApp.builder()
            .appId(appId)
            .appName(appMetadata.getName())
            .version(appMetadata.getVersion())
            .deployedUrl(previewUrl)
            .installedAt(LocalDateTime.now())
            .launchCount(0)
            .status(SimulatorAppStatus.INSTALLED)
            .build();
        
        // Update profile atomically
        profile.getInstalledApps().add(installedApp);
        profile.setActiveInstalls(profile.getActiveInstalls() + 1);
        profile.setLastActiveAt(LocalDateTime.now());
        profile.getQuotaHistory().add(
            QuotaHistoryEntry.of(LocalDateTime.now(), profile.getActiveInstalls())
        );
        
        profileRepository.save(profile);
        
        // Audit
        auditLogService.logSimulatorEvent("APP_INSTALL", userId, 
            Map.of("appId", appId, "previewUrl", previewUrl));
        
        return SimulatorInstallResult.success(installedApp, profile);
    }
    
    /**
     * Uninstall app from simulator
     */
    @Transactional
    public void uninstallApp(String userId, String appId) {
        UserSimulatorProfile profile = getOrCreateProfile(userId);
        
        boolean removed = profile.getInstalledApps().removeIf(app -> 
            app.getAppId().equals(appId) && 
            app.getStatus() != SimulatorAppStatus.RUNNING
        );
        
        if (removed) {
            profile.setActiveInstalls(profile.getActiveInstalls() - 1);
            profile.setLastActiveAt(LocalDateTime.now());
            profileRepository.save(profile);
            
            // Terminate running session if any
            if (profile.getCurrentSession() != null && 
                profile.getCurrentSession().getActiveAppId().equals(appId)) {
                sessionService.terminateSession(profile.getCurrentSession().getSessionId());
            }
            
            auditLogService.logSimulatorEvent("APP_UNINSTALL", userId, 
                Map.of("appId", appId));
        }
    }
}
```

#### SimulatorQuotaService

```java
@Service
@Slf4j
public class SimulatorQuotaService {
    
    /**
     * Validate if user can install app
     */
    public void validateCanInstall(UserSimulatorProfile profile, String appId) {
        // Check quota
        if (profile.getActiveInstalls() >= profile.getInstallQuota()) {
            throw new SimulatorQuotaExceededException(
                String.format("Quota exceeded: %d/%d apps installed", 
                    profile.getActiveInstalls(), profile.getInstallQuota())
            );
        }
        
        // Check duplicate
        boolean alreadyInstalled = profile.getInstalledApps().stream()
            .anyMatch(app -> app.getAppId().equals(appId));
        if (alreadyInstalled) {
            throw new SimulatorConflictException(
                "App already installed in simulator"
            );
        }
        
        // Check app ownership (call to app service)
        if (!appOwnershipService.belongsToUser(appId, profile.getUserId())) {
            throw new AccessDeniedException("App not owned by user");
        }
    }
    
    /**
     * Check if user has reached install limit
     */
    public boolean hasQuotaRemaining(UserSimulatorProfile profile) {
        return profile.getActiveInstalls() < profile.getInstallQuota();
    }
    
    /**
     * Get remaining slots
     */
    public int getRemainingSlots(UserSimulatorProfile profile) {
        return profile.getInstallQuota() - profile.getActiveInstalls();
    }
}
```

#### SimulatorDeploymentService

```java
@Service
@Slf4j
public class SimulatorDeploymentService {
    
    @Autowired
    private CloudRunDeploymentService cloudRunService;
    
    @Value("${simulator.cloudrun.region:us-central1}")
    private String region;
    
    @Value("${simulator.preview.url.format}")
    private String urlFormat;
    
    /**
     * Deploy app to simulator preview environment
     * Creates isolated Cloud Run service per app instance
     */
    public String deployToSimulator(String appId, String deviceType) {
        String serviceName = String.format("sim-%s-%s", appId, deviceType.toLowerCase());
        
        try {
            // Deploy to Cloud Run with simulator-specific config
            String url = cloudRunService.deployService(
                serviceName,
                appId,
                DeploymentConfig.builder()
                    .memory("2Gi")
                    .cpu(1)
                    .minInstances(0)
                    .maxInstances(1)  // single instance per preview
                    .build()
            );
            
            log.info("Deployed simulator for app {} at {}", appId, url);
            return url;
        } catch (Exception e) {
            log.error("Failed to deploy simulator for app " + appId, e);
            throw new SimulatorDeploymentException(
                "Failed to launch simulator: " + e.getMessage(), e
            );
        }
    }
    
    /**
     * Undeploy app from simulator (cleanup)
     */
    public void undeployFromSimulator(String appId) {
        String serviceName = String.format("sim-%s-*", appId);
        cloudRunService.deleteServicesByPattern(serviceName);
    }
}
```

---

## 4. REPOSITORY LAYER

```java
@Repository
public interface SimulatorProfileRepository 
    extends ReactiveCrudRepository<UserSimulatorProfile, String> {
    
    /**
     * Find by Firebase UID
     */
    Mono<UserSimulatorProfile> findByUserId(String userId);
    
    /**
     * Find active profiles (has active installs)
     */
    Flux<UserSimulatorProfile> findByActiveInstallsGreaterThan(int min);
    
    /**
     * Count total simulator usage across platform
     */
    Mono<Long> countAllActiveInstalls();
}
```

**Firestore Configuration:**

```java
@Configuration
public class FirestoreSimulatorConfig {
    
    @Bean
    public QueryGateway queryGateway() { ... }
    
    /**
     * Ensure indexes exist for simulator_profiles collection
     */
    @PostConstruct
    public void ensureIndexes() {
        // Composite index: userId + installedApps.appId (for uniqueness)
        // Index: lastActiveAt (for active user queries)
    }
}
```

---

## 5. CONTROLLER DESIGN

```java
@RestController
@RequestMapping("/api/simulator")
@Validated
@Tag(name = "Simulator", description = "Simulator management endpoints")
public class SimulatorControllerV2 {
    
    @Autowired
    private SimulatorService simulatorService;
    
    @Autowired
    private SimulatorQuotaService quotaService;
    
    @Autowired
    private SimulatorProfileRepository profileRepository;
    
    // ─────────────────────────────────────────────────────────────
    // Profile Management
    // ─────────────────────────────────────────────────────────────
    
    @GetMapping("/profile/{userId}")
    @Operation(summary = "Get simulator profile", 
               description = "Returns user's simulator configuration and installed apps")
    @PreAuthorize("authentication.name == #userId or hasRole('ADMIN')")
    public Mono<UserSimulatorProfileDTO> getProfile(
            @PathVariable String userId,
            Authentication auth) {
        return simulatorService.getOrCreateProfile(userId)
            .map(this::toProfileDTO);
    }
    
    @PostMapping("/profile/{userId}")
    @Operation(summary = "Update simulator profile")
    @PreAuthorize("authentication.name == #userId or hasRole('ADMIN')")
    public Mono<UserSimulatorProfileDTO> updateProfile(
            @PathVariable String userId,
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication auth) {
        return simulatorService.updateProfile(userId, request)
            .map(this::toProfileDTO);
    }
    
    // ─────────────────────────────────────────────────────────────
    // Installation Management
    // ─────────────────────────────────────────────────────────────
    
    @PostMapping("/install")
    @Operation(summary = "Install app to simulator")
    @PreAuthorize("isAuthenticated()")
    public Mono<SimulatorInstallResponse> installApp(
            @Valid @RequestBody InstallAppRequest request,
            Authentication auth) {
        String userId = auth.getName();
        
        return simulatorService.installApp(
                userId, 
                request.getAppId(), 
                request.getDeviceProfile()
            )
            .map(this::toInstallResponse);
    }
    
    @DeleteMapping("/install/{appId}")
    @Operation(summary = "Uninstall app from simulator")
    @PreAuthorize("isAuthenticated()")
    public Mono<ResponseEntity<Void>> uninstallApp(
            @PathVariable String appId,
            Authentication auth) {
        return simulatorService.uninstallApp(auth.getName(), appId)
            .then(Mono.just(ResponseEntity.ok().build()))
            .onErrorResume(ResourceNotFoundException.class, 
                e -> Mono.just(ResponseEntity.notFound().build()));
    }
    
    @GetMapping("/installed")
    @Operation(summary = "List installed apps")
    @PreAuthorize("isAuthenticated()")
    public Mono<InstalledAppsResponse> getInstalledApps(Authentication auth) {
        return simulatorService.getInstalledApps(auth.getName())
            .map(this::toInstalledResponse);
    }
    
    // ─────────────────────────────────────────────────────────────
    // Session Management
    // ─────────────────────────────────────────────────────────────
    
    @PostMapping("/session/start")
    @Operation(summary = "Start simulator session for an installed app")
    @PreAuthorize("isAuthenticated()")
    public Mono<SessionStartResponse> startSession(
            @RequestParam String appId,
            Authentication auth) {
        return simulatorService.startSession(auth.getName(), appId)
            .map(this::toSessionResponse);
    }
    
    @PostMapping("/session/stop")
    @Operation(summary = "Stop current simulator session")
    @PreAuthorize("isAuthenticated()")
    public Mono<Void> stopSession(Authentication auth) {
        return simulatorService.stopSession(auth.getName());
    }
    
    @GetMapping("/session/status")
    @Operation(summary = "Get current session status")
    @PreAuthorize("isAuthenticated()")
    public Mono<SessionStatusResponse> getSessionStatus(Authentication auth) {
        return simulatorService.getSessionStatus(auth.getName())
            .map(this::toSessionStatusResponse);
    }
    
    // ─────────────────────────────────────────────────────────────
    // Device Management
    // ─────────────────────────────────────────────────────────────
    
    @GetMapping("/devices")
    @Operation(summary = "List available device profiles")
    @PreAuthorize("isAuthenticated()")
    public Mono<List<DeviceProfileDTO>> getAvailableDevices() {
        return Mono.just(SimulatorDevice.getAvailableProfiles());
    }
    
    @PostMapping("/device/configure")
    @Operation(summary = "Update device configuration")
    @PreAuthorize("isAuthenticated()")
    public Mono<DeviceProfileDTO> configureDevice(
            @Valid @RequestBody DeviceConfigureRequest request,
            Authentication auth) {
        return simulatorService.configureDevice(auth.getName(), request)
            .map(this::toDeviceDTO);
    }
    
    // ─────────────────────────────────────────────────────────────
    // Admin Endpoints
    // ─────────────────────────────────────────────────────────────
    
    @GetMapping("/admin/usage")
    @Operation(summary = "Get simulator usage across all users (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<AdminUserUsageDTO> getAllUsage() {
        return simulatorService.getAllUserUsage()
            .map(this::toAdminUsageDTO);
    }
    
    @PostMapping("/admin/set-quota/{userId}")
    @Operation(summary = "Override user's install quota (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<UserSimulatorProfileDTO> adminSetQuota(
            @PathVariable String userId,
            @RequestParam int quota,
            Authentication auth) {
        return simulatorService.adminSetQuota(userId, quota)
            .map(this::toProfileDTO);
    }
    
    @DeleteMapping("/admin/cleanup/stale")
    @Operation(summary = "Clean up stale sessions and expired apps (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<CleanupResultDTO> cleanupStale() {
        return simulatorService.cleanupStaleData()
            .map(this::toCleanupResult);
    }
    
    // ─────────────────────────────────────────────────────────────
    // Webhook (Internal)
    // ─────────────────────────────────────────────────────────────
    
    @PostMapping("/webhook/deployment-complete")
    @Operation(summary = "[INTERNAL] Called by Cloud Run on deployment completion")
    public Mono<Void> onDeploymentComplete(
            @Valid @RequestBody DeploymentWebhook payload,
            @RequestHeader("X-Simulator-Secret") String secret) {
        // TODO: Add service account authentication
        if (!isValidInternalSecret(secret)) {
            return Mono.error(new AccessDeniedException("Invalid webhook secret"));
        }
        
        return simulatorService.handleDeploymentComplete(payload);
    }
    
    // ─────────────────────────────────────────────────────────────
    // Utilities
    // ─────────────────────────────────────────────────────────────
    
    private UserSimulatorProfileDTO toProfileDTO(UserSimulatorProfile p) { ... }
    private SimulatorInstallResponse toInstallResponse(SimulatorInstallResult r) { ... }
    // ... mappers
}
```

---

## 6. DTOs (Data Transfer Objects)

```java
// Request DTOs
public record InstallAppRequest(
    @NotNull String appId,
    @Pattern(regexp = "PIXEL_6|IPHONE_15|SAMSUNG_S24|CUSTOM") String deviceProfile
) {}

public record UpdateProfileRequest(
    @Min(1) @Max(20) Integer installQuota,
    DeviceConfigureRequest device
) {}

public record DeviceConfigureRequest(
    String type,
    String osVersion,
    String screenResolution,
    Integer densityDpi
) {}

// Response DTOs
public record UserSimulatorProfileDTO(
    String userId,
    int installQuota,
    int activeInstalls,
    List<SimulatorAppDTO> installedApps,
    DeviceProfileDTO device,
    SessionStatusDTO currentSession,
    Instant lastActiveAt
) {}

public record SimulatorAppDTO(
    String appId,
    String appName,
    String version,
    String previewUrl,
    Instant installedAt,
    int launchCount,
    SimulatorAppStatus status
) {}

public record SessionStartResponse(
    String sessionId,
    String websocketUrl,
    SessionState state,
    Instant startedAt
) {}
```

---

## 7. ERROR HANDLING

```java
@RestControllerAdvice
public class SimulatorExceptionHandler {
    
    @ExceptionHandler(SimulatorQuotaExceededException.class)
    public ResponseEntity<ErrorResponse> handleQuotaExceeded(
            SimulatorQuotaExceededException ex) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)  // 409
            .body(ErrorResponse.of(
                "QUOTA_EXCEEDED", 
                ex.getMessage(),
                Map.of("quota", ex.getUsed(), "limit", ex.getLimit())
            ));
    }
    
    @ExceptionHandler(SimulatorDeploymentException.class)
    public ResponseEntity<ErrorResponse> handleDeploymentFailed(
            SimulatorDeploymentException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_GATEWAY)  // 502
            .body(ErrorResponse.of(
                "DEPLOYMENT_FAILED",
                "Failed to launch simulator: " + ex.getMessage(),
                null
            ));
    }
    
    @ExceptionHandler(SimulatorResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            SimulatorResourceNotFoundException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse.of(
                "NOT_FOUND",
                ex.getMessage(),
                null
            ));
    }
}
```

---

## 8. WEBSOCKET REAL-TIME UPDATES

```java
@Configuration
@EnableWebSocketMessageBroker
public class SimulatorWebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/simulator")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}

@Component
public class SimulatorWebSocketHandler {
    
    @Autowired
    private SimulatorNotificationService notificationService;
    
    @MessageMapping("/simulator/install-progress")
    public void handleInstallProgress(SimulatorProgressMessage msg) {
        // Broadcast progress to user
        messagingTemplate.convertAndSendToUser(
            msg.getUserId(),
            "/queue/simulator",
            SimulatorProgressDTO.builder()
                .appId(msg.getAppId())
                .progress(msg.getProgress())
                .stage(msg.getStage())
                .build()
        );
    }
}
```

**Frontend subscription:**

```javascript
const socket = new SockJS('/ws/simulator');
const stompClient = Stomp.over(socket);

stompClient.connect({}, () => {
    stompClient.subscribe('/user/queue/simulator', (msg) => {
        const update = JSON.parse(msg.body);
        updateProgressBar(update.appId, update.progress);
    });
});
```

---

## 9. AUTOMATED CLEANUP JOB

```java
@Component
public class SimulatorCleanupScheduler {
    
    @Autowired
    private SimulatorProfileRepository profileRepository;
    
    @Autowired
    private SimulatorDeploymentService deploymentService;
    
    /**
     * Run daily: remove expired apps and stale sessions
     */
    @Scheduled(cron = "0 3 * * *") // 3 AM daily
    @Transactional
    public void cleanupStaleData() {
        Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
        
        profileRepository.findAll()
            .filter(p -> p.getLastActiveAt().isBefore(cutoff))
            .flatMap(profile -> {
                // Remove expired installed apps
                List<SimulatorApp> toRemove = profile.getInstalledApps().stream()
                    .filter(app -> app.getInstalledAt().isBefore(cutoff))
                    .toList();
                
                profile.getInstalledApps().removeAll(toRemove);
                profile.setActiveInstalls(profile.getInstalledApps().size());
                
                // Undeploy from Cloud Run
                toRemove.forEach(app -> 
                    deploymentService.undeployFromSimulator(app.getAppId()));
                
                return profileRepository.save(profile);
            })
            .subscribe();
    }
}
```

---

## 10. SECURITY CONSIDERATIONS

### Authentication

- All endpoints require Firebase ID token (enforced by `AuthenticationFilter`)
- WebSocket upgrade request validated via session cookie

### Authorization

- Users can only access their own profiles
- Admin role required for admin endpoints
- Service account for internal webhooks

### Data Isolation

- Firestore queries always filter by `userId`
- Multi-tenant separation enforced at repository layer
- No cross-user data leakage

---

## 11. PERFORMANCE OPTIMIZATIONS

### Cache Layer

```java
@Service
@CacheConfig(cacheNames = "simulator-profiles")
public class SimulatorProfileCacheService {
    
    @Cacheable(key = "#userId")
    public UserSimulatorProfile getProfile(String userId) { ... }
    
    @CacheEvict(key = "#userId")
    public void evictProfile(String userId) { ... }
}
```

### Firestore Transactions

```java
@Transactional
public SimulatorInstallResult installApp(...) {
    // Entire operation runs in single Firestore transaction
    // Prevents race conditions when multiple concurrent installs
}
```

### Connection Pooling

- Cloud Run client connection pool: 20 connections
- Firestore reactive client: 100 concurrent streams

---

## 12. TESTING CHECKLIST

### Unit Tests

- [x] Quota validation logic
- [x] Install/uninstall atomicity
- [x] Duplicate app detection
- [ ] Device validation
- [ ] Session state transitions

### Integration Tests

- [x] End-to-end install flow with Firestore emulator
- [ ] Concurrent install race condition test
- [ ] WebSocket message delivery
- [ ] Admin quota override

### Contract Tests

```yaml
# openapi.yaml - simulator endpoints
paths:
  /api/simulator/install:
    post:
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/InstallAppRequest'
      responses:
        '201':
          description: App installed
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/InstallResponse'
```

---

## 13. DEPLOYMENT CHECKLIST

### Infrastructure

- [ ] Firestore collection `simulator_profiles` created with indexes
- [ ] Cloud Run service account has `run.services.deploy` permission
- [ ] Redis cache (optional) configured for profile caching
- [ ] Prometheus metrics endpoint exposed
- [ ] WebSocket endpoint load balancer configured

### CI/CD

- [ ] Build simulator module: `./gradlew :supremeai:compileJava`
- [ ] Run simulator-specific tests: `./gradlew test --tests "*Simulator*"`
- [ ] Integration test with Firestore emulator in CI
- [ ] Deploy to staging first, smoke test install flow

### Monitoring

- [ ] Grafana dashboard for simulator metrics
- [ ] Alert on `simulator_active_sessions > 1000`
- [ ] Alert on `simulator_installs_failed_total` spike

---

## 14. OPEN QUESTIONS

| Question | Decision | Owner |
|----------|----------|-------|
| Should preview environments auto-delete? | YES - after 7 days | Product |
| Max concurrent sessions per user? | 1 (single active session) | Eng |
| Allow custom device profiles? | Phase 2 (predefined only v1) | Product |
| Should simulator include network throttling? | No - future enhancement | Eng |
| Persist user app data across installs? | No - fresh install each time | Product |

---

## 15. REFERENCES

- `src/main/java/com/supremeai/controller/SimulatorController.java` - current stub  
- `src/main/java/com/supremeai/model/UserSimulatorProfile.java` - current model  
- `docs_new/architecture/10-IMPLEMENTATION/SUPREMEAI_COMPLETE_EXECUTION_PLAN.md` - roadmap  
- Firebase Firestore documentation: https://firebase.google.com/docs/firestore

---

**END OF SPECIFICATION**
