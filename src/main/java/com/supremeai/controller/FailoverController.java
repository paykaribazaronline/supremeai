package com.supremeai.controller;

import com.supremeai.fallback.AIFallbackOrchestrator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/failover")
@CrossOrigin(origins = "*")
public class FailoverController {

    private final AIFallbackOrchestrator fallbackOrchestrator;

    public FailoverController(AIFallbackOrchestrator fallbackOrchestrator) {
        this.fallbackOrchestrator = fallbackOrchestrator;
    }

    @PostMapping("/execute")
    public ResponseEntity<Map<String, String>> executeWithFailover(@RequestBody Map<String, String> request) {
        String taskCategory = request.getOrDefault("taskCategory", "general");
        String errorSignature = request.getOrDefault("errorSignature", "unknown");
        String prompt = request.get("prompt");
        if (prompt == null || prompt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing 'prompt' field"));
        }
        try {
            String result = fallbackOrchestrator.executeWithSupremeIntelligence(taskCategory, errorSignature, prompt);
            return ResponseEntity.ok(Map.of("status", "success", "result", result));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }

    @GetMapping("/providers")
    public ResponseEntity<Map<String, Object>> getProviderStatus() {
        return ResponseEntity.ok(fallbackOrchestrator.getProviderHealthStatus());
    }
}
