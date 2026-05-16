package com.supremeai.controller;

import com.supremeai.service.CodeGenerationService;
import com.supremeai.service.AppOrchestrationService;
import com.supremeai.generation.FullStackCodeGenerator;
import com.supremeai.generation.MultiPlatformGenerator;
import com.supremeai.model.GeneratedApp;
import com.supremeai.model.EntityDefinition;
import com.supremeai.model.FieldDefinition;
import com.supremeai.repository.GeneratedAppRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import com.supremeai.dto.AppGenerationRequest;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import com.supremeai.response.ApiResponse;
import java.util.UUID;

/**
 * Controller for app generation endpoints.
 * Handles requests to generate applications based on user requirements.
 */
@RestController
@RequestMapping({"/api/generate", "/api/teaching/create-app"})
public class AppGenerationController {
    
    private static final Logger logger = LoggerFactory.getLogger(AppGenerationController.class);
    
    @Autowired
    private CodeGenerationService codeGenerationService;
    
    @Autowired
    private FullStackCodeGenerator fullStackCodeGenerator;
    
    @Autowired
    private MultiPlatformGenerator multiPlatformGenerator;

    @Autowired
    private GeneratedAppRepository generatedAppRepository;

    @Autowired
    private AppOrchestrationService appOrchestrationService;

