package org.example.api;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.Map;

/**
 * API Versioning Controller
 * Provides multiple API versions for backward compatibility
 * 
 * v1: Legacy endpoints (maintained for backward compatibility)
 * v2: New endpoints with enhanced features
 */
@RestController
@RequestMapping("/api")
public class APIVersionController {
    
    /**
     * API v1 - Legacy endpoints (backward compatible)
     */
    @GetMapping("/v1/info")
    public Map<String, Object> getAPIInfoV1() {
        return Map.ofEntries(
            Map.entry("version", "v1"),
            Map.entry("status", "active"),
            Map.entry("endpoints", new String[]{
                "/api/v1/projects",
                "/api/v1/agents",
                "/api/v1/providers"
            }),
            Map.entry("description", "Legacy API endpoints (backward compatible)")
        );
    }
    
    /**
     * API v2 - New endpoints with enhanced features
     */
    @GetMapping("/v2/info")
    public Map<String, Object> getAPIInfoV2() {
        return Map.ofEntries(
            Map.entry("version", "v2"),
            Map.entry("status", "current"),
            Map.entry("endpoints", new String[]{
                "/api/v2/projects",
                "/api/v2/agents",
                "/api/v2/providers",
                "/api/v2/webhooks",
                "/api/v2/batch"
            }),
            Map.entry("features", new String[]{
                "Webhooks with retry logic",
                "Request/response batching",
                "Enhanced error handling",
                "Rate limiting",
                "Request tracking"
            }),
            Map.entry("description", "Modern API with advanced features"),
            Map.entry("deprecated", new String[]{
                "v1 will be sunset on 2026-12-31"
            })
        );
    }
    
    /**
     * API root endpoint with version information
     */
    @GetMapping
    public Map<String, Object> getAPIRoot() {
        return Map.ofEntries(
            Map.entry("name", "SupremeAI API"),
            Map.entry("currentVersion", "v2"),
            Map.entry("versions", new Map[]{
                Map.ofEntries(
                    Map.entry("version", "v1"),
                    Map.entry("status", "active"),
                    Map.entry("info_url", "/api/v1/info")
                ),
                Map.ofEntries(
                    Map.entry("version", "v2"),
                    Map.entry("status", "current"),
                    Map.entry("info_url", "/api/v2/info")
                )
            }),
            Map.entry("documentation", "/swagger-ui.html"),
            Map.entry("openapi", "/v3/api-docs")
        );
    }
}
