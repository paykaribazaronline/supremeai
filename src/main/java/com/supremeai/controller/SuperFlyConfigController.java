package com.supremeai.controller;

import com.supremeai.model.SuperFlyConfig;
import com.supremeai.repository.SuperFlyConfigRepository;
import com.supremeai.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/config/superfly")
public class SuperFlyConfigController {

    private static final Logger log = LoggerFactory.getLogger(SuperFlyConfigController.class);

    private final SuperFlyConfigRepository repository;

    @Autowired
    public SuperFlyConfigController(SuperFlyConfigRepository repository) {
        this.repository = repository;
    }

    /**
     * Get the SuperFly dynamic settings.
     * Accessible by guests, users, and admins since it's a general feature.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'AGENT_MANAGER', 'GUEST')")
    public Mono<ResponseEntity<ApiResponse<SuperFlyConfig>>> getSettings() {
        return repository.findById("superfly_settings")
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("[SuperFly] No settings found in Firestore. Seeding default configuration...");
                    SuperFlyConfig defaultConfig = new SuperFlyConfig();
                    return repository.save(defaultConfig);
                }))
                .map(config -> ResponseEntity.ok(ApiResponse.ok(config)))
                .onErrorResume(e -> {
                    log.error("[SuperFly] Failed to fetch settings: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(500)
                            .body(ApiResponse.error("Failed to load SuperFly configurations: " + e.getMessage())));
                });
    }

    /**
     * Update SuperFly configuration (Admin only).
     */
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<ApiResponse<SuperFlyConfig>>> updateSettings(@RequestBody SuperFlyConfig update) {
        update.setId("superfly_settings");
        return repository.save(update)
                .map(saved -> ResponseEntity.ok(ApiResponse.ok(saved)))
                .onErrorResume(e -> {
                    log.error("[SuperFly] Failed to update settings: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(500)
                            .body(ApiResponse.error("Failed to save SuperFly configurations: " + e.getMessage())));
                });
    }
}
