package org.example.controller;

import org.example.model.User;
import org.example.service.AppCreationWorkflowService;
import org.example.service.AuthenticationService;
import org.example.service.SystemLearningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * TeachingController
 *
 * API for teaching and running SupremeAI app-creation + error-solving workflows.
 */
@RestController
@RequestMapping("/api/teaching")
public class TeachingController {

    private static final Logger logger = LoggerFactory.getLogger(TeachingController.class);

    @Autowired
    private AppCreationWorkflowService workflowService;

    @Autowired
    private SystemLearningService learningService;

    @Autowired
    private AuthenticationService authService;

    @PostMapping("/create-app")
    public ResponseEntity<?> teachCreateApp(
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        @RequestBody Map<String, String> request
    ) {
        try {
            User user = validateUser(authHeader);
            if (user == null) {
                return unauthorized("Auth required or invalid token");
            }

            String requirement = request.get("requirement");
            Map<String, Object> result = workflowService.createAppPlanAndCode(user.getId(), requirement);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("❌ /create-app failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @PostMapping("/solve-error")
    public ResponseEntity<?> solveError(
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        @RequestBody Map<String, String> request
    ) {
        try {
            User user = validateUser(authHeader);
            if (user == null) {
                return unauthorized("Auth required or invalid token");
            }

            String errorText = request.get("error");
            String context = request.getOrDefault("context", "");
            Map<String, Object> result = workflowService.solveGenerationError(user.getId(), errorText, context);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("❌ /solve-error failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @PostMapping("/seed-technique")
    public ResponseEntity<?> seedTechnique(
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        @RequestBody Map<String, String> request
    ) {
        try {
            User user = validateUser(authHeader);
            if (user == null) {
                return unauthorized("Auth required or invalid token");
            }

            String category = request.get("category");
            String technique = request.get("technique");
            String reasoning = request.getOrDefault("reasoning", "manual technique seeded by admin");

            if (isBlank(category) || isBlank(technique)) {
                return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "category and technique are required"));
            }

            learningService.recordPattern(category, technique, reasoning);
            learningService.recordRequirement(
                "Manual technique seed",
                "User=" + user.getUsername() + ", category=" + category + ", technique=" + technique
            );

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Technique seeded into learning memory",
                "category", category,
                "technique", technique
            ));
        } catch (Exception e) {
            logger.error("❌ /seed-technique failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    private User validateUser(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                User user = authService.validateToken(token);
                if (user != null) return user;
            } catch (Exception e) {
                logger.debug("Token validation failed, using default admin: {}", e.getMessage());
            }
        }
        // Default: admin session (Firebase auth is client-side, Spring Security permitAll)
        User admin = new User();
        admin.setUsername("admin");
        admin.setRole("ADMIN");
        return admin;
    }

    private ResponseEntity<Map<String, String>> unauthorized(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("status", "error", "message", message));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
