package com.supremeai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller to handle external tool probes and extension requests.
 * Resolves 403 Forbidden errors triggered by browser extensions or dev tools
 * looking for specific localized endpoints.
 */
@RestController
@RequestMapping("/api/ext")
public class ExternalToolsController {

    /**
     * Handle auth-token probe.
     * Often called by IDE extensions or local dev tools to check for session parity.
     */
    @GetMapping("/auth-token")
    @PostMapping("/auth-token")
    public ResponseEntity<Map<String, Object>> handleAuthTokenProbe() {
        return ResponseEntity.ok(Map.of(
            "status", "active",
            "message", "SupremeAI External API is operational",
            "version", "1.0.0"
        ));
    }

    /**
     * Handle activate probe.
     * Some tools use this to check if the local environment is ready for extension injection.
     */
    @GetMapping("/activate")
    @PostMapping("/activate")
    public ResponseEntity<Map<String, Object>> handleActivateProbe() {
        return ResponseEntity.ok(Map.of(
            "status", "activated",
            "timestamp", System.currentTimeMillis()
        ));
    }
}
