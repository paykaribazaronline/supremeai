# SimulatorController - Perfect Implementation Guide

**Status:** P2 Feature - Supporting Infrastructure  
**Component:** `/api/simulator/*` endpoints  
**Phase:** Phase 6-7 (Visualization & Multi-Platform)  
**Last Updated:** 2026-04-20  

---

## 🎯 Executive Summary

The SimulatorController provides a **cloud-based app preview environment** where users can install, run, and test their generated Android/iOS applications before publishing to app stores. It serves as a critical quality assurance and demo tool in the SupremeAI workflow.

**Current State:** Basic CRUD only (in-memory profile storage)  
**Target State:** Full-featured simulator management with session orchestration, multi-device support, and integration with generated apps

---

## 📊 Current Implementation Analysis

### Existing Components

| File | Lines | Status |
|------|-------|--------|
| `SimulatorController.java` | 43 | Basic CRUD - storage only |
| `UserSimulatorProfile.java` | 21 | Basic model - missing fields |

### Current API Endpoints

| Endpoint | Method | Purpose | Status |
|----------|--------|---------|--------|
| `GET /api/simulator/profile/{userId}` | GET | Get user profile | ✅ Works (in-memory) |
| `POST /api/simulator/profile/{userId}/save` | POST | Save profile | ✅ Works (in-memory) |
| `POST /api/simulator/admin/set-quota/{userId}` | POST | Admin set quota | ✅ Works (in-memory) |

**Major Gaps:**

- ❌ No app installation/uninstallation
- ❌ No simulator session launch/management
- ❌ No Firestore persistence (currently in-memory only)
- ❌ No integration with generated apps
- ❌ No quota validation on install
- ❌ No simulator state/device type specification
- ❌ No audit logging for simulator events
- ❌ No admin analytics/overview
- ❌ No simulator runtime/execution

---

## 🎯 Perfect Implementation Requirements

### 1. Enhanced Data Model

```java
public class UserSimulatorProfile {
    private String userId;
    private int installQuota = 5;
    private int activeInstalls = 0;           // Track active count
    private List<SimulatorApp> installedApps; // Expanded with metadata
    private SimulatorDevice device;           // Device type/configuration
    private String lastState;                 // JSON of simulator session
    private LocalDateTime lastActiveAt;
    private SimulatorSession currentSession;  // Active session if any
    private QuotaHistory[] quotaHistory;      // Usage over time
    
    // Inner classes
    public static class SimulatorApp {
        private String appId;           // Reference to generated app
        private String appName;
        private String version;
        private String deployedUrl;      // Cloud Run URL or similar
        private LocalDateTime installedAt;
        private int launchCount;
        private LocalDateTime lastLaunchedAt;
        private SimulatorAppStatus status; // INSTALLED, RUNNING, ERROR
    }
    
    public static class SimulatorDevice {
        private DeviceType type = DeviceType.PIXEL_6;   // DEFAULT
        private String osVersion = "Android 14";
        private String screenResolution = "1080x2340";
        private int densityDpi = 440;
        private boolean hasGooglePlayServices = false;
        private Map<String, String> customProperties;
    }
    
    public static class SimulatorSession {
        private String sessionId;
        private String activeAppId;
        private String sessionUrl;     // WebSocket URL for streaming
        private LocalDateTime startedAt;
        private LocalDateTime lastHeartbeat;
        private SessionState state;    // ACTIVE, PAUSED, TERMINATED
    }
}
```

### 2. New API Endpoints (Complete Set)

#### A. Installation Management

```http
POST   /api/simulator/install           Install an app to simulator
DELETE /api/simulator/install/{appId}   Uninstall app
GET    /api/simulator/installed         List all installed apps
POST   /api/simulator/install/batch     Bulk install (multiple apps)
```

#### B. Simulator Session Control

```http
POST   /api/simulator/session/start     Start simulator session (launch app)
POST   /api/simulator/session/pause     Pause running session
POST   /api/simulator/session/resume    Resume paused session
POST   /api/simulator/session/stop      Terminate session
GET    /api/simulator/session/status    Get current session state
GET    /api/simulator/session/logs      Get stdout/stderr from simulator
```

#### C. Device Configuration

