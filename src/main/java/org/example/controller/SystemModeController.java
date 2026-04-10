package org.example.controller;

import org.example.model.SystemMode;
import org.example.service.SystemModeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * System Mode Controller
 * REST API for managing SupremeAI's 3-mode system
 */
@RestController
@RequestMapping("/api/system-mode")
@CrossOrigin(origins = "*")
public class SystemModeController {

    @Autowired
    private SystemModeService systemModeService;

    /**
     * Get current system mode
     * GET /api/system-mode
     */
    @GetMapping
    public ResponseEntity<?> getCurrentMode() {
        try {
            SystemMode currentMode = systemModeService.getCurrentMode();
            return ResponseEntity.ok(Map.of(
                "mode", currentMode.name(),
                "displayName", currentMode.getDisplayName(),
                "icon", currentMode.getIcon(),
                "description", currentMode.getDescription()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get complete mode status
     * GET /api/system-mode/status
     */
    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        try {
            return ResponseEntity.ok(systemModeService.getModeStatus());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * List all available modes
     * GET /api/system-mode/available
     */
    @GetMapping("/available")
    public ResponseEntity<?> getAvailableModes() {
        try {
            List<Map<String, Object>> modes = new ArrayList<>();
            for (SystemMode mode : SystemMode.values()) {
                modes.add(Map.of(
                    "name", mode.name(),
                    "displayName", mode.getDisplayName(),
                    "icon", mode.getIcon(),
                    "description", mode.getDescription()
                ));
            }
            return ResponseEntity.ok(Map.of("modes", modes));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Change system mode
     * POST /api/system-mode/set
     * Body: { "mode": "FULLY_AUTOMATIC" | "PRESET_RULES" | "MANUAL_ONLY", "adminName": "Admin@SupremeAI" }
     */
    @PostMapping("/set")
    public ResponseEntity<?> setMode(@RequestBody Map<String, String> request) {
        try {
            String modeStr = request.get("mode");
            String adminName = request.get("adminName");

            if (modeStr == null || adminName == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Missing 'mode' or 'adminName' in request"
                ));
            }

            SystemMode newMode = SystemMode.valueOf(modeStr.toUpperCase());
            systemModeService.setSystemMode(newMode, adminName);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "System mode changed to: " + newMode.getDisplayName(),
                "newMode", newMode.name(),
                "icon", newMode.getIcon()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid mode. Use: FULLY_AUTOMATIC, PRESET_RULES, or MANUAL_ONLY"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Check if operation can be executed
     * POST /api/system-mode/check-operation
     * Body: { "operation": "LEARN_FROM_ERRORS", "confidence": 95 }
     */
    @PostMapping("/check-operation")
    public ResponseEntity<?> checkOperation(@RequestBody Map<String, Object> request) {
        try {
            String operation = (String) request.get("operation");
            int confidence = ((Number) request.get("confidence")).intValue();

            SystemModeService.OperationDecision decision = 
                systemModeService.canExecuteOperation(operation, confidence);

            return ResponseEntity.ok(Map.of(
                "operationName", decision.getOperationName(),
                "mode", decision.getMode().name(),
                "allowed", decision.isAllowed(),
                "reason", decision.getReason(),
                "confidence", decision.getConfidence(),
                "requiresApproval", decision.isRequiresApproval()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Set allowed operations for PRESET_RULES mode
     * POST /api/system-mode/allowed-operations
     */
    @PostMapping("/allowed-operations")
    public ResponseEntity<?> setAllowedOperations(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> operations = (List<String>) request.get("operations");
            String adminName = (String) request.get("adminName");

            systemModeService.setAllowedOperations(operations, adminName);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Allowed operations updated",
                "count", operations.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Set autonomy level for FULLY_AUTOMATIC mode
     * POST /api/system-mode/autonomy
     * Body: { "level": 75, "adminName": "Admin@SupremeAI" }
     */
    @PostMapping("/autonomy")
    public ResponseEntity<?> setAutonomy(@RequestBody Map<String, Object> request) {
        try {
            int level = ((Number) request.get("level")).intValue();
            String adminName = (String) request.get("adminName");

            systemModeService.setAutonomyLevel(level, adminName);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Autonomy level set to: " + level + "%",
                "level", level
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get pending approvals (MANUAL_ONLY mode)
     * GET /api/system-mode/pending-approvals
     */
    @GetMapping("/pending-approvals")
    public ResponseEntity<?> getPendingApprovals() {
        try {
            return ResponseEntity.ok(Map.of(
                "pending", systemModeService.getPendingApprovals(),
                "count", systemModeService.getPendingApprovals().size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Approve an operation (MANUAL_ONLY mode)
     * POST /api/system-mode/approve
     * Body: { "operationId": "op-123", "adminName": "Admin@SupremeAI" }
     */
    @PostMapping("/approve")
    public ResponseEntity<?> approveOperation(@RequestBody Map<String, String> request) {
        try {
            String operationId = request.get("operationId");
            String adminName = request.get("adminName");

            systemModeService.approveOperation(operationId, adminName);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Operation approved: " + operationId
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Request approval for an operation
     * POST /api/system-mode/request-approval
     * Body: { "operationId": "op-123", "description": "Learn from error patterns" }
     */
    @PostMapping("/request-approval")
    public ResponseEntity<?> requestApproval(@RequestBody Map<String, String> request) {
        try {
            String operationId = request.get("operationId");
            String description = request.get("description");

            systemModeService.requestApproval(operationId, description);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Approval requested for: " + description
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get mode change history
     * GET /api/system-mode/history
     */
    @GetMapping("/history")
    public ResponseEntity<?> getHistory() {
        try {
            return ResponseEntity.ok(Map.of(
                "history", systemModeService.getModeChangeHistory()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
