# Simulator Development Quick Start

**For Developers Implementing Simulator Features**  
**Prerequisite:** Read `SIMULATOR_CONTROLLER_PERFECTION_PLAN.md` first

---

## 🚦 Current State Assessment

```
SimulatorController.java            [43 lines]  Basic CRUD stub
UserSimulatorProfile.java           [21 lines]  Minimal model
Firestore Repository                [MISSING]   No persistence layer
DeploymentService                   [MISSING]   No Cloud Run integration
WebSocket Handler                   [MISSING]   No real-time updates
Frontend UI                         [MISSING]   No React components
```

**Integration Points:**

- `AuthenticationFilter` - already provides Firebase auth
- `CodeGenerationService` - produces apps to install
- `CloudRunDeploymentService` - exists, needs simulator extension
- `AuditLogService` - exists, needs simulator events

---

## 📦 Step 1: Set Up Firestore Repository

```java
// src/main/java/com/supremeai/repository/SimulatorProfileRepository.java
@Repository
public interface SimulatorProfileRepository 
    extends ReactiveCrudRepository<UserSimulatorProfile, String> {
    
    Mono<UserSimulatorProfile> findByUserId(String userId);
    
    Flux<UserSimulatorProfile> findByActiveInstallsGreaterThan(int count);
}
```

**Firestore Collection:** `simulator_profiles`  
**Document ID:** Firebase user ID

Add to `scripts/setup-firestore-collections.js`:

```javascript
await firestore.createCollection('simulator_profiles', {
  singleFieldIndexes: [
    { fieldPath: 'lastActiveAt' },
    { fieldPath: 'activeInstalls' }
  ]
});
```

---

## 🔧 Step 2: Implement Core Service

```java
// src/main/java/com/supremeai/service/SimulatorService.java
@Service
@Slf4j
@RequiredArgsConstructor
public class SimulatorService {
    
    private final SimulatorProfileRepository profileRepository;
    private final SimulatorDeploymentService deploymentService;
    private final SimulatorQuotaService quotaService;
    private final AuditLogService auditLogService;
    
    /**
     * Install a generated app to user's simulator
     */
    @Transactional
    public SimulatorInstallResult installApp(String userId, String appId, String deviceType) {
        // TODO: Implement from SPECIFICATION document
        // 1. Load profile (or create default)
        // 2. quotaService.validateCanInstall(...)
        // 3. Deploy to Cloud Run preview
        // 4. Update profile atomically
        // 5. Log audit event
        // 6. Return result with preview URL
        throw new UnsupportedOperationException("To be implemented");
    }
    
    /**
     * Uninstall app from simulator
     */
    @Transactional
    public void uninstallApp(String userId, String appId) {
        // TODO: Implement
        throw new UnsupportedOperationException("To be implemented");
    }
    
    /**
     * Start simulator session (launch app)
     */
    public SessionStartResult startSession(String userId, String appId) {
        // TODO: Implement
        throw new UnsupportedOperationException("To be implemented");
    }
    
    /**
     * Get user's installed apps
     */
    public Mono<UserSimulatorProfile> getUserProfile(String userId) {
        return profileRepository.findByUserId(userId)
            .switchIfEmpty(Mono.defer(() -> 
                profileRepository.save(createDefaultProfile(userId))
            ));
    }
    
    private UserSimulatorProfile createDefaultProfile(String userId) {
        UserSimulatorProfile profile = new UserSimulatorProfile();
        profile.setUserId(userId);
        profile.setInstallQuota(5);
        profile.setActiveInstalls(0);
        profile.setInstalledApps(new ArrayList<>());
        profile.setDevice(SimulatorDevice.defaultDevice());
        profile.setLastActiveAt(LocalDateTime.now());
        return profile;
    }
}
```

---

## ⚙️ Step 3: Implement Quota Service

```java
// src/main/java/com/supremeai/service/SimulatorQuotaService.java
@Service
@Slf4j
public class SimulatorQuotaService {
    
    /**
     * Validates user can install an app
     * @throws SimulatorQuotaExceededException if limit reached
     * @throws SimulatorConflictException if app already installed
     */
    public void validateCanInstall(UserSimulatorProfile profile, String appId) {
        // Check quota
        if (profile.getActiveInstalls() >= profile.getInstallQuota()) {
            throw new SimulatorQuotaExceededException(
                profile.getActiveInstalls(), 
                profile.getInstallQuota()
            );
        }
        
        // Check duplicate
        boolean exists = profile.getInstalledApps().stream()
            .anyMatch(app -> app.getAppId().equals(appId));
        if (exists) {
            throw new SimulatorConflictException("App already installed");
        }
    }
}
```