```http
GET    /api/simulator/devices           List available device profiles
POST   /api/simulator/device/configure  Update device settings
POST   /api/simulator/device/reset      Reset to defaults
```

#### D. Admin Operations

```http
GET    /api/simulator/admin/usage       All users' simulator usage
POST   /api/simulator/admin/force-quota  Override user quota
DELETE /api/simulator/admin/cleanup      Remove stale sessions
GET    /api/simulator/admin/metrics     Prometheus metrics
```

#### E. Webhook Integration

```http
POST   /api/simulator/webhook/event     Internal webhook for app events
```

### 3. Firestore Integration

**Collection:** `simulator_profiles`

**Document Structure:**

```
simulator_profiles/{userId}
├── userId (string)
├── installQuota (int)
├── activeInstalls (int)
├── installedApps (array of SimulatorApp objects)
├── device (SimulatorDevice object)
├── currentSession (SimulatorSession object)
├── lastActiveAt (timestamp)
└── quotaHistory (array of {date, count})

simulator_sessions/{sessionId}
├── sessionId (string)
├── userId (string)
├── appId (string)
├── startedAt (timestamp)
├── lastHeartbeat (timestamp)
└── state (string)

simulator_audit_log/{logId}
├── userId (string)
├── action (string)        // INSTALL, UNINSTALL, LAUNCH, etc.
├── appId (string, optional)
├── timestamp (timestamp)
└── details (map)
```

### 4. Quota Enforcement Logic

```java
@Service
public class SimulatorQuotaService {
    
    /**
     * Check if user can install more apps
     */
    public boolean canInstallApp(String userId, String appId) {
        UserSimulatorProfile profile = repository.findByUserId(userId);
        if (profile == null) return false;
        
        // Check active installs < quota
        if (profile.getActiveInstalls() >= profile.getInstallQuota()) {
            return false;
        }
        
        // Check if app already installed
        if (profile.getInstalledApps().stream()
                .anyMatch(app -> app.getAppId().equals(appId))) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Record installation (increment counters)
     */
    public void recordInstallation(String userId, String appId) { ... }
    
    /**
     * Record uninstallation (decrement counters)
     */
    public void recordUninstallation(String userId, String appId) { ... }
}
```

### 5. Simulator Runtime (Web-based Emulator)

**Technology:** WebAssembly-based Android emulator OR containerized app preview

**Options:**

1. **Web-based APK viewer** - Render APK in browser via WebAssembly (fast, simple)
2. **Cloud-run per-instance** - Each app deployed to separate Cloud Run service with preview URL
3. **Device farm integration** - Integrate with Firebase Test Lab or AWS Device Farm

**Recommended:** Option 2 (simplest & fits existing architecture)

**Implementation:**

- When app installed → deploy to Cloud Run with simulator-specific config
- Return preview URL: `https://{app-id}-simulator.run.app`
- User clicks "Launch" → opens preview in new tab
- SimulatorController tracks session & usage

### 6. Real-time Session Management

```java
@EnableWebSocket
@Component
public class SimulatorWebSocketHandler extends TextWebSocketHandler {
    // Stream simulator output to frontend
    // Handle user interactions (touch, rotate, etc.)
    // Send heartbeats
}
```

### 7. Audit & Monitoring

```java
@Aspect
@Component
public class SimulatorAuditAspect {
    // Log all simulator actions:
    // - INSTALL, UNINSTALL, LAUNCH, STOP, QUOTA_EXCEEDED
    // Store in simulator_audit_log collection
}
```

**Prometheus Metrics:**

- `simulator_installs_total`
- `simulator_sessions_active`
- `simulator_quota_exceeded_total`
- `simulator_launch_duration_seconds`

---

## 🔧 Complete Implementation Checklist

### Phase 1: Core Model & Persistence

- [ ] Expand `UserSimulatorProfile` with complete nested model
- [ ] Create Firestore repository: `SimulatorProfileRepository`
- [ ] Migrate from in-memory map to Firestore
- [ ] Add data validation (Bean Validation annotations)
- [ ] Create DTOs for request/response separation
- [ ] Add Firestore indexing strategy

### Phase 2: Installation Workflow

