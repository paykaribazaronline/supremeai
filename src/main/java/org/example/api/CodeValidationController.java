package org.example.api;

import org.example.service.CodeValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * CodeValidationController: REST API for code quality validation
 * 
 * Endpoints expose validation capabilities for all 5 supported frameworks:
 * - Compilation error detection
 * - Code linting and style checking
 * - Framework-specific anti-pattern detection
 * - Build readiness assessment
 */
@RestController
@RequestMapping("/api/validation")
@CrossOrigin(origins = "*")
public class CodeValidationController {

    @Autowired
    private CodeValidationService validationService;

    /**
     * Validate a project for compilation and code quality
     * 
     * POST /api/validation/validate
     * Request: { "projectId": "proj-123", "templateType": "REACT" }
     * Response: { 
     *   "projectId": "proj-123",
     *   "isValid": true,
     *   "totalIssues": 0,
     *   "criticalCount": 0,
     *   "errorCount": 0,
     *   "warningCount": 0,
     *   "validationScore": 100,
     *   "issues": []
     * }
     */
    @PostMapping("/validate")
    public Map<String, Object> validateProject(
            @RequestBody Map<String, String> request) {
        
        String projectId = request.get("projectId");
        String templateType = request.get("templateType");
        
        if (projectId == null || projectId.trim().isEmpty()) {
            return buildErrorResponse("projectId", "projectId is required");
        }
        if (templateType == null || templateType.trim().isEmpty()) {
            return buildErrorResponse("templateType", "templateType is required");
        }

        return validationService.validateProject(projectId, templateType);
    }

