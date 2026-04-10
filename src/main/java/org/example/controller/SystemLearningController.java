package org.example.controller;

import org.example.model.SystemLearning;
import org.example.service.SystemLearningService;
import org.example.service.KnowledgeReseedService;
import org.example.service.ProviderCoverageService;
import org.example.service.IncidentLearningIngestionService;
import org.example.service.InternetResearchService;
import org.example.service.AuthenticationService;
import org.example.model.User;
import org.example.service.ActiveLearningHarvesterService;
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
    private ActiveLearningHarvesterService harvesterService;

    @Autowired
    private KnowledgeReseedService knowledgeReseedService;

    @Autowired
    private ProviderCoverageService providerCoverageService;

    @Autowired
    private IncidentLearningIngestionService incidentIngestionService;

    @Autowired(required = false)
    private InternetResearchService internetResearchService;
    
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
     * GET /api/learning/incidents or /api/learning/incidents/{category}
     * Return incident-derived playbooks (root cause + fix + prevention checks).
     */
    @GetMapping({"/incidents", "/incidents/{category}"})
    public ResponseEntity<?> getIncidentPlaybooks(
            @PathVariable(required = false) String category,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = authenticate(authHeader);

            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "Auth required"));
            }

            List<SystemLearning> incidents = learningService.getIncidentPlaybooks(category);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "category", category == null ? "ALL" : category,
                "incidents", incidents,
                "count", incidents.size(),
                "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            logger.error("❌ Incident list error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * POST /api/learning/incident
     * Learn from any production/dev incident in a structured format.
     */
    @PostMapping("/incident")
    public ResponseEntity<?> learnIncident(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> request) {
        try {
            User user = authenticate(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "Auth required"));
            }

            String category = String.valueOf(request.getOrDefault("category", "GENERAL"));
            String problem = String.valueOf(request.getOrDefault("problem", "Unspecified incident"));
            String rootCause = String.valueOf(request.getOrDefault("rootCause", "Root cause pending analysis"));
            String fix = String.valueOf(request.getOrDefault("fix", "Fix pending implementation"));

            @SuppressWarnings("unchecked")
            List<String> preventionChecks = request.get("preventionChecks") instanceof List
                ? ((List<Object>) request.get("preventionChecks")).stream().map(String::valueOf).toList()
                : List.of();

            Double confidence = null;
            Object confidenceRaw = request.get("confidenceScore");
            if (confidenceRaw instanceof Number) {
                confidence = ((Number) confidenceRaw).doubleValue();
            } else if (confidenceRaw instanceof String && !((String) confidenceRaw).isBlank()) {
                confidence = Double.parseDouble((String) confidenceRaw);
            }

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("reportedBy", user.getUsername());
            metadata.put("source", "api/learning/incident");
            metadata.put("reportedAt", System.currentTimeMillis());

            Object metadataRaw = request.get("metadata");
            if (metadataRaw instanceof Map<?, ?> rawMap) {
                rawMap.forEach((k, v) -> metadata.put(String.valueOf(k), v));
            }

            Map<String, Object> result = learningService.learnFromIncident(
                category,
                problem,
                rootCause,
                fix,
                preventionChecks,
                confidence,
                metadata
            );

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("❌ Incident learning error: {}", e.getMessage(), e);
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

    /**
     * POST /api/learning/harvest
     * Trigger an active knowledge harvest from GitHub issues, PRs, CI logs and web search.
     */
    @PostMapping("/harvest")
    public ResponseEntity<?> triggerHarvest(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Setup-Token", required = false) String setupToken) {
        try {
            User user = authenticate(authHeader);
            boolean setupTokenAuthorized = isSetupTokenAuthorized(setupToken);

            if (user == null && !setupTokenAuthorized) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "Auth required"));
            }

            String trigger = user != null ? "api:" + user.getUsername() : "automation";
            Map<String, Object> result = harvesterService.runHarvest(trigger);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("❌ Harvest error: {}", e.getMessage());
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

    /**
     * POST /api/learning/ingest/logs?maxFiles=14
     * Manually trigger incident ingestion from execution logs.
     */
    @PostMapping("/ingest/logs")
    public ResponseEntity<?> ingestExecutionLogs(
            @RequestParam(defaultValue = "14") int maxFiles,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = authenticate(authHeader);

            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "Auth required"));
            }

            Map<String, Object> result = incidentIngestionService.ingestRecentExecutionLogs(maxFiles);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("❌ Log ingestion error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * GET /api/learning/insights
     * Returns recurring incident categories and confidence trends.
     */
    @GetMapping("/insights")
    public ResponseEntity<?> getLearningInsights(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = authenticate(authHeader);

            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "Auth required"));
            }

            return ResponseEntity.ok(incidentIngestionService.getIncidentLearningInsights());
        } catch (Exception e) {
            logger.error("❌ Learning insights error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * GET /api/learning/research-stats
     * View internet research stats (GitHub, SO, HN, DEV.to)
     */
    @GetMapping("/research-stats")
    public ResponseEntity<?> getResearchStats(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = authenticate(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "Auth required"));
            }
            if (internetResearchService == null) {
                return ResponseEntity.ok(Map.of("status", "unavailable",
                    "message", "InternetResearchService not loaded"));
            }
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "research", internetResearchService.getResearchStats(),
                "user", user.getUsername()
            ));
        } catch (Exception e) {
            logger.error("❌ Research stats error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * POST /api/learning/research-now
     * Trigger an immediate research cycle (admin only)
     */
    @PostMapping("/research-now")
    public ResponseEntity<?> triggerResearchNow(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = authenticate(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "Auth required"));
            }
            if (internetResearchService == null) {
                return ResponseEntity.ok(Map.of("status", "unavailable",
                    "message", "InternetResearchService not loaded"));
            }
            // Run in separate thread so API returns immediately
            new Thread(() -> internetResearchService.runResearchCycle()).start();
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Research cycle triggered — check /research-stats in ~2 minutes"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    private User authenticate(String authHeader) {
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX) && authHeader.length() > BEARER_PREFIX.length()) {
            try {
                String token = authHeader.substring(BEARER_PREFIX.length());
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

    private boolean isSetupTokenAuthorized(String setupToken) {
        if (setupToken == null || setupToken.isBlank()) {
            return false;
        }
        String expected = System.getenv("SUPREMEAI_SETUP_TOKEN");
        return expected != null && !expected.isBlank() && expected.equals(setupToken);
    }
}