---

## 🚀 Step 4: Implement Deployment Service

```java
// src/main/java/com/supremeai/service/SimulatorDeploymentService.java
@Service
@Slf4j
public class SimulatorDeploymentService {
    
    @Autowired
    private CloudRunDeploymentService cloudRun; // existing service
    
    @Value("${simulator.cloudrun.region:us-central1}")
    private String region;
    
    /**
     * Deploys generated app to preview environment
     * Creates isolated Cloud Run service per app+device combo
     */
    public String deployToSimulator(String appId, String deviceType) {
        String serviceName = String.format("sim-%s-%s", 
            appId.toLowerCase(), 
            deviceType.toLowerCase()
        );
        
        try {
            // Deploy using existing Cloud Run service
            return cloudRun.deploySimulatorPreview(
                serviceName,
                appId,
                getSimulatorRuntimeConfig(deviceType)
            );
        } catch (CloudRunException e) {
            throw new SimulatorDeploymentException(
                "Failed to deploy simulator: " + e.getMessage(), 
                e
            );
        }
    }
    
    private DeploymentConfig getSimulatorRuntimeConfig(String deviceType) {
        return DeploymentConfig.builder()
            .memory("2Gi")
            .cpu(1)
            .minInstances(0)
            .maxInstances(1)
            .environment(Map.of(
                "SIMULATOR_MODE", "preview",
                "DEVICE_PROFILE", deviceType
            ))
            .build();
    }
}
```

**Extend `CloudRunDeploymentService` if needed:**

```java
public class CloudRunDeploymentService {
    
    public String deploySimulatorPreview(String serviceName, String appId, 
                                          DeploymentConfig config) {
        // Reuse existing Cloud Run deployment code
        // Build from app artifacts stored in GCS
        // Return public HTTPS URL
        throw new UnsupportedOperationException("Implement");
    }
}
```

---

## 📡 Step 5: WebSocket Handler (Real-time)

```java
// src/main/java/com/supremeai/websocket/SimulatorWebSocketHandler.java
@Component
@EnableWebSocketMessageBroker
public class SimulatorWebSocketConfig 
    implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }
    
    @Override
    public void registerStompendpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/simulator")
            .setAllowedOriginPatterns("*")
            .withSockJS();
    }
}

@Component
public class SimulatorNotificationService {
    
    @Autowired
    private SimulatorMessagingTemplate messagingTemplate;
    
    /**
     * Send real-time install progress to user
     */
    public void sendInstallProgress(String userId, String appId, int percent) {
        messagingTemplate.convertAndSendToUser(
            userId,
            "/queue/simulator",
            Map.of(
                "type", "INSTALL_PROGRESS",
                "appId", appId,
                "percent", percent
            )
        );
    }
}
```

**Frontend usage (later):**

```javascript
const socket = new SockJS('/ws/simulator');
const client = Stomp.over(socket);
client.connect({}, () => {
    client.subscribe('/user/queue/simulator', (msg) => {
        showProgress(JSON.parse(msg.body));
    });
});
```

---

## 🛡️ Step 6: Exception Hierarchy

```java
// src/main/java/com/supremeai/exception/SimulatorException.java
public class SimulatorException extends RuntimeException {
    public SimulatorException(String message) { super(message); }
}

public class SimulatorQuotaExceededException extends SimulatorException {
    private final int used;
    private final int limit;
    // constructor + getters
}

public class SimulatorDeploymentException extends SimulatorException {
    // deployment-specific errors
}

public class SimulatorResourceNotFoundException extends SimulatorException {
    // app not found
}

public class SimulatorConflictException extends SimulatorException {
    // duplicate install, session conflict
}
```

---

## 🎯 Step 7: Extend SimulatorController