- [ ] Implement `installApp(userId, appId)` with quota check
- [ ] Implement `uninstallApp(userId, appId)`
- [ ] Add installation validation (app exists, owned by user, compatible)
- [ ] Add event publishing (ApplicationEventPublisher)
- [ ] Create audit log entry for each install/uninstall

### Phase 3: Simulator Runtime Integration

- [ ] Build `SimulatorDeploymentService` - deploys app to preview environment
- [ ] Generate Cloud Run service per installed app (if not existing)
- [ ] Inject simulator telemetry (usage tracking, performance)
- [ ] Create unique preview URL per app install
- [ ] Add TTL auto-cleanup (apps expire after 7 days by default)

### Phase 4: Session Management

- [ ] Implement WebSocket handler for real-time communication
- [ ] Create session lifecycle (start, pause, resume, stop)
- [ ] Add session persistence with cleanup of stale sessions
- [ ] Implement heartbeat mechanism
- [ ] Add session timeout (30 min inactive → auto-terminate)

### Phase 5: Admin Dashboard

- [ ] `GET /api/simulator/admin/usage` - summary across all users
- [ ] `GET /api/simulator/admin/metrics` - Prometheus metrics endpoint
- [ ] Add admin-only method to force-quota override
- [ ] Add admin-only method to kill user sessions
- [ ] Add pagination for large user lists

### Phase 6: Frontend UI

- [ ] Create React component: `SimulatorPanel.tsx`
- [ ] Show installed apps grid with screenshots
- [ ] Add "Launch" button → opens preview in iframe or new tab
- [ ] Show quota badge: "3/5 apps installed"
- [ ] Add device selector dropdown (Pixel 6, iPhone 15, etc.)
- [ ] Add "Uninstall" confirmation modal
- [ ] Add session logs viewer (console output)

### Phase 7: Advanced Features

- [ ] Multi-device support (store separate profiles per device)
- [ ] App version management (roll back to previous version)
- [ ] Automated screenshot capture on install
- [ ] Performance metrics per app (load time, memory usage)
- [ ] Shareable simulator link (temporary public access)
- [ ] Integration with UserApiController (quota billing)

### Phase 8: Resilience & Monitoring

- [ ] Circuit breaker for simulator deployment
- [ ] Retry logic for failed deployments
- [ ] Health check endpoint for simulator service
- [ ] Alert on quota exhaustion spikes
- [ ] Cost tracking per simulator session

---

## 📐 API Specification (Final Design)

### Request/Response Examples

#### Install App

```http
POST /api/simulator/install
Authorization: Bearer {firebaseToken}
Content-Type: application/json

{
  "appId": "generated-app-12345",
  "deviceProfile": "PIXEL_6"
}

→ 201 Created
{
  "success": true,
  "app": {
    "appId": "generated-app-12345",
    "appName": "MyShopApp",
    "installedAt": "2026-04-20T10:30:00Z",
    "previewUrl": "https://app-12345-simulator.run.app",
    "status": "INSTALLED"
  },
  "quota": {
    "used": 2,
    "total": 5
  }
}
```

#### List Installed

```http
GET /api/simulator/installed
Authorization: Bearer {firebaseToken}

→ 200 OK
{
  "installedApps": [
    {
      "appId": "app-123",
      "appName": "MyShop",
      "version": "1.0.0",
      "installedAt": "2026-04-19T14:22:00Z",
      "launchCount": 5,
      "lastLaunchedAt": "2026-04-20T08:12:00Z",
      "status": "INSTALLED",
      "previewUrl": "https://app-123-simulator.run.app"
    }
  ],
  "quota": { "used": 1, "total": 5 }
}
```

#### Start Session

```http
POST /api/simulator/session/start?appId={appId}
Authorization: Bearer {firebaseToken}

→ 200 OK
{
  "sessionId": "sess_abc123",
  "state": "ACTIVE",
  "websocketUrl": "wss://simulator.run.app/ws/sess_abc123",
  "startedAt": "2026-04-20T10:35:00Z"
}
```

---

## 🔐 Security & Access Control

| Role | Permissions |
|------|-------------|
| **User** | Read own profile, install own apps, launch own sessions |
| **Admin** | All user permissions + set any quota, view all users, force cleanup |
| **System** | Internal webhook authentication (service account) |

