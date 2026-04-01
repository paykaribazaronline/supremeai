package org.example.controller;

import org.example.model.SystemLearning;
import org.example.service.SystemLearningService;
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
    
    @Autowired
    private SystemLearningService learningService;
    
    @Autowired
    private AuthenticationService authService;
    
    /**
     * GET /api/learning/stats
     * View what system has learned
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "Auth required"));
            }
            
            String token = authHeader.substring(7);
            User user = authService.validateToken(token);
            
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
            if (authHeader == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            String token = authHeader.substring(7);
            User user = authService.validateToken(token);
            
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
            if (authHeader == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            String token = authHeader.substring(7);
            User user = authService.validateToken(token);
            
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
}
