package com.supremeai.controller;

import com.supremeai.fallback.ThirdOpinionOrchestrator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/failover")
@CrossOrigin(origins = "*")
public class FailoverController {

    private final ThirdOpinionOrchestrator thirdOpinionOrchestrator;

    public FailoverController(ThirdOpinionOrchestrator thirdOpinionOrchestrator) {
        this.thirdOpinionOrchestrator = thirdOpinionOrchestrator;
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
            String result = thirdOpinionOrchestrator.executeWithSupremeIntelligence(taskCategory, errorSignature, prompt).block();
            return ResponseEntity.ok(Map.of("status", "success", "result", result));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }

    @GetMapping("/providers")
    public ResponseEntity<Map<String, Object>> getProviderStatus() {
        return ResponseEntity.ok(thirdOpinionOrchestrator.getProviderHealthStatus());
    }
}
