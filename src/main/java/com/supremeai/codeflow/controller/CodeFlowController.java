package com.supremeai.codeflow.controller;

import com.supremeai.codeflow.model.CodeRepository;
import com.supremeai.codeflow.service.CodeFlowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API Controller for CodeFlow module
 * Provides endpoints for code analysis, visualization, and error resolution
 */
@RestController
@RequestMapping("/api/codeflow")
public class CodeFlowController {
    
    private static final Logger logger = LoggerFactory.getLogger(CodeFlowController.class);
    
    private final CodeFlowService codeFlowService;

    public CodeFlowController(CodeFlowService codeFlowService) {
        this.codeFlowService = codeFlowService;
    }
    
    /**
     * Analyze a repository
     * POST /api/codeflow/analyze
     */
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeRepository(
            @RequestBody AnalysisRequest request) {
        
        logger.info("Received analysis request for repository: {}", request.getRepoUrl());
        
        try {
            CodeRepository result = codeFlowService.analyzeRepository(
                request.getRepoUrl(),
                request.getSourceType(),
                request.getOwnerId()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Analysis started successfully");
            response.put("repositoryId", result.getId());
            response.put("status", result.getAnalysisStatus());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Analysis failed", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Analysis failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Get analysis results
     * GET /api/codeflow/analysis/{repositoryId}
     */
    @GetMapping("/analysis/{repositoryId}")
    public ResponseEntity<Map<String, Object>> getAnalysis(@PathVariable String repositoryId) {
        
        try {
            CodeRepository result = codeFlowService.getAnalysis(repositoryId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", result);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get analysis", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to retrieve analysis: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
    
    /**
     * Get analyses for owner
     * GET /api/codeflow/owner/{ownerId}
     */
    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<Map<String, Object>> getAnalysesForOwner(@PathVariable String ownerId) {
        
        try {
            List<CodeRepository> results = codeFlowService.getAnalysesForOwner(ownerId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", results);
            response.put("count", results.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get analyses", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to retrieve analyses: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Analyze pull request impact
     * POST /api/codeflow/pr/analyze
     */
    @PostMapping("/pr/analyze")
    public ResponseEntity<Map<String, Object>> analyzePullRequest(
            @RequestBody PullRequestRequest request) {
        
        logger.info("Analyzing PR impact for repository: {}", request.getRepositoryId());
        
        try {
            CodeRepository.PullRequestAnalysis analysis = codeFlowService.analyzePullRequest(
                request.getRepositoryId(),
                request.getChangedFiles()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", analysis);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("PR analysis failed", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "PR analysis failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Get health score
     * GET /api/codeflow/health/{repositoryId}
     */
    @GetMapping("/health/{repositoryId}")
    public ResponseEntity<Map<String, Object>> getHealthScore(@PathVariable String repositoryId) {
        
        try {
            CodeRepository repo = codeFlowService.getAnalysis(repositoryId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("score", repo.getHealthScore());
            response.put("grade", repo.getHealthGrade());
            response.put("issues", repo.getHealthIssues());
            response.put("securityIssues", repo.getSecurityIssues());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get health score", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to retrieve health score: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
    
    /**
     * Get dependency graph
     * GET /api/codeflow/graph/{repositoryId}
     */
    @GetMapping("/graph/{repositoryId}")
    public ResponseEntity<Map<String, Object>> getDependencyGraph(@PathVariable String repositoryId) {
        
        try {
            CodeRepository repo = codeFlowService.getAnalysis(repositoryId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("graph", repo.getDependencyGraph());
            response.put("circularDependencies", repo.getCircularDependencies());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get dependency graph", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to retrieve graph: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
    
    /**
     * Get security issues
     * GET /api/codeflow/security/{repositoryId}
     */
    @GetMapping("/security/{repositoryId}")
    public ResponseEntity<Map<String, Object>> getSecurityIssues(@PathVariable String repositoryId) {
        
        try {
            CodeRepository repo = codeFlowService.getAnalysis(repositoryId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("issues", repo.getSecurityIssues());
            
            // Group by severity
            Map<String, Long> bySeverity = repo.getSecurityIssues().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    CodeRepository.SecurityIssue::getSeverity,
                    java.util.stream.Collectors.counting()
                ));
            response.put("bySeverity", bySeverity);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get security issues", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to retrieve security issues: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
    
    /**
     * Get AI suggestions
     * GET /api/codeflow/suggestions/{repositoryId}
     */
    @GetMapping("/suggestions/{repositoryId}")
    public ResponseEntity<Map<String, Object>> getSuggestions(@PathVariable String repositoryId) {
        
        try {
            CodeRepository repo = codeFlowService.getAnalysis(repositoryId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("suggestions", repo.getAiSuggestions());
            response.put("errorAnalyses", repo.getErrorAnalyses());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get suggestions", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to retrieve suggestions: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
    
    /**
     * Get dead code report
     * GET /api/codeflow/deadcode/{repositoryId}
     */
    @GetMapping("/deadcode/{repositoryId}")
    public ResponseEntity<Map<String, Object>> getDeadCode(@PathVariable String repositoryId) {
        
        try {
            CodeRepository repo = codeFlowService.getAnalysis(repositoryId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("deadCode", repo.getDeadCode());
            response.put("count", repo.getDeadCode().size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get dead code", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to retrieve dead code: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
    
    /**
     * Get pattern detection results
     * GET /api/codeflow/patterns/{repositoryId}
     */
    @GetMapping("/patterns/{repositoryId}")
    public ResponseEntity<Map<String, Object>> getPatterns(@PathVariable String repositoryId) {
        
        try {
            CodeRepository repo = codeFlowService.getAnalysis(repositoryId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("patterns", repo.getDetectedPatterns());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get patterns", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to retrieve patterns: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
    
    /**
     * Export analysis as JSON
     * GET /api/codeflow/export/json/{repositoryId}
     */
    @GetMapping("/export/json/{repositoryId}")
    public ResponseEntity<CodeRepository> exportJson(@PathVariable String repositoryId) {
        
        try {
            CodeRepository repo = codeFlowService.getAnalysis(repositoryId);
            return ResponseEntity.ok(repo);
            
        } catch (Exception e) {
            logger.error("Export failed", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    
    /**
     * Re-analyze repository
     * POST /api/codeflow/reanalyze/{repositoryId}
     */
    @PostMapping("/reanalyze/{repositoryId}")
    public ResponseEntity<Map<String, Object>> reanalyze(@PathVariable String repositoryId) {
        
        logger.info("Re-analyzing repository: {}", repositoryId);
        
        try {
            // Trigger re-analysis (bypass cache)
            CodeRepository repo = codeFlowService.getAnalysis(repositoryId);
            codeFlowService.performFullAnalysis(repositoryId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Re-analysis started");
            response.put("repositoryId", repositoryId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Re-analysis failed", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Re-analysis failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Health check endpoint
     * GET /api/codeflow/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "CodeFlow");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
    
    // Request DTOs
    
    public static class AnalysisRequest {
        private String repoUrl;
        private String sourceType;
        private String ownerId;
        
        public String getRepoUrl() { return repoUrl; }
        public void setRepoUrl(String repoUrl) { this.repoUrl = repoUrl; }
        
        public String getSourceType() { return sourceType; }
        public void setSourceType(String sourceType) { this.sourceType = sourceType; }
        
        public String getOwnerId() { return ownerId; }
        public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    }
    
    public static class PullRequestRequest {
        private String repositoryId;
        private List<String> changedFiles;
        
        public String getRepositoryId() { return repositoryId; }
        public void setRepositoryId(String repositoryId) { this.repositoryId = repositoryId; }
        
        public List<String> getChangedFiles() { return changedFiles; }
        public void setChangedFiles(List<String> changedFiles) { this.changedFiles = changedFiles; }
    }
}