```java
// Update: src/main/java/com/supremeai/controller/SimulatorController.java
@RestController
@RequestMapping("/api/simulator")
@RequiredArgsConstructor
@Validated
public class SimulatorController {
    
    private final SimulatorService simulatorService;
    private final SimulatorQuotaService quotaService;
    
    // Keep existing profile endpoints (delegate to service)
    
    @PostMapping("/install")
    @PreAuthorize("isAuthenticated()")
    public Mono<SimulatorInstallResponse> installApp(
            @Valid @RequestBody InstallAppRequest request,
            Authentication auth) {
        try {
            SimulatorInstallResult result = simulatorService.installApp(
                auth.getName(), 
                request.getAppId(), 
                request.getDeviceProfile()
            );
            return Mono.just(SimulatorInstallResponse.success(result));
        } catch (SimulatorQuotaExceededException e) {
            return Mono.just(SimulatorInstallResponse.quotaExceeded(e));
        }
    }
    
    @DeleteMapping("/install/{appId}")
    @PreAuthorize("isAuthenticated()")
    public Mono<ResponseEntity<Void>> uninstallApp(
            @PathVariable String appId,
            Authentication auth) {
        simulatorService.uninstallApp(auth.getName(), appId);
        return Mono.just(ResponseEntity.ok().build());
    }
    
    @GetMapping("/installed")
    @PreAuthorize("isAuthenticated()")
    public Mono<InstalledAppsResponse> getInstalled(Authentication auth) {
        return simulatorService.getUserProfile(auth.getName())
            .map(profile -> new InstalledAppsResponse(
                profile.getInstalledApps(),
                profile.getActiveInstalls(),
                profile.getInstallQuota()
            ));
    }
    
    // TODO: Add remaining endpoints from spec
}
```

---

## 🧪 Step 8: Testing Your Implementation

### Unit Test Example

```java
// src/test/java/com/supremeai/service/SimulatorServiceTest.java
@SpringBootTest
class SimulatorServiceTest {
    
    @Autowired
    private SimulatorService service;
    
    @Autowired
    private SimulatorProfileRepository repo;
    
    @Test
    @Transactional
    void testInstallApp_WithinQuota() {
        // Arrange
        String userId = "test-user-1";
        String appId = "app-123";
        
        UserSimulatorProfile profile = new UserSimulatorProfile();
        profile.setUserId(userId);
        profile.setInstallQuota(5);
        profile.setActiveInstalls(0);
        repo.save(profile).block();
        
        // Act
        SimulatorInstallResult result = service.installApp(userId, appId, "PIXEL_6");
        
        // Assert
        assertTrue(result.success());
        assertEquals(1, result.getProfile().getActiveInstalls());
        assertNotNull(result.getPreviewUrl());
    }
    
    @Test
    void testInstallApp_QuotaExceeded() {
        // Arrange: user at quota
        String userId = "test-user-2";
        UserSimulatorProfile profile = createProfileAtQuota(userId, 5, 5);
        repo.save(profile).block();
        
        // Act/Assert
        assertThrows(SimulatorQuotaExceededException.class, () -> {
            service.installApp(userId, "new-app", "PIXEL_6");
        });
    }
}
```

### Integration Test with Firestore Emulator

```bash
# Start Firestore emulator
gcloud beta emulators firestore start --host-port=localhost:8080

# Set env vars
export FIRESTORE_EMULATOR_HOST=localhost:8080

# Run tests
./gradlew test --tests "*SimulatorIntegrationTest*"
```

---

## 📊 Step 9: Monitoring Setup

Add to `SimulatorDeploymentService`:

```java
@Autowired
private MeterRegistry meterRegistry;

private final Counter installCounter;
private final Timer installTimer;

public SimulatorDeploymentService(MeterRegistry registry) {
    this.installCounter = Counter.builder("simulator.installs.total")
        .description("Total app installations")
        .tag("region", region)
        .register(registry);
    
    this.installTimer = Timer.builder("simulator.install.duration")
        .description("Installation duration")
        .publishPercentiles(0.5, 0.95, 0.99)
        .register(registry);
}

public String deployToSimulator(String appId, String deviceType) {
    return installTimer.record(() -> {
        // deployment logic...
        installCounter.increment();
        return url;
    });
}
```

**Grafana Panel Query:**

```
histogram_quantile(0.95, rate(simulator_install_duration_seconds_bucket[5m]))
```

---

## 🔐 Step 10: Security Hardening

### Validate App Ownership

```java
@Service
public class AppOwnershipService {
    
    @Autowired
    private ExistingProjectRepository projectRepository; // or GeneratedApp repo
    
    public boolean belongsToUser(String appId, String userId) {
        return projectRepository.findByAppIdAndOwnerId(appId, userId)
            .hasElement();
    }
}
```

### Rate Limiting (Optional)

