package com.supremeai.controller;

import com.supremeai.healing.AutoHealingEngine;
import com.supremeai.intelligence.healing.InfiniteAutoHealer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/healing-system")
@CrossOrigin(origins = "*")
public class HealingSystemController {

    private final AutoHealingEngine autoHealingEngine;
    private final InfiniteAutoHealer infiniteAutoHealer;

    public HealingSystemController(AutoHealingEngine autoHealingEngine, InfiniteAutoHealer infiniteAutoHealer) {
        this.autoHealingEngine = autoHealingEngine;
        this.infiniteAutoHealer = infiniteAutoHealer;
    }

    @PostMapping("/detect")
    public ResponseEntity<Map<String, Object>> detectAndFix(@RequestBody Map<String, String> request) {
        String error = request.get("error");
        if (error == null || error.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing 'error' field"));
        }
        return ResponseEntity.ok(autoHealingEngine.detectAndFix(error));
    }

    @PostMapping("/develop")
    public ResponseEntity<Map<String, String>> developUntilPerfection(@RequestBody Map<String, String> request) {
        String taskCategory = request.getOrDefault("taskCategory", "general");
        String prompt = request.get("prompt");
        if (prompt == null || prompt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing 'prompt' field"));
        }
        try {
            String result = infiniteAutoHealer.developUntilPerfection(taskCategory, prompt);
            return ResponseEntity.ok(Map.of("status", "success", "code", result));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "status", "active",
                "autoHealing", "enabled",
                "infiniteLoop", "enabled"
        ));
    }
}