    /**
     * Validate multiple projects in batch
     * 
     * POST /api/validation/batch-validate
     * Request: { 
     *   "projectIds": ["proj-1", "proj-2"],
     *   "templateType": "NODEJS"
     * }
     * Response: { 
     *   "results": [ { projectId, isValid, issues... }, ... ],
     *   "batchSummary": { passed, failed, totalIssues }
     * }
     */
    @PostMapping("/batch-validate")
    public Map<String, Object> validateBatch(
            @RequestBody Map<String, Object> request) {
        
        @SuppressWarnings("unchecked")
        List<String> projectIds = (List<String>) request.get("projectIds");
        String templateType = (String) request.get("templateType");
        
        if (projectIds == null || projectIds.isEmpty()) {
            return buildErrorResponse("projectIds", "projectIds array is required and non-empty");
        }
        if (templateType == null || templateType.trim().isEmpty()) {
            return buildErrorResponse("templateType", "templateType is required");
        }

        List<Map<String, Object>> results = new ArrayList<>();
        int passedCount = 0;
        long totalIssues = 0;

        for (String projectId : projectIds) {
            Map<String, Object> validation = validationService.validateProject(projectId, templateType);
            results.add(validation);
            
            if ((Boolean) validation.get("isValid")) {
                passedCount++;
            }
            totalIssues += (Long) validation.get("totalIssues");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("results", results);
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalProjects", projectIds.size());
        summary.put("passedProjects", passedCount);
        summary.put("failedProjects", projectIds.size() - passedCount);
        summary.put("successRate", projectIds.size() > 0 ? (passedCount * 100 / projectIds.size()) : 0);
        summary.put("totalIssues", totalIssues);
        response.put("batchSummary", summary);
        response.put("timestamp", System.currentTimeMillis());

        return response;
    }

    /**
     * Check if a specific project is ready for deployment
     * 
     * GET /api/validation/readiness/{projectId}?templateType=REACT
     * Response: {
     *   "projectId": "proj-123",
     *   "isReadyForDeployment": true,
     *   "readinessScore": 95,
     *   "blockers": [],
     *   "recommendations": ["Add error handling in route handlers"]
     * }
     */
    @GetMapping("/readiness/{projectId}")
    public Map<String, Object> getDeploymentReadiness(
            @PathVariable String projectId,
            @RequestParam String templateType) {
        
        Map<String, Object> validation = validationService.validateProject(projectId, templateType);
        
        boolean isValid = (Boolean) validation.get("isValid");
        int score = (Integer) validation.get("validationScore");
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> issues = (List<Map<String, Object>>) validation.get("issues");
        
        List<String> blockers = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        
        for (Map<String, Object> issue : issues) {
            String severity = (String) issue.get("severity");
            String message = (String) issue.get("message");
            
            if ("CRITICAL".equals(severity) || "ERROR".equals(severity)) {
                blockers.add(message);
            } else if ("WARNING".equals(severity)) {
                recommendations.add(message);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("projectId", projectId);
        response.put("templateType", templateType);
        response.put("isReadyForDeployment", blockers.isEmpty());
        response.put("readinessScore", score);
        response.put("blockers", blockers);
        response.put("recommendations", recommendations);
        response.put("totalIssues", validation.get("totalIssues"));
        response.put("timestamp", System.currentTimeMillis());

        return response;
    }

    /**
     * Get detailed validation report with categorized issues
     * 
     * GET /api/validation/report/{projectId}?templateType=NODEJS
     * Response: {
     *   "projectId": "proj-123",
     *   "summary": { isValid, totalIssues, ... },
     *   "issuesByCategory": {
     *     "MISSING_DEPENDENCY": [ { message, file, ... } ],
     *     "SYNTAX_ERROR": [ ... ]
     *   },
     *   "issuesBySeverity": {
     *     "CRITICAL": [ ... ],
     *     "ERROR": [ ... ],
     *     ...
     *   }
     * }
     */
    @GetMapping("/report/{projectId}")
    public Map<String, Object> getValidationReport(
            @PathVariable String projectId,
            @RequestParam String templateType) {
        
        Map<String, Object> validation = validationService.validateProject(projectId, templateType);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> issues = (List<Map<String, Object>>) validation.get("issues");
        
        // Group by category (error code)
        Map<String, List<Map<String, Object>>> byCategory = new HashMap<>();
        Map<String, List<Map<String, Object>>> bySeverity = new HashMap<>();
        
        for (Map<String, Object> issue : issues) {
            String code = (String) issue.get("code");
            String severity = (String) issue.get("severity");
            
            byCategory.computeIfAbsent(code, k -> new ArrayList<>()).add(issue);
            bySeverity.computeIfAbsent(severity, k -> new ArrayList<>()).add(issue);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("projectId", projectId);
        response.put("templateType", templateType);
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("isValid", validation.get("isValid"));
        summary.put("totalIssues", validation.get("totalIssues"));
        summary.put("criticalCount", validation.get("criticalCount"));
        summary.put("errorCount", validation.get("errorCount"));
        summary.put("warningCount", validation.get("warningCount"));
        summary.put("validationScore", validation.get("validationScore"));
        response.put("summary", summary);
        
        response.put("issuesByCategory", byCategory);
        response.put("issuesBySeverity", bySeverity);
        response.put("timestamp", System.currentTimeMillis());

        return response;
    }

    /**
     * Compare validation results between two projects (for improvement tracking)
     * 
     * GET /api/validation/compare?projectId1=proj-1&projectId2=proj-2&templateType=REACT
     * Response: {
     *   "project1": { validation results },
     *   "project2": { validation results },
     *   "improvement": {
     *     "scoreChange": 15,
     *     "issueReduction": 3,
     *     "isImproved": true
     *   }
     * }
     */
    @GetMapping("/compare")
    public Map<String, Object> compareProjects(
            @RequestParam String projectId1,
            @RequestParam String projectId2,
            @RequestParam String templateType) {
        
        Map<String, Object> val1 = validationService.validateProject(projectId1, templateType);
        Map<String, Object> val2 = validationService.validateProject(projectId2, templateType);
        
        int score1 = (Integer) val1.get("validationScore");
        int score2 = (Integer) val2.get("validationScore");
        long issues1 = (Long) val1.get("totalIssues");
        long issues2 = (Long) val2.get("totalIssues");
        
        Map<String, Object> response = new HashMap<>();
        response.put("project1", val1);
        response.put("project2", val2);
        
        Map<String, Object> improvement = new HashMap<>();
        improvement.put("scoreChange", score2 - score1);
        improvement.put("issueReduction", issues1 - issues2);
        improvement.put("isImproved", score2 > score1 && issues2 < issues1);
        response.put("comparison", improvement);
        response.put("timestamp", System.currentTimeMillis());

        return response;
    }

    /**
     * Get validation statistics across supported frameworks
     * 
     * GET /api/validation/framework-stats?templateTypes=REACT,NODEJS,JAVA
     * Response: {
     *   "REACT": { avgScore, errorCount, ... },
     *   "NODEJS": { ... },
     *   "framework_summary": { totalProjects, avgScore, ... }
     * }
     */
    @GetMapping("/framework-stats")
    public Map<String, Object> getFrameworkStats(
            @RequestParam(required = false) String templateTypes) {
        
        Map<String, Object> response = new HashMap<>();
        
        // Get supported frameworks
        List<String> frameworks = new ArrayList<>(Arrays.asList(
                "REACT", "NODEJS", "FLUTTER", "PYTHON", "JAVA"
        ));
        
        if (templateTypes != null && !templateTypes.isEmpty()) {
            frameworks = Arrays.asList(templateTypes.split(","));
        }

        for (String framework : frameworks) {
            // Would aggregate validation data from all projects using this framework
            // For now, provide structure for future enhancement
            Map<String, Object> stats = new HashMap<>();
            stats.put("framework", framework);
            stats.put("supportedVersion", getFrameworkVersion(framework));
            stats.put("commonIssues", getCommonIssuesForFramework(framework));
            response.put(framework, stats);
        }

        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * Get validation configuration (supported frameworks, rules, etc.)
     * 
     * GET /api/validation/config
     */
    @GetMapping("/config")
    public Map<String, Object> getValidationConfig() {
        Map<String, Object> response = new HashMap<>();
        
        List<String> frameworks = Arrays.asList("REACT", "NODEJS", "FLUTTER", "PYTHON", "JAVA");
        Map<String, Object> rules = new HashMap<>();
        
        rules.put("COMPILATION_REQUIRED", true);
        rules.put("CHECK_DEPENDENCIES", true);
        rules.put("CHECK_SYNTAX", true);
        rules.put("CHECK_FRAMEWORKS", true);
        rules.put("CHECK_IMPORTS_EXPORTS", true);
        rules.put("STRICT_MODE", false);
        
        response.put("supportedFrameworks", frameworks);
        response.put("severityLevels", Arrays.asList("CRITICAL", "ERROR", "WARNING", "INFO"));
        response.put("validationRules", rules);
        response.put("errorCategories", Arrays.asList(
                "MISSING_DEPENDENCY", "SYNTAX_ERROR", "UNMATCHED_BRACES",
                "MISSING_IMPORTS", "MISSING_EXPORTS", "TYPE_ERROR",
                "MISSING_MAIN", "MISSING_VERSION", "INVALID_CONFIG"
        ));
        
        return response;
    }

    /**
     * Health check for validation service
     * 
     * GET /api/validation/health
     */
    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "CodeValidationService");
        response.put("status", "running");
        response.put("supportedFrameworks", 5);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    // ==================== Helper Methods ====================

    private Map<String, Object> buildErrorResponse(String field, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("field", field);
        error.put("message", message);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }

    private String getFrameworkVersion(String framework) {
        return switch (framework) {
            case "REACT" -> "18.x + TypeScript 5.x";
            case "NODEJS" -> "18.x + Express 4.x";
            case "FLUTTER" -> "3.x + Dart 3.x";
            case "PYTHON" -> "3.10+ + FastAPI";
            case "JAVA" -> "17+ + Spring Boot 3.x";
            default -> "Unknown";
        };
    }

    @SuppressWarnings("unchecked")
    private List<String> getCommonIssuesForFramework(String framework) {
        return switch (framework) {
            case "REACT" -> Arrays.asList(
                    "Missing React imports for hooks",
                    "Improper component export",
                    "Missing TypeScript prop types",
                    "Unsafe type assertions"
            );
            case "NODEJS" -> Arrays.asList(
                    "Missing express import",
                    "No route handlers defined",
                    "Missing environment variables",
                    "Unhandled promise rejections"
            );
            case "FLUTTER" -> Arrays.asList(
                    "Missing main() function",
                    "Invalid widget hierarchy",
                    "Missing pubspec dependencies",
                    "Improper async/await usage"
            );
            case "PYTHON" -> Arrays.asList(
                    "Indentation errors",
                    "Missing colons after control structures",
                    "Import order violations",
                    "Missing requirements.txt entries"
            );
            case "JAVA" -> Arrays.asList(
                    "Unmatched braces",
                    "Missing Spring Boot annotations",
                    "Improper dependency injection",
                    "Missing pom.xml configuration"
            );
            default -> new ArrayList<>();
        };
    }
}
