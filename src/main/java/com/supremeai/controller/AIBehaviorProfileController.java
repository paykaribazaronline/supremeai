package com.supremeai.controller;

import com.supremeai.model.AIBehaviorProfile;
import com.supremeai.service.AIBehaviorProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/behavior-profiles")
public class AIBehaviorProfileController {
    private static final Logger logger = LoggerFactory.getLogger(AIBehaviorProfileController.class);

    @Autowired
    private AIBehaviorProfileService profileService;

    @GetMapping("/project/{projectId}")
    public Mono<ResponseEntity<AIBehaviorProfile>> getProfileByProject(@PathVariable String projectId) {
        return profileService.getProfileForProject(projectId)
                .map(profile -> ResponseEntity.ok(profile))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping
    public Flux<AIBehaviorProfile> getAllProfiles() {
        return profileService.getAllProfiles();
    }

    @PostMapping
    public Mono<ResponseEntity<AIBehaviorProfile>> saveProfile(@RequestBody AIBehaviorProfile profile) {
        return profileService.saveProfile(profile)
                .map(saved -> ResponseEntity.status(HttpStatus.CREATED).body(saved));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteProfile(@PathVariable String id) {
        return profileService.deleteProfile(id)
                .then(Mono.just(ResponseEntity.noContent().build()));
    }

    @GetMapping("/default")
    public Mono<ResponseEntity<AIBehaviorProfile>> getDefaultProfile() {
        return profileService.getDefaultProfile()
                .map(profile -> ResponseEntity.ok(profile));
    }

    @GetMapping("/security-options")
    public ResponseEntity<Map<String, String>> getSecurityOptions() {
        Map<String, String> options = Map.of(
                "LOW", "Basic security checks only",
                "MEDIUM", "Standard security practices (OWASP Top 10)",
                "HIGH", "Strict security with additional auditing and encryption"
        );
        return ResponseEntity.ok(options);
    }

    @GetMapping("/performance-options")
    public ResponseEntity<Map<String, String>> getPerformanceOptions() {
        Map<String, String> options = Map.of(
                "SPEED_OPTIMIZED", "Prioritize fastest execution",
                "BALANCED", "Balance speed, memory, and readability",
                "QUALITY_OPTIMIZED", "Prioritize code quality and maintainability"
        );
        return ResponseEntity.ok(options);
    }
}