    @Autowired
    private WebSocketController webSocketController;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'GUEST')")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> generateApp(
            @Valid @RequestBody AppGenerationRequest request,
            Authentication auth) {
        
        String requestId = UUID.randomUUID().toString();
        String name = request.getName();
        String userId = auth != null ? auth.getName() : "anonymous";
        String description = request.getDescription();
        String platform = request.getPlatform();
        boolean useAI = request.isUseAI();

        logger.info("App generation request received [{}]: {} (useAI: {}) by user {}", 
            requestId, name, useAI, userId);

        if (useAI && "project".equals(request.getType())) {
            // Trigger the Full Orchestration Pipeline (AI + Code + GitHub)
            appOrchestrationService.runFullPipeline(description != null ? description : name, null)
                .flatMap(result -> {
                    // Save to repository for persistence/preview
                    String appId = UUID.randomUUID().toString();
                    GeneratedApp generatedApp = new GeneratedApp(appId, userId, platform, "React");
                    generatedApp.setHtmlContent(buildPreviewHtml(name, platform, description, result));
                    generatedApp.setStatus("GENERATED");
                    generatedApp.setRequestId(requestId);
                    
                    if (result.get("generatedApp") instanceof Map) {
                        Map<String, Object> genApp = (Map<String, Object>) result.get("generatedApp");
                        if (genApp.containsKey("files")) {
                            generatedApp.setSourceFiles((Map<String, String>) genApp.get("files"));
                        }
                    }
                    
                    return generatedAppRepository.save(generatedApp).thenReturn(result);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                    res -> logger.info("Orchestration pipeline completed for {}", requestId),
                    err -> logger.error("Orchestration pipeline failed for " + requestId, err)
                );

            Map<String, Object> acceptedResponse = new HashMap<>();
            acceptedResponse.put("requestId", requestId);
            acceptedResponse.put("status", "ACCEPTED");
            acceptedResponse.put("message", "Full AI orchestration pipeline started");
            
            return Mono.just(ResponseEntity.accepted().body(ApiResponse.ok(acceptedResponse)));
        }
        
        // Fallback to legacy/direct generation logic
        Mono.fromCallable(() -> {
            try {
                String database = request.getDatabase();
                String type = request.getType();

                webSocketController.broadcastAppGenProgress(requestId, name, "INITIALIZING", 5, "Initializing generation engine...");

                Map<String, String> decisions = new HashMap<>();
                decisions.put("architecture", "monolith");
                decisions.put("database", database);
                decisions.put("apiStyle", "REST");
                decisions.put("authType", "JWT");
                decisions.put("frontend", "React");
                decisions.put("deployment", "GCP");
                
                webSocketController.broadcastAppGenProgress(requestId, name, "ANALYZING", 15, "Analyzing requirements and entities...");
                
                Map<String, Object> result;
                
                // Use enhanced AI-powered generation if requested (legacy path)
                if (useAI) {
                    List<EntityDefinition> entities = request.getEntities();
                    if (entities == null) entities = new ArrayList<>();
                    result = codeGenerationService.generateAppWithAI(name, description, entities, database, "JWT");
                } else {
                    webSocketController.broadcastAppGenProgress(requestId, name, "GENERATING_CORE", 30, "Generating core application structure...");
                    
                    switch (platform.toLowerCase()) {
                        case "fullstack":
                            result = codeGenerationService.generateFromContext(decisions);
                            break;
                        case "web":
                        case "android":
                        case "ios":
                        case "desktop":
                            Map<String, String> platformResult = multiPlatformGenerator.generateForPlatform(
                                description != null && !description.isEmpty() ? description : name, platform);
                            result = new HashMap<>(platformResult);
                            result.put("decisions", decisions);
                            break;
                        default:
                            result = codeGenerationService.generateFromContext(decisions);
                            break;
                    }
                }
                
                webSocketController.broadcastAppGenProgress(requestId, name, "FINALIZING", 80, "Finalizing files and preparing preview...");

                result.put("name", name);
                result.put("description", description);
                result.put("platform", platform);
                result.put("type", type);
                result.put("status", "GENERATED");
                result.put("requestId", requestId);

                String appId = UUID.randomUUID().toString();
                GeneratedApp generatedApp = new GeneratedApp(appId, userId, platform, "React");
                generatedApp.setHtmlContent(buildPreviewHtml(name, platform, description, result));
                generatedApp.setStatus("GENERATED");
                generatedApp.setRequestId(requestId);
                
                if (result.containsKey("files")) {
                    generatedApp.setSourceFiles((Map<String, String>) result.get("files"));
                }
                
                generatedAppRepository.save(generatedApp).subscribe();
                result.put("appId", appId);
                
                webSocketController.broadcastAppGenProgress(requestId, name, "COMPLETED", 100, "Generation completed successfully!");
                return result;
            } catch (Exception e) {
                logger.error("Async app generation failed for request " + requestId, e);
                webSocketController.broadcastAppGenProgress(requestId, name, "FAILED", 0, "Error: " + e.getMessage());
                throw new RuntimeException(e);
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe(); 

        Map<String, Object> acceptedResponse = new HashMap<>();
        acceptedResponse.put("requestId", requestId);
        acceptedResponse.put("status", "ACCEPTED");
        acceptedResponse.put("message", "App generation started in background");
        
        return Mono.just(ResponseEntity.accepted().body(ApiResponse.ok(acceptedResponse)));
    }


    private String buildPreviewHtml(String name, String platform, String description, Map<String, Object> result) {
        StringBuilder htmlBuilder = new StringBuilder();
        htmlBuilder.append("<!DOCTYPE html><html><head><title>").append(name).append(" - Preview</title>");
        htmlBuilder.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        htmlBuilder.append("<style>");
        htmlBuilder.append("body { font-family: 'Inter', system-ui, sans-serif; background: #f0f2f5; margin: 0; padding: 20px; display: flex; justify-content: center; }");
        htmlBuilder.append(".container { background: white; border-radius: 16px; box-shadow: 0 10px 25px rgba(0,0,0,0.05); padding: 40px; max-width: 800px; width: 100%; }");
        htmlBuilder.append("h1 { color: #1a73e8; margin-top: 0; }");
        htmlBuilder.append(".badge { background: #e8f0fe; color: #1a73e8; padding: 4px 12px; border-radius: 20px; font-size: 14px; font-weight: 500; }");
        htmlBuilder.append(".file-list { margin-top: 30px; border-top: 1px solid #eee; padding-top: 20px; }");
        htmlBuilder.append(".file-item { display: flex; align-items: center; padding: 10px; border-bottom: 1px solid #f9f9f9; }");
        htmlBuilder.append(".file-icon { margin-right: 12px; color: #5f6368; }");
        htmlBuilder.append("</style></head><body>");
        htmlBuilder.append("<div class='container'>");
        htmlBuilder.append("<div style='display:flex; justify-content:space-between; align-items:center'>");
        htmlBuilder.append("<h1>").append(name).append("</h1>");
        htmlBuilder.append("<span class='badge'>").append(platform).append("</span>");
        htmlBuilder.append("</div>");
        htmlBuilder.append("<p style='color: #5f6368'>").append(description != null ? description : "AI-generated application build.").append("</p>");
        
        htmlBuilder.append("<div class='file-list'><h3>Generated Blueprint Files</h3>");
        if (result.containsKey("files")) {
            Map<String, String> files = (Map<String, String>) result.get("files");
            files.keySet().stream().limit(10).forEach(fileName -> {
                htmlBuilder.append("<div class='file-item'><span class='file-icon'>📄</span>").append(fileName).append("</div>");
            });
            if (files.size() > 10) {
                htmlBuilder.append("<p style='color:#999; margin-left:26px'>... and ").append(files.size() - 10).append(" more files</p>");
            }
        }
        htmlBuilder.append("</div>");
        
        htmlBuilder.append("<div style='margin-top: 40px; padding: 20px; background: #fff8e1; border-radius: 8px; border-left: 4px solid #ffc107;'>");
        htmlBuilder.append("<strong>Pro Tip:</strong> Use the SupremeAI Dashboard to deploy this code directly to Google Cloud or Firebase.");
        htmlBuilder.append("</div>");
        
        htmlBuilder.append("</div></body></html>");
        return htmlBuilder.toString();
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
    public Mono<ResponseEntity<ApiResponse<Map<String, String>>>> health() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "AppGenerationService");
        return Mono.just(ResponseEntity.ok(ApiResponse.ok(health)));
    }
    
    /**
     * Infrastructure advice endpoint - provides AI-powered deployment guidance.
     */
    @PostMapping("/infrastructure-advice")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'GUEST')")
    public Mono<ResponseEntity<ApiResponse<String>>> getInfrastructureAdvice(
            @RequestBody Map<String, String> request) {
        
        String appName = request.getOrDefault("appName", "My App");
        String description = request.getOrDefault("description", "");
        String techStack = request.getOrDefault("techStack", "Full Stack Spring Boot/React");
        String cloudPreference = request.getOrDefault("cloudPreference", "GCP");

        logger.info("Generating infrastructure advice for app: {}", appName);

        return codeGenerationService.generateInfrastructureAdvice(appName, description, techStack, cloudPreference)
                .map(advice -> ResponseEntity.ok(ApiResponse.ok(advice)))
                .onErrorResume(e -> {
                    logger.error("Failed to generate infrastructure advice", e);
                    return Mono.just(ResponseEntity.internalServerError().body(ApiResponse.error("Failed to generate infrastructure advice: " + e.getMessage())));
                });
    }

    /**
     * Preview generation - returns sample output without creating files.
     */
    @PostMapping("/preview")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> previewGeneration(@RequestBody Map<String, Object> request) {
        return Mono.fromCallable(() -> {
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
            @SuppressWarnings("unchecked")
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
            
            return ResponseEntity.ok(ApiResponse.ok(result));
            
        }).subscribeOn(Schedulers.boundedElastic())
        .onErrorResume(e -> {
            logger.error("Preview generation failed", e);
            return Mono.just(ResponseEntity.internalServerError().body(ApiResponse.error("Preview generation failed: " + e.getMessage())));
        });
    }
}
