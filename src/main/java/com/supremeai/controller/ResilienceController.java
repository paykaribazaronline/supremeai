package com.supremeai.controller;

import com.supremeai.fallback.AIFallbackOrchestrator;
import com.supremeai.resilience.RetryableAIExecutor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/resilience")
public class ResilienceController {

    private final RetryableAIExecutor retryableAIExecutor;
    private final AIFallbackOrchestrator fallbackOrchestrator;

    public ResilienceController(RetryableAIExecutor retryableAIExecutor, AIFallbackOrchestrator fallbackOrchestrator) {
        this.retryableAIExecutor = retryableAIExecutor;
        this.fallbackOrchestrator = fallbackOrchestrator;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = fallbackOrchestrator.getProviderHealthStatus();
        return ResponseEntity.ok(status);
    }

    @GetMapping("/retry-config")
    public ResponseEntity<Map<String, Object>> getRetryConfig() {
        return ResponseEntity.ok(Map.of(
                "maxRetries", 3,
                "initialBackoffMs", 500,
                "backoffMultiplier", 2.0,
                "jitterFactor", 0.3
        ));
    }

    @PostMapping("/test-retry")
    public ResponseEntity<Map<String, Object>> testRetry(@RequestBody Map<String, String> request) {
        String provider = request.getOrDefault("provider", "groq");
        try {
            String result = retryableAIExecutor.execute(provider, provider, () -> "Resilience test passed for " + provider);
            return ResponseEntity.ok(Map.of("status", "success", "result", result));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }
}
