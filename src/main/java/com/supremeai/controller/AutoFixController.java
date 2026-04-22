package com.supremeai.controller;

import com.supremeai.healing.AutoHealingEngine;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auto-fix")
@CrossOrigin(origins = "*")
public class AutoFixController {

    private final AutoHealingEngine autoHealingEngine;

    public AutoFixController(AutoHealingEngine autoHealingEngine) {
        this.autoHealingEngine = autoHealingEngine;
    }

    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeError(@RequestBody Map<String, String> request) {
        String error = request.get("error");
        if (error == null || error.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing 'error' field"));
        }
        Map<String, Object> result = autoHealingEngine.detectAndFix(error);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "status", "active",
                "service", "AutoHealingEngine"
        ));
    }
}
