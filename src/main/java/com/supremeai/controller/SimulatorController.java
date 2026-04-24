package com.supremeai.controller;

import com.supremeai.model.UserSimulatorProfile;
import com.supremeai.repository.UserSimulatorProfileRepository;
import com.supremeai.service.SimulatorService;
import com.supremeai.exception.SimulatorConflictException;
import com.supremeai.exception.SimulatorDeploymentException;
import com.supremeai.exception.SimulatorQuotaExceededException;
import com.supremeai.exception.SimulatorResourceNotFoundException;
import com.supremeai.exception.SimulatorSessionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for simulator management.
 *
 * Security: All endpoints require authentication via AuthenticationFilter (Firebase).
 * Users can only access their own profile. Admin endpoints require ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/simulator")
public class SimulatorController {

    private static final Logger logger = LoggerFactory.getLogger(SimulatorController.class);

    @Autowired
    private SimulatorService simulatorService;

    @Autowired
    private UserSimulatorProfileRepository profileRepository;

    // ─────────────────────────────────────────────────────────────────────────────
    // Profile Management
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * GET /api/simulator/profile
     * Get current user's simulator profile (auto-creates if missing)
     */
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserSimulatorProfile> getProfile(Authentication auth) {
        String userId = auth.getName();
        UserSimulatorProfile profile = simulatorService.getProfile(userId).block();
        return ResponseEntity.ok(profile);
    }

    /**
     * POST /api/simulator/profile
     * Update current user's simulator profile (quota, device)
     */
    @PostMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserSimulatorProfile> updateProfile(
            Authentication auth,
            @RequestBody Map<String, Object> updates) {
        String userId = auth.getName();
        
        SimulatorService.UpdateProfileRequest request = new SimulatorService.UpdateProfileRequest();
        
        // Parse installQuota if present
        if (updates.containsKey("installQuota")) {
            Number quota = (Number) updates.get("installQuota");
            request.setInstallQuota(quota.intValue());
        }
        
        // Parse device if present
        if (updates.containsKey("device")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> deviceMap = (Map<String, Object>) updates.get("device");
            SimulatorService.DeviceUpdateRequest deviceReq = new SimulatorService.DeviceUpdateRequest();
            deviceReq.setType((String) deviceMap.get("type"));
            deviceReq.setOsVersion((String) deviceMap.get("osVersion"));
            deviceReq.setScreenResolution((String) deviceMap.get("screenResolution"));
            Object dpiObj = deviceMap.get("densityDpi");
            if (dpiObj instanceof Number dpi) {
                deviceReq.setDensityDpi(dpi.intValue());
            }
            request.setDevice(deviceReq);
        }
        
        UserSimulatorProfile updated = simulatorService.updateProfile(userId, request).block();
        return ResponseEntity.ok(updated);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Installation Management
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * POST /api/simulator/install
     * Install a generated app to simulator
     * Body: { "appId": "abc-123", "deviceProfile": "PIXEL_6" }
     */
    @PostMapping("/install")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> installApp(
            Authentication auth,
            @RequestBody Map<String, String> request) {
        
        String userId = auth.getName();
        String appId = request.get("appId");
        String deviceProfile = request.getOrDefault("deviceProfile", "PIXEL_6");

        if (appId == null || appId.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "appId is required"));
        }

