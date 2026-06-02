package com.supremeai.controller;

import com.supremeai.dto.AppGenerationRequest;
import com.supremeai.model.EntityDefinition;
import com.supremeai.model.FieldDefinition;
import com.supremeai.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import com.supremeai.service.AppGenerationService;

import java.util.*;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping({"/api/generate", "/api/teaching/create-app"})
public class AppGenerationController {

    private static final Logger logger = LoggerFactory.getLogger(AppGenerationController.class);

    private final AppGenerationService appGenerationService;

    public AppGenerationController(AppGenerationService appGenerationService) {
        this.appGenerationService = appGenerationService;
    }

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

        List<EntityDefinition> entities = request.getEntities();
        if (entities == null) {
            entities = List.of(appGenerationService.createDefaultProductEntity());
        }

        return appGenerationService.generateApp(requestId, name, userId, description, platform, useAI, request.getType(), request.getDatabase(), entities)
                .map(response -> ResponseEntity.accepted().body(ApiResponse.ok(response)))
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .onErrorResume(IllegalArgumentException.class, e ->
                        Mono.just(ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()))))
                .onErrorResume(Exception.class, e -> {
                    logger.error("Failed to generate app for request {}", requestId, e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ApiResponse.error("Failed to generate application: " + e.getMessage())));
                });
    }

    @GetMapping("/health")
    public Mono<ResponseEntity<ApiResponse<Map<String, String>>>> health() {
        return appGenerationService.health();
    }

    @PostMapping("/infrastructure-advice")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'GUEST')")
    public Mono<ResponseEntity<ApiResponse<String>>> getInfrastructureAdvice(
            @RequestBody Map<String, String> request) {

        String appName = request.getOrDefault("appName", "My App");
        String description = request.getOrDefault("description", "");
        String techStack = request.getOrDefault("techStack", "Full Stack Spring Boot/React");
        String cloudPreference = request.getOrDefault("cloudPreference", "GCP");

        return appGenerationService.getInfrastructureAdvice(appName, description, techStack, cloudPreference);
    }

    @PostMapping("/preview")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> previewGeneration(@RequestBody Map<String, Object> request) {
        return appGenerationService.previewGeneration(request)
                .map(response -> ResponseEntity.ok(response))
                .onErrorResume(e -> {
                    logger.error("Preview generation failed", e);
                    return Mono.just(ResponseEntity.internalServerError()
                            .body(ApiResponse.error("Preview generation failed: " + e.getMessage())));
                });
    }

}
