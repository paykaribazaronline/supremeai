package com.supremeai.controller;

import com.supremeai.service.CodeGenerationService;
import com.supremeai.service.CodeGenerationServiceEnhanced;
import com.supremeai.generation.FullStackCodeGenerator;
import com.supremeai.generation.MultiPlatformGenerator;
import com.supremeai.model.EntityDefinition;
import com.supremeai.model.FieldDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for app generation endpoints.
 * Handles requests to generate applications based on user requirements.
 */
@RestController
@RequestMapping("/api/generate")
public class AppGenerationController {
    
    private static final Logger logger = LoggerFactory.getLogger(AppGenerationController.class);
    
    @Autowired
    private CodeGenerationService codeGenerationService;
    
    @Autowired
    private FullStackCodeGenerator fullStackCodeGenerator;
    
    @Autowired
    private MultiPlatformGenerator multiPlatformGenerator;
    
    @Autowired
    private CodeGenerationServiceEnhanced codeGenerationServiceEnhanced;
    
    /**
     * Generate application from requirements.
     * POST /api/generate
     * Body: { "name": "App Name", "description": "App description", "platform": "fullstack", "database": "PostgreSQL" }
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> generateApp(@RequestBody Map<String, Object> request) {
        try {
            String name = (String) request.getOrDefault("name", "GeneratedApp");
            String description = (String) request.getOrDefault("description", "");
            String platform = (String) request.getOrDefault("platform", "fullstack");
            String database = (String) request.getOrDefault("database", "PostgreSQL");
            String type = (String) request.getOrDefault("type", "project");
            boolean useAI = (Boolean) request.getOrDefault("useAI", false);
            
            logger.info("Generating app: {} (platform: {}, database: {}, AI: {})", name, platform, database, useAI);
            
            Map<String, String> decisions = new HashMap<>();
            decisions.put("architecture", "monolith");
            decisions.put("database", database);
            decisions.put("apiStyle", "REST");
            decisions.put("authType", "JWT");
            decisions.put("frontend", "React");
            decisions.put("deployment", "GCP");
            
            Map<String, Object> result;
            
            // Use enhanced AI-powered generation if requested
            if (useAI) {
                List<EntityDefinition> entities = parseEntitiesFromRequest(request);
                result = codeGenerationServiceEnhanced.generateAppWithAI(
                    name, description, entities, database, "JWT"
                );
            } else {
                // Use appropriate generator based on platform
                switch (platform.toLowerCase()) {
                    case "fullstack":
                        result = codeGenerationService.generateFromContext(decisions);
                        break;
                        
                    case "web":
                    case "android":
                    case "ios":
                    case "desktop":
                        Map<String, String> platformResult = multiPlatformGenerator.generateForPlatform(
                            description != null && !description.isEmpty() ? description : name, 
                            platform
                        );
                        result = new HashMap<>(platformResult);
                        result.put("decisions", decisions);
                        break;
                        
                    default:
                        // Default to fullstack generation
                        result = codeGenerationService.generateFromContext(decisions);
                        break;
                }
            }
            
            // Add metadata
            result.put("name", name);
            result.put("description", description);
            result.put("platform", platform);
            result.put("type", type);
            result.put("status", "GENERATED");
            result.put("message", "App generated successfully");
            
            logger.info("App generation completed: {} ({} files)", name, result.getOrDefault("fileCount", 0));
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("App generation failed", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("message", "App generation failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * Parse entity definitions from request
     */
    @SuppressWarnings("unchecked")
    private List<EntityDefinition> parseEntitiesFromRequest(Map<String, Object> request) {
        List<EntityDefinition> entities = new ArrayList<>();
        
        // Check if custom entities are provided
        if (request.containsKey("entities")) {
            List<Map<String, Object>> entityMaps = (List<Map<String, Object>>) request.get("entities");
            for (Map<String, Object> entityMap : entityMaps) {
                EntityDefinition entity = new EntityDefinition();
                entity.setName((String) entityMap.get("name"));
                entity.setDescription((String) entityMap.get("description"));
                
                List<FieldDefinition> fields = new ArrayList<>();
                if (entityMap.containsKey("fields")) {
                    List<Map<String, Object>> fieldMaps = (List<Map<String, Object>>) entityMap.get("fields");
                    for (Map<String, Object> fieldMap : fieldMaps) {
                        FieldDefinition field = new FieldDefinition();
                        field.setName((String) fieldMap.get("name"));
                        field.setType((String) fieldMap.get("type"));
                        field.setRequired((Boolean) fieldMap.getOrDefault("required", false));
                        field.setUnique((Boolean) fieldMap.getOrDefault("unique", false));
                        if (fieldMap.containsKey("maxLength")) {
                            field.setMaxLength(((Number) fieldMap.get("maxLength")).intValue());
                        }
                        fields.add(field);
                    }
                }
                entity.setFields(fields);
                entities.add(entity);
            }
        } else {
            // Default to Product entity
            entities.add(createDefaultProductEntity());
        }
        
        return entities;
    }
    
    /**
     * Create default Product entity
     */
    private EntityDefinition createDefaultProductEntity() {
        EntityDefinition entity = new EntityDefinition();
        entity.setName("Product");
        entity.setDescription("Product entity with basic fields");
        
        List<FieldDefinition> fields = new ArrayList<>();
        
        FieldDefinition nameField = new FieldDefinition();
        nameField.setName("name");
        nameField.setType("string");
        nameField.setRequired(true);
        nameField.setMaxLength(255);
        fields.add(nameField);
        
        FieldDefinition descField = new FieldDefinition();
        descField.setName("description");
        descField.setType("text");
        descField.setRequired(false);
        fields.add(descField);
        
        FieldDefinition priceField = new FieldDefinition();
        priceField.setName("price");
        priceField.setType("double");
        priceField.setRequired(true);
        fields.add(priceField);
        
        FieldDefinition stockField = new FieldDefinition();
        stockField.setName("stock");
        stockField.setType("integer");
        stockField.setRequired(false);
        fields.add(stockField);
        
        FieldDefinition categoryField = new FieldDefinition();
        categoryField.setName("category");
        categoryField.setType("string");
        categoryField.setRequired(false);
        categoryField.setMaxLength(100);
        fields.add(categoryField);
        
        entity.setFields(fields);
        return entity;
    }
    
    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "AppGenerationService");
        return ResponseEntity.ok(health);
    }
    
    /**
     * Preview generation - returns sample output without creating files.
     */
    @PostMapping("/preview")
    public ResponseEntity<Map<String, Object>> previewGeneration(@RequestBody Map<String, Object> request) {
        try {
            String platform = (String) request.getOrDefault("platform", "fullstack");
            
            Map<String, String> decisions = new HashMap<>();
            decisions.put("architecture", "monolith");
            decisions.put("database", "PostgreSQL");
            decisions.put("apiStyle", "REST");
            decisions.put("authType", "JWT");
            decisions.put("frontend", "React");
            decisions.put("deployment", "GCP");
            
            Map<String, Object> result = codeGenerationService.generateFromContext(decisions);
            
            // Limit preview to first few files
            Map<String, String> files = (Map<String, String>) result.get("files");
            if (files != null && files.size() > 3) {
                Map<String, String> previewFiles = new HashMap<>();
                int count = 0;
                for (Map.Entry<String, String> entry : files.entrySet()) {
                    if (count++ >= 3) break;
                    previewFiles.put(entry.getKey(), entry.getValue());
                }
                result.put("files", previewFiles);
                result.put("preview", true);
                result.put("totalFiles", files.size());
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Preview generation failed", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("message", "Preview generation failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