**Authentication:** Firebase ID token (already in `AuthenticationFilter`)  
**Authorization:** Spring Security `@PreAuthorize` rules

```java
@RestController
@RequestMapping("/api/simulator")
@PreAuthorize("isAuthenticated()")
public class SimulatorController {
    
    @GetMapping("/profile/{userId}")
    @PreAuthorize("authentication.name == #userId or hasRole('ADMIN')")
    public UserSimulatorProfile getProfile(@PathVariable String userId) { ... }
    
    @PostMapping("/install")
    @PreAuthorize("isAuthenticated()")
    public InstallResponse installApp(@RequestBody InstallRequest request, 
                                      Authentication auth) { ... }
}
```

---

## 🗄️ Database Schema (Firestore)

### Collection: `simulator_profiles`

```json
{
  "userId": "firebase-uid-123",
  "installQuota": 5,
  "activeInstalls": 2,
  "installedApps": [
    {
      "appId": "app-generated-001",
      "appName": "MyFirstApp",
      "version": "1.0.0",
      "deployedUrl": "https://app-001-simulator.run.app",
      "installedAt": {"_seconds": 1745180400, "_nanos": 0},
      "launchCount": 3,
      "lastLaunchedAt": {"_seconds": 1745200000, "_nanos": 0},
      "status": "INSTALLED"
    }
  ],
  "device": {
    "type": "PIXEL_6",
    "osVersion": "Android 14",
    "screenResolution": "1080x2340",
    "densityDpi": 440
  },
  "currentSession": {
    "sessionId": "sess_xyz",
    "activeAppId": "app-generated-001",
    "sessionUrl": "wss://simulator.run.app/ws/sess_xyz",
    "startedAt": {"_seconds": 1745200000},
    "state": "ACTIVE"
  },
  "lastActiveAt": {"_seconds": 1745200500},
  "quotaHistory": [
    {"date": "2026-04-01", "count": 1},
    {"date": "2026-04-15", "count": 3}
  ],
  "createdAt": {"_seconds": 1745000000},
  "updatedAt": {"_seconds": 1745200500}
}
```

---

## 🏗️ Service Layer Design

```java
@Service
public class SimulatorService {
    
    @Autowired
    private SimulatorProfileRepository profileRepository;
    
    @Autowired
    private AppDeploymentService deploymentService;
    
    @Autowired
    private SimulatorWebSocketHandler webSocketHandler;
    
    @Autowired
    private AuditLogService auditLogService;
    
    /**
     * Install app to user's simulator
     */
    public SimulatorInstallResult installApp(String userId, String appId, String deviceType) {
        // 1. Load profile
        UserSimulatorProfile profile = profileRepository.findByUserId(userId);
        
        // 2. Check quota
        if (!canInstall(profile, appId)) {
            throw new SimulatorQuotaExceededException(
                "Quota exceeded: " + profile.getActiveInstalls() + "/" + profile.getInstallQuota()
            );
        }
        
        // 3. Validate app exists and belongs to user
        GeneratedApp app = appRepository.findByAppIdAndUserId(appId, userId);
        if (app == null) {
            throw new ResourceNotFoundException("App not found or unauthorized");
        }
        
        // 4. Deploy to preview environment
        String previewUrl = deploymentService.deployToSimulator(app, deviceType);
        
        // 5. Update profile
        SimulatorApp installedApp = new SimulatorApp();
        installedApp.setAppId(appId);
        installedApp.setAppName(app.getName());
        installedApp.setVersion(app.getVersion());
        installedApp.setDeployedUrl(previewUrl);
        installedApp.setInstalledAt(LocalDateTime.now());
        installedApp.setStatus(SimulatorAppStatus.INSTALLED);
        
        profile.getInstalledApps().add(installedApp);
        profile.setActiveInstalls(profile.getActiveInstalls() + 1);
        profile.setLastActiveAt(LocalDateTime.now());
        profileRepository.save(profile);
        
        // 6. Log audit event
        auditLogService.log("APP_INSTALL", userId, Map.of(
            "appId", appId,
            "device", deviceType,
            "previewUrl", previewUrl
        ));
        
        return new SimulatorInstallResult(profile, installedApp);
    }
}
```

---

## 🧪 Testing Strategy

### Unit Tests

