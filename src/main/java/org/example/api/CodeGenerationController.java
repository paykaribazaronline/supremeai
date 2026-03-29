package org.example.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.example.service.CodeGenerationOrchestrator;
import java.util.*;

/**
 * CodeGenerationController: REST API for intelligent code generation
 * 
 * Endpoints for generating components, services, models with AI:
 * - React components with hooks
 * - Node.js services with routes and tests
 * - Multi-framework model generation
 * - Batch generation for rapid development
 * - Validation integration during generation
 */
@RestController
@RequestMapping("/api/generation")
@CrossOrigin(origins = "http://localhost:8001")
public class CodeGenerationController {

    @Autowired
    private CodeGenerationOrchestrator orchestrator;

    /**
     * Generate a React component
     * POST /api/generation/react-component
     */
    @PostMapping("/react-component")
    public Map<String, Object> generateReactComponent(
            @RequestParam String projectId,
            @RequestParam String componentName,
            @RequestParam String description,
            @RequestParam(required = false, defaultValue = "") String features) {
        
        List<String> featureList = features.isEmpty() ? 
            new ArrayList<>() : 
            Arrays.asList(features.split(","));
        
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> result = orchestrator.generateReactComponent(
                projectId, componentName, description, featureList
            );
            response.put("success", true);
            response.put("data", result);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * Generate a Node.js service with routes and tests
     * POST /api/generation/node-service
     */
    @PostMapping("/node-service")
    public Map<String, Object> generateNodeService(
            @RequestParam String projectId,
            @RequestParam String serviceName,
            @RequestParam String description,
            @RequestParam(required = false, defaultValue = "") String methods) {
        
        List<String> methodList = methods.isEmpty() ? 
            new ArrayList<>() : 
            Arrays.asList(methods.split(","));
        
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> result = orchestrator.generateNodeService(
                projectId, serviceName, description, methodList
            );
            response.put("success", true);
            response.put("data", result);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * Generate a model/entity class
     * POST /api/generation/model
     */
    @PostMapping("/model")
    public Map<String, Object> generateModel(
            @RequestParam String projectId,
            @RequestParam String modelName,
            @RequestParam String framework,
            @RequestBody(required = false) Map<String, Object> body) {
        
        @SuppressWarnings("unchecked")
        Map<String, String> fields = (Map<String, String>) 
            (body != null ? body.get("fields") : new HashMap<>());
        
        @SuppressWarnings("unchecked")
        List<String> relations = (List<String>) 
            (body != null ? body.get("relations") : new ArrayList<>());
        
        if (fields == null) fields = new HashMap<>();
        if (relations == null) relations = new ArrayList<>();
        
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> result = orchestrator.generateModel(
                projectId, modelName, framework, fields, relations
            );
            response.put("success", true);
            response.put("data", result);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * Generate and immediately validate a module
     * POST /api/generation/validate-and-generate
     */
    @PostMapping("/validate-and-generate")
    public Map<String, Object> generateAndValidate(
            @RequestParam String projectId,
            @RequestParam String moduleName,
            @RequestParam String framework,
            @RequestParam String description) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> result = orchestrator.generateAndValidateModule(
                projectId, moduleName, framework, description
            );
            response.put("success", true);
            response.put("data", result);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * Generate utility/helper functions
     * POST /api/generation/utility
     */
    @PostMapping("/utility")
    public Map<String, Object> generateUtility(
            @RequestParam String projectId,
            @RequestParam String utilityName,
            @RequestParam String framework,
            @RequestParam String purpose) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> result = orchestrator.generateUtility(
                projectId, utilityName, framework, purpose
            );
            response.put("success", true);
            response.put("data", result);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * Batch generate multiple components
     * POST /api/generation/batch
     */
    @PostMapping("/batch")
    public Map<String, Object> generateBatch(
            @RequestParam String projectId,
            @RequestParam String framework,
            @RequestBody List<Map<String, String>> components) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> result = orchestrator.generateBatch(
                projectId, framework, components
            );
            response.put("success", true);
            response.put("data", result);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * Get generation capabilities and statistics
     * GET /api/generation/stats
     */
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> stats = orchestrator.getGenerationStats();
            response.put("success", true);
            response.put("stats", stats);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
    /**
     * Get generation history for a project
     * GET /api/generation/history/{projectId}
     */
    @GetMapping("/history/{projectId}")
    public Map<String, Object> getGenerationHistory(@PathVariable String projectId) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> history = orchestrator.getGenerationHistory(projectId);
            response.put("success", true);
            response.put("data", history);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * Get overall generation statistics across all projects
     * GET /api/generation/analytics
     */
    @GetMapping("/analytics")
    public Map<String, Object> getGenerationAnalytics() {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> stats = orchestrator.getAllGenerationStats();
            response.put("success", true);
            response.put("analytics", stats);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * Get supported frameworks
     * GET /api/generation/frameworks
     */
    @GetMapping("/frameworks")
    public Map<String, Object> getFrameworks() {
        Map<String, Object> response = new HashMap<>();
        response.put("frameworks", Arrays.asList("REACT", "NODEJS", "FLUTTER", "PYTHON", "JAVA"));
        response.put("componentTypes", Arrays.asList("component", "service", "model", "utility", "controller"));
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * Get generation configuration
     * GET /api/generation/config
     */
    @GetMapping("/config")
    public Map<String, Object> getConfig() {
        Map<String, Object> response = new HashMap<>();
        
        Map<String, Object> config = new HashMap<>();
        config.put("aiModels", Arrays.asList("gpt-4", "claude-3", "gpt-3.5-turbo"));
        config.put("maxComponentsPerBatch", 50);
        config.put("estimatedTimePerComponent", "2 seconds");
        config.put("supportedLanguages", Arrays.asList("typescript", "javascript", "python", "java", "dart"));
        config.put("codeQualityThreshold", 80);
        config.put("autoValidation", true);
        config.put("autoFixing", true);
        
        response.put("config", config);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * Health check
     * GET /api/generation/health
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("service", "CodeGenerationOrchestrator");
        response.put("version", "1.0");
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}