        SimulatorService.SimulatorInstallResult result = 
            simulatorService.installApp(userId, appId, deviceProfile);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("app", Map.of(
            "appId", result.getInstalledApp().getAppId(),
            "appName", result.getInstalledApp().getAppName(),
            "previewUrl", result.getPreviewUrl(),
            "installedAt", result.getInstalledApp().getInstalledAt(),
            "status", result.getInstalledApp().getStatus().name()
        ));
        response.put("quota", Map.of(
            "used", result.getActiveInstalls(),
            "total", result.getInstallQuota()
        ));
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * DELETE /api/simulator/install/{appId}
     * Uninstall an app from simulator
     */
    @DeleteMapping("/install/{appId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> uninstallApp(
            Authentication auth,
            @PathVariable String appId) {
        
        String userId = auth.getName();
        simulatorService.uninstallApp(userId, appId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * GET /api/simulator/installed
     * List all installed apps for authenticated user
     */
    @GetMapping("/installed")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getInstalledApps(Authentication auth) {
        String userId = auth.getName();
        
        UserSimulatorProfile profile = simulatorService.getProfile(userId).block();
        if (profile == null) {
            return ResponseEntity.ok(Map.of(
                "installedApps", java.util.List.of(),
                "quota", Map.of("used", 0, "total", 5)
            ));
        }

        java.util.List<Map<String, Object>> apps = profile.getInstalledApps().stream()
            .map(app -> {
                Map<String, Object> map = new HashMap<>();
                map.put("appId", app.getAppId());
                map.put("appName", app.getAppName());
                map.put("version", app.getVersion());
                map.put("previewUrl", app.getDeployedUrl());
                map.put("installedAt", app.getInstalledAt());
                map.put("launchCount", app.getLaunchCount());
                map.put("lastLaunchedAt", app.getLastLaunchedAt());
                map.put("status", app.getStatus().name());
                return map;
            })
            .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("installedApps", apps);
        response.put("quota", Map.of(
            "used", profile.getActiveInstalls(),
            "total", profile.getInstallQuota()
        ));
        
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Session Management
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * POST /api/simulator/session/start?appId={appId}
     * Start simulator session for an installed app
     */
    @PostMapping("/session/start")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> startSession(
            Authentication auth,
            @RequestParam String appId) {
        
        String userId = auth.getName();
        
        SimulatorService.SessionStartResult result = 
            simulatorService.startSession(userId, appId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", result.getSessionId());
        response.put("websocketUrl", result.getWebsocketUrl());
        response.put("previewUrl", result.getPreviewUrl());
        response.put("state", result.getState());
        response.put("startedAt", result.getStartedAt());
        
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/simulator/session/stop
     * Stop current simulator session
     */
    @PostMapping("/session/stop")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> stopSession(Authentication auth) {
        String userId = auth.getName();
        simulatorService.stopSession(userId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * GET /api/simulator/session/status
     * Get current session status
     */
    @GetMapping("/session/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getSessionStatus(Authentication auth) {
        String userId = auth.getName();
        
        return simulatorService.getSessionStatus(userId)
            .map(status -> {
                Map<String, Object> response = new HashMap<>();
                if (status == null || status.getState() == null || "NONE".equals(status.getState())) {
                    response.put("hasSession", false);
                } else {
                    response.put("hasSession", true);
                    response.put("sessionId", status.getSessionId());
                    response.put("activeAppId", status.getActiveAppId());
                    response.put("state", status.getState());
                    response.put("lastHeartbeat", status.getLastHeartbeat());
                }
                return ResponseEntity.ok(response);
            })
            .onErrorResume(e -> {
                logger.error("Error getting session status", e);
                return Mono.just(ResponseEntity.ok(Map.of("hasSession", false)));
            })
            .block();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Device Management
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * GET /api/simulator/devices
     * List available device profiles
     */
    @GetMapping("/devices")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<java.util.List<Map<String, Object>>> getAvailableDevices() {
        java.util.List<Map<String, Object>> devices = new java.util.ArrayList<>();
        
        for (UserSimulatorProfile.DeviceProfile.DeviceType type : 
             UserSimulatorProfile.DeviceProfile.DeviceType.values()) {
            Map<String, Object> device = new HashMap<>();
            device.put("type", type.name());
            device.put("name", type.getDisplayName());
            device.put("osVersion", type.getOsVersion());
            device.put("screenResolution", type.getResolution());
            device.put("densityDpi", type.getDensityDpi());
            devices.add(device);
        }
        
        return ResponseEntity.ok(devices);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Admin Operations (stubbed - to be implemented)
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * GET /api/simulator/admin/usage
     * Get simulator usage across all users (Admin only)
     */
    @GetMapping("/admin/usage")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.List<Map<String, Object>>> getAllUsage(Authentication auth) {
        // TODO: Implement admin usage aggregation via SimulatorService
        return ResponseEntity.ok(java.util.List.of());
    }

    /**
     * POST /api/simulator/admin/set-quota/{userId}
     * Override user's install quota (Admin only)
     */
    @PostMapping("/admin/set-quota/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserSimulatorProfile> adminSetQuota(
            Authentication auth,
            @PathVariable String userId,
            @RequestParam int quota) {
        
        // Clamp quota to 1-20
        int safeQuota = Math.max(1, Math.min(20, quota));
        
        UserSimulatorProfile profile = simulatorService.getProfile(userId).block();
        if (profile == null) {
            return ResponseEntity.notFound().build();
        }
        
        simulatorService.updateProfile(userId, 
            new SimulatorService.UpdateProfileRequest() {{
                setInstallQuota(safeQuota);
            }}
        ).block();
        
        return ResponseEntity.ok(profile);
    }
}