```java
@SpringBootTest
class SimulatorServiceTest {
    
    @Test
    void testInstallApp_WithinQuota() {
        // Given: user has quota available
        UserSimulatorProfile profile = new UserSimulatorProfile();
        profile.setInstallQuota(5);
        profile.setActiveInstalls(2);
        
        // When: install app
        SimulatorInstallResult result = service.installApp(profile, appId);
        
        // Then: success
        assertTrue(result.isSuccess());
        assertEquals(3, profile.getActiveInstalls());
    }
    
    @Test
    void testInstallApp_ExceedsQuota() {
        // Given: user at quota limit
        UserSimulatorProfile profile = new UserSimulatorProfile();
        profile.setInstallQuota(2);
        profile.setActiveInstalls(2);
        
        // When/Then: throws exception
        assertThrows(SimulatorQuotaExceededException.class, () -> {
            service.installApp(profile, appId);
        });
    }
}
```

### Integration Tests

```java
@AutoConfigureMockMvc
class SimulatorControllerIntegrationTest {
    
    @Test
    void testInstallApp_EndToEnd() throws Exception {
        // Authenticate as user
        mockMvc.perform(post("/api/simulator/install")
            .header("Authorization", "Bearer " + validToken)
            .content("{\"appId\":\"app-123\",\"deviceProfile\":\"PIXEL_6\"}")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true));
    }
}
```

### Load Tests

```java
public class SimulatorLoadTest extends LoadTestingSuite {
    // Simulate 100 concurrent users installing apps
    // Measure quota enforcement latency
    // Verify no race conditions on quota counters
}
```

---

## 📊 Monitoring & Metrics

**Prometheus Metrics:**

```java
@Component
public class SimulatorMetrics {
    
    Counter installCounter = Counter.build()
        .name("simulator_installs_total")
        .help("Total apps installed to simulator")
        .labelNames("userId", "appType")
        .register();
    
    Gauge activeSessionsGauge = Gauge.build()
        .name("simulator_active_sessions")
        .help("Currently active simulator sessions")
        .register();
    
    Histogram installDuration = Histogram.build()
        .name("simulator_install_duration_seconds")
        .help("Time to install app to simulator")
        .buckets(0.1, 0.5, 1.0, 2.0, 5.0)
        .register();
}
```

**Logging:**

- ERROR: Failed deployments, quota exceeded (with userId)
- WARN: Session timeout, stale session cleanup
- INFO: Install/uninstall events, session start/stop
- DEBUG: Detailed deployment steps, WebSocket messages

---

## 🚀 Deployment Configuration

### application.properties additions

```properties
# Simulator settings
simulator.max.installs.per.user=${SIMULATOR_MAX_INSTALLS:5}
simulator.session.timeout.minutes=${SIMULATOR_SESSION_TIMEOUT:30}
simulator.deployment.timeout.seconds=${SIMULATOR_DEPLOY_TIMEOUT:120}
simulator.auto.cleanup.enabled=${SIMULATOR_AUTO_CLEANUP:true}
simulator.app.expiry.days=${SIMULATOR_APP_EXPIRY_DAYS:7}

# Preview URL format
simulator.preview.url.format=https://{appId}-simulator.{domain}

# Cloud Run settings for simulator deployments
simulator.cloudrun.region=${SIMULATOR_REGION:us-central1}
simulator.cloudrun.memory=${SIMULATOR_MEMORY:2Gi}
simulator.cloudrun.cpu=${SIMULATOR_CPU:1}
```

---

## 🎓 User Guide (For End Users)

### Getting Started

1. **Access Simulator**
   - Navigate to Dashboard → My Simulator
   - Your profile loads automatically

2. **Check Quota**
   - See "X/5 Apps Installed" at top
   - Admin can increase quota on request

3. **Install an App**
   - From your Projects list, click "..." → "Install to Simulator"
   - Or from Simulator page, click "Add App" and select
   - Wait ~30 seconds for deployment

4. **Launch Simulator**
   - Click "Launch" button on installed app card
   - Simulator opens in new browser tab
   - Interact with your app in full-screen device frame

5. **Manage Device**
   - Change device type: dropdown on profile page
   - Reset device: "Factory Reset" button (clears data)

6. **Uninstall**
   - Click trash icon on app card
   - Confirms → app removed, quota freed