Add per-user rate limit on install attempts (prevents abuse):

```java
@RateLimited(key = "#userId", limit = "10/ minute")
public void installApp(String userId, String appId, String deviceType) {
    // ... existing logic
}
```

---

## 🚢 Step 11: Deployment Configuration

Add to `src/main/resources/application.properties`:

```properties
# Simulator settings
simulator.max.installs.per.user=${SIMULATOR_MAX_INSTALLS:5}
simulator.session.timeout.minutes=${SIMULATOR_SESSION_TIMEOUT:30}
simulator.deployment.timeout.seconds=${SIMULATOR_DEPLOY_TIMEOUT:120}
simulator.auto.cleanup.enabled=${SIMULATOR_AUTO_CLEANUP:true}
simulator.app.expiry.days=${SIMULATOR_APP_EXPIRY_DAYS:7}

# Preview URL
simulator.preview.url.format=https://{appId}-simulator.{domain}
simulator.preview.domain=${SIMULATOR_DOMAIN:run.app}

# Cloud Run
simulator.cloudrun.region=${SIMULATOR_REGION:us-central1}
simulator.cloudrun.memory=${SIMULATOR_MEMORY:2Gi}
simulator.cloudrun.cpu=${SIMULATOR_CPU:1}

# WebSocket
simulator.websocket.allowed-origins=${SIMULATOR_WS_ORIGINS:*}
```

---

## 📈 Step 12: Observability

### Logging

```java
log.info("Installing app {} to simulator for user {}", appId, userId);
log.debug("Deployment config: {}", config);
log.error("Failed to deploy simulator for app " + appId, e);
```

**Structured log fields:**

```
timestamp, level, logger, userId, appId, deviceType, durationMs, outcome
```

### Metrics to Track

| Metric | Type | Labels | Description |
|--------|------|--------|-------------|
| `simulator_installs_total` | Counter | `userId`, `device` | Count of installs |
| `simulator_installs_failed_total` | Counter | `userId`, `reason` | Failed installations |
| `simulator_active_sessions` | Gauge | `userId` | Currently running |
| `simulator_install_duration_seconds` | Histogram | `userId` | P50, P95, P99 latency |
| `simulator_quota_exceeded_total` | Counter | `userId` | Quota rejection count |

---

## 🐛 Debugging Tips

### Issue: Install hangs

**Check:** Cloud Run deployment permissions, service account has `run.deploy` role

### Issue: WebSocket not connecting

**Check:** `application.properties` WebSocket CORS config; SockJS endpoint reachable

### Issue: Quota not enforced

**Check:** Firestore transaction boundaries; concurrent update handling

### Issue: Preview URL 404 after install

**Check:** Cloud Run service deployed successfully; service name pattern matches; IAM policy allows invoker

---

## ✅ Feature Completion Checklist

Before marking simulator feature "done":

- [ ] All endpoints return correct HTTP status codes
- [ ] Firestore data persists across server restarts
- [ ] Concurrent installs don't exceed quota (race condition tested)
- [ ] WebSocket updates arrive within 1 second
- [ ] Audit logs contain userId, appId, timestamp for all actions
- [ ] Admins can view all user simulator profiles
- [ ] Preview URLs are unique and secure (unguessable)
- [ ] Failed deployments are cleaned up (no orphan Cloud Run services)
- [ ] Daily cleanup job removes apps > 7 days old
- [ ] Frontend panel shows installed apps, quota, launch button
- [ ] Documentation exists in `docs_new/guides/`
- [ ] All unit tests pass (>80% coverage on service layer)
- [ ] Performance: < 2 second install latency (p95)

---

## 🏁 Quick Start Summary

1. **Create repository** - `SimulatorProfileRepository extends ReactiveCrudRepository`
2. **Implement service** - `SimulatorService.installApp()` with quota check
3. **Add deployment** - `SimulatorDeploymentService.deployToSimulator()`
4. **Update controller** - Replace stubs with real logic
5. **Write tests** - Unit + integration with Firestore emulator
6. **Add metrics** - Prometheus counters/histograms
7. **Deploy to dev** - Smoke test manually via Postman
8. **Build frontend** - React panel with installed apps grid
9. **Document** - User guide in `docs_new/guides/`
10. **Ship!** - Mark epic complete

---

**Questions?** See full spec: `SIMULATOR_CONTROLLER_PERFECTION_PLAN.md`
