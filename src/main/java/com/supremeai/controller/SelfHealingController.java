package com.supremeai.controller;

import com.supremeai.service.SelfHealingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/self-healing")
@CrossOrigin(origins = "*")
public class SelfHealingController {

    private final SelfHealingService selfHealingService;

    public SelfHealingController(SelfHealingService selfHealingService) {
        this.selfHealingService = selfHealingService;
    }

    @PostMapping("/retry")
    public ResponseEntity<Map<String, Object>> executeWithRetry(@RequestBody Map<String, Object> request) {
        int maxAttempts = (int) request.getOrDefault("maxAttempts", 3);
        long initialBackoff = ((Number) request.getOrDefault("initialBackoff", 1000L)).longValue();
        String taskName = (String) request.getOrDefault("taskName", "unknown");

        try {
            String result = selfHealingService.executeWithRetry(
                    () -> Mono.just("Task " + taskName + " completed successfully"),
                    maxAttempts,
                    initialBackoff
            ).block();

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "result", result,
                    "maxAttempts", maxAttempts
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "status", "failed",
                    "error", e.getMessage(),
                    "maxAttempts", maxAttempts
            ));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "status", "active",
                "service", "SelfHealingService"
        ));
    }
}
