package org.example.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * API Version Controller
 * Provides API versioning and endpoint routing
 */
@RestController
@RequestMapping("/api")
public class APIVersionController {
    
    @GetMapping
    public Map<String, Object> getAPIRoot() {
        return Map.ofEntries(
            Map.entry("message", "SupremeAI API"),
            Map.entry("currentVersion", "v2"),
            Map.entry("versions", new String[]{"v1", "v2"}),
            Map.entry("documentation", "/api/docs")
        );
    }
    
    @GetMapping("/v1/info")
    public Map<String, Object> getAPIInfoV1() {
        return Map.ofEntries(
            Map.entry("version", "v1"),
            Map.entry("deprecated", "This version is deprecated. Please upgrade to v2."),
            Map.entry("endpoints", new String[]{
                "/api/v1/projects",
                "/api/v1/agents",
                "/api/v1/metrics"
            })
        );
    }
    
    @GetMapping("/v2/info")
    public Map<String, Object> getAPIInfoV2() {
        return Map.ofEntries(
            Map.entry("version", "v2"),
            Map.entry("features", new String[]{
                "Webhooks with retry logic",
                "Request/response batching",
                "Enhanced error handling"
            }),
            Map.entry("endpoints", new String[]{
                "/api/v2/projects",
                "/api/v2/webhooks",
                "/api/v2/batch"
            })
        );
    }
}
