package org.example.controller;

import org.example.model.AdminControl;
import org.example.model.PendingAction;
import org.example.model.User;
import org.example.service.AdminControlService;
import org.example.service.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * Admin Control Controller
 * 
 * Endpoints for admin to control system behavior:
 * - GET /api/admin/control - Get current status
 * - POST /api/admin/control/mode - Change permission mode
 * - POST /api/admin/control/stop - Force stop
 * - POST /api/admin/control/resume - Resume operations
 * - GET /api/admin/control/pending - Get pending actions
 * - POST /api/admin/control/pending/{id}/approve - Approve action
 * - POST /api/admin/control/pending/{id}/reject - Reject action
 * - GET /api/admin/control/history - Get action history
 */
@RestController
@RequestMapping("/api/admin/control")
public class AdminControlController {
    private static final Logger logger = LoggerFactory.getLogger(AdminControlController.class);
    
    @Autowired
    private AdminControlService adminControlService;
    
    @Autowired
    private AuthenticationService authService;
    
    /**
     * GET /api/admin/control
     * Get current system control status
     */
    @GetMapping
    public ResponseEntity<?> getStatus(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = extractUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "Admin authentication required"));
            }
            
            Map<String, Object> status = adminControlService.getStatus();
            status.put("requestedBy", user.getUsername());
            status.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", status
            ));
            
        } catch (Exception e) {
            logger.error("❌ Error getting status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
    
    /**
     * POST /api/admin/control/mode
     * Change permission mode (AUTO/WAIT/FORCE_STOP)
     * 
     * Body: {
     *   "mode": "AUTO" | "WAIT" | "FORCE_STOP",
     *   "description": "Why this mode?"
     * }
     */
    @PostMapping("/mode")
    public ResponseEntity<?> changeMode(
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User admin = extractUserFromToken(authHeader);
            if (admin == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "Admin authentication required"));
            }
            
            String modeStr = request.get("mode");
            String description = request.get("description");
            
            if (modeStr == null || modeStr.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "Mode required (AUTO/WAIT/FORCE_STOP)"));
            }
            
            AdminControl.PermissionMode mode = AdminControl.PermissionMode.valueOf(modeStr);
            AdminControl control = adminControlService.setPermissionMode(mode, admin.getUsername(), description);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Permission mode changed to " + mode);
            response.put("mode", mode);
            response.put("isRunning", control.isRunning());
            response.put("canCommit", control.isCanCommit());
            response.put("changedBy", admin.getUsername());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("status", "error", "message", "Invalid mode. Use: AUTO, WAIT, or FORCE_STOP"));
        } catch (Exception e) {
            logger.error("❌ Error changing mode: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
    
    /**
     * POST /api/admin/control/stop
     * Force stop all operations
     */
    @PostMapping("/stop")
    public ResponseEntity<?> forceStop(
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User admin = extractUserFromToken(authHeader);
            if (admin == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "Admin authentication required"));
            }
            
            String reason = request.get("reason");
            AdminControl control = adminControlService.forceStop(admin.getUsername(), reason);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "🛑 System FORCE STOPPED");
            response.put("isRunning", control.isRunning());
            response.put("reason", reason);
            response.put("stoppedBy", admin.getUsername());
            response.put("timestamp", System.currentTimeMillis());
            
            logger.warn("🛑 FORCE STOP executed by {}: {}", admin.getUsername(), reason);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Error forcing stop: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
    
    /**
     * POST /api/admin/control/resume
     * Resume operations
     */
    @PostMapping("/resume")
    public ResponseEntity<?> resume(
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User admin = extractUserFromToken(authHeader);
            if (admin == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "Admin authentication required"));
            }
            
            String modeStr = request.getOrDefault("mode", "WAIT");
            AdminControl.PermissionMode mode = AdminControl.PermissionMode.valueOf(modeStr);
            AdminControl control = adminControlService.resume(admin.getUsername(), mode);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "✅ System resumed with mode: " + mode);
            response.put("mode", mode);
            response.put("isRunning", control.isRunning());
            response.put("resumedBy", admin.getUsername());
            response.put("timestamp", System.currentTimeMillis());
            
            logger.info("✅ System resumed by {}", admin.getUsername());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Error resuming: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
    
    /**
     * GET /api/admin/control/pending
     * Get all pending actions waiting for approval
     */
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingActions(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = extractUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "Admin authentication required"));
            }
            
            List<PendingAction> pending = adminControlService.getPendingActions();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("count", pending.size());
            response.put("actions", pending);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Error getting pending actions: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
    
    /**
     * POST /api/admin/control/pending/{id}/approve
     * Approve a pending action
     */
    @PostMapping("/pending/{id}/approve")
    public ResponseEntity<?> approvePendingAction(
            @PathVariable String id,
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User admin = extractUserFromToken(authHeader);
            if (admin == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "Admin authentication required"));
            }
            
            String reason = request.get("reason");
            PendingAction action = adminControlService.approvePendingAction(id, admin.getUsername(), reason);
            
            if (action == null) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "✅ Action approved and ready to execute");
            response.put("action", action);
            response.put("approvedBy", admin.getUsername());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Error approving action: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
    
    /**
     * POST /api/admin/control/pending/{id}/reject
     * Reject a pending action
     */
    @PostMapping("/pending/{id}/reject")
    public ResponseEntity<?> rejectPendingAction(
            @PathVariable String id,
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User admin = extractUserFromToken(authHeader);
            if (admin == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "Admin authentication required"));
            }
            
            String reason = request.get("reason");
            PendingAction action = adminControlService.rejectPendingAction(id, admin.getUsername(), reason);
            
            if (action == null) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "❌ Action rejected and cancelled");
            response.put("action", action);
            response.put("rejectedBy", admin.getUsername());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Error rejecting action: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
    
    /**
     * GET /api/admin/control/history
     * Get action execution history
     */
    @GetMapping("/history")
    public ResponseEntity<?> getActionHistory(
            @RequestParam(defaultValue = "20") int limit,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = extractUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "Admin authentication required"));
            }
            
            List<PendingAction> history = adminControlService.getActionHistory(limit);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("count", history.size());
            response.put("actions", history);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Error getting history: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
    
    // ============ PRIVATE HELPER METHODS ============
    
    private User extractUserFromToken(String authHeader) throws Exception {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        
        String token = authHeader.substring(7);
        return authService.validateToken(token);
    }
}