### Tips

- **Best for quick testing** before generating APK
- **Screenshots** automatically captured on install
- **Session logs** available under "Advanced" tab
- **Quota resets** monthly on billing cycle date

---

## 📋 Migration Plan (From Current to Perfect)

### Sprint 1-2: Persistence & Installation (Weeks 1-2)

- [ ] Create Firestore repository
- [ ] Migrate all endpoints to use Firestore
- [ ] Implement install/uninstall endpoints
- [ ] Add quota validation
- [ ] Create integration tests

### Sprint 3-4: Deployment & Sessions (Weeks 3-4)

- [ ] Build `SimulatorDeploymentService`
- [ ] Implement Cloud Run preview deployment
- [ ] Create WebSocket handler
- [ ] Add session management
- [ ] Build basic React UI panel

### Sprint 5-6: Admin & Monitoring (Weeks 5-6)

- [ ] Admin endpoints (usage, metrics)
- [ ] Audit logging aspect
- [ ] Prometheus metrics
- [ ] Dashboard admin panel
- [ ] Documentation & user guide

### Sprint 7+: Polish (Weeks 7+)

- [ ] Multi-device support
- [ ] App version rollback
- [ ] Shareable links
- [ ] Cost tracking integration
- [ ] Performance benchmarking

---

## ⚠️ Potential Pitfalls & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Cloud Run cost explosion | High | Auto-cleanup after 7 days; quota limits; budget alerts |
| Concurrent install race conditions | Medium | Synchronized quota checks via Firestore transactions |
| Simulator session resource leaks | Medium | Heartbeat + auto-timeout; cleanup daemon |
| Large apps deployment timeout | Medium | Async deployment + status polling |
| User confusion between simulator vs generated APK | Low | Clear UI labels; tooltips; separate sections |
| Frontend complexity (iframe embedding) | Medium | Use new tab for simulator; simpler first iteration |
| Device type compatibility issues | Low | Test matrix; default to Pixel 6 |

---

## 🎯 Success Criteria

### MVP Definition (Sprint 2 Complete)

- [ ] User can install up to quota limit of generated apps
- [ ] Installed apps launch in browser-based preview (new tab)
- [ ] Quota enforced across concurrent requests
- [ ] All data persisted to Firestore
- [ ] Basic audit logs captured
- [ ] React UI panel shows installed apps with Launch button
- [ ] Admin can view all simulator usage
- [ ] TTL auto-cleanup removes apps older than 7 days

### Perfect Definition (Sprint 6 Complete)

- [ ] All "Perfect Implementation" items above completed
- [ ] Zero in-memory state (fully persisted)
- [ ] < 2 second install latency (p99)
- [ ] Session recovery after network interruption
- [ ] Multi-device profiles saved and selectable
- [ ] Full admin analytics dashboard with charts
- [ ] Cost per simulator session tracked < $0.01
- [ ] User satisfaction > 4.5/5 on simulator feature
- [ ] Zero data loss on server restart

---

## 📚 Related Documentation

- `docs_new/architecture/02-ARCHITECTURE/PROVIDER_QUICK_REFERENCE.md` - Provider management
- `docs_new/guides/MASTER_ROADMAP_INTEGRATED_2026.md` - Implementation timeline
- `SUPREMEAI_ENHANCEMENT_ROADMAP.md` - UX enhancements priority
- Firebase collections setup: `scripts/setup-firestore-collections.js`

---

## 🏁 Conclusion

The SimulatorController is currently a minimal stub with ~64 lines of code. To make it **production-perfect**, implement the phased approach above:

1. **Persistence first** (replace in-memory map)
2. **Installation workflow** (core value)
3. **Runtime deployment** (actual simulator)
4. **Admin controls** (management)
5. **Polish features** (multi-device, metrics)

**Estimated Effort:**  
MVP: 2-3 sprints (2 weeks each) → 4-6 weeks total  
Perfect: 6-8 sprints → 12-16 weeks total

**Dependencies:**  

- Code generation must produce deployable artifacts  
- Cloud Run deployments must be automated  
- Frontend team to build UI panel  

**Value:** Users can test generated apps in a live environment **before** downloading APK → reduces support issues and increases confidence in generated code.
