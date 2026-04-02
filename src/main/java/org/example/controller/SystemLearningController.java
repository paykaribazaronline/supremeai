package org.example.controller;

import org.example.model.SystemLearning;
import org.example.service.SystemLearningService;
import org.example.service.KnowledgeReseedService;
import org.example.service.ProviderCoverageService;
import org.example.service.AuthenticationService;
import org.example.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * System Learning Controller
 * Admin views SupremeAI's brain/memory
 */
@RestController
@RequestMapping("/api/learning")
public class SystemLearningController {
    private static final Logger logger = LoggerFactory.getLogger(SystemLearningController.class);
    private static final String BEARER_PREFIX = "Bearer ";
    
    @Autowired
    private SystemLearningService learningService;
    
    @Autowired
    private AuthenticationService authService;

    @Autowired
    private KnowledgeReseedService knowledgeReseedService;

    @Autowired
    private ProviderCoverageService providerCoverageService;
    
    /**
     * GET /api/learning/stats
     * View what system has learned
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = authenticate(authHeader);
            
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error"));
            }
            
            Map<String, Object> stats = learningService.getLearningStats();
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "learning_stats", stats,
                "user", user.getUsername(),
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            logger.error("❌ Stats error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
    
    /**
     * GET /api/learning/critical
     * View critical requirements
     */
    @GetMapping("/critical")
    public ResponseEntity<?> getCriticalRequirements(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = authenticate(authHeader);
            
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            List<SystemLearning> requirements = learningService.getCriticalRequirements();
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "critical_requirements", requirements,
                "count", requirements.size()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * GET /api/learning/solutions/{category}
     * Get solutions for category
     */
    @GetMapping("/solutions/{category}")
    public ResponseEntity<?> getSolutions(
            @PathVariable String category,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = authenticate(authHeader);
            
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            List<String> solutions = learningService.getSolutionsFor(category);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "category", category,
                "solutions", solutions,
                "count", solutions.size()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/learning/techniques
     * View seeded operational techniques.
     */
    @GetMapping({"/techniques", "/techniques/{category}"})
    public ResponseEntity<?> getTechniques(
            @PathVariable(required = false) String category,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = authenticate(authHeader);

            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "Auth required"));
            }

            List<SystemLearning> techniques = learningService.getTechniques(category);

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "category", category == null ? "ALL" : category,
                "techniques", techniques,
                "count", techniques.size(),
                "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            logger.error("❌ Techniques error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * POST /api/learning/reseed
     * Reseed all knowledge from backend seeders.
     */
    @PostMapping("/reseed")
    public ResponseEntity<?> reseedKnowledge(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Setup-Token", required = false) String setupToken) {
        try {
            User user = authenticate(authHeader);
            boolean setupTokenAuthorized = isSetupTokenAuthorized(setupToken);

            if (user == null && !setupTokenAuthorized) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "Auth required"));
            }

            String trigger = user != null ? "admin-dashboard:" + user.getUsername() : "automation:push-workflow";
            Map<String, Object> reseedResult = knowledgeReseedService.reseedAllKnowledge(trigger);
            return ResponseEntity.ok(reseedResult);
        } catch (Exception e) {
            logger.error("❌ Reseed error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @GetMapping("/providers/coverage")
    public ResponseEntity<?> getProviderCoverage(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = authenticate(authHeader);

            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "Auth required"));
            }

            Map<String, Object> coverage = providerCoverageService.getCoverageSummary();
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "coverage", coverage,
                "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            logger.error("❌ Provider coverage error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    private User authenticate(String authHeader) {
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX) || authHeader.length() <= BEARER_PREFIX.length()) {
            return null;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        try {
            return authService.validateToken(token);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isSetupTokenAuthorized(String setupToken) {
        if (setupToken == null || setupToken.isBlank()) {
            return false;
        }
        String expected = System.getenv("SUPREMEAI_SETUP_TOKEN");
        return expected != null && !expected.isBlank() && expected.equals(setupToken);
    }
}
