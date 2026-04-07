package org.example.controller;

import org.example.service.FeatureRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Feature Registry Controller — API for all platforms to discover features.
 * 
 * GET  /api/features          → All features (platforms render dynamically)
 * GET  /api/features/sync     → Sync report (which platform is missing what)
 * GET  /api/features/status   → Quick sync status
 * GET  /api/features/{id}     → Single feature details
 * POST /api/features/reload   → Reload registry from file
 */
@RestController
@RequestMapping("/api/features")
public class FeatureRegistryController {

    private static final Logger logger = LoggerFactory.getLogger(FeatureRegistryController.class);

    @Autowired
    private FeatureRegistryService featureRegistryService;

    /**
     * GET /api/features
     * Returns all registered features — any platform can use this to render its UI dynamically.
     */
    @GetMapping
    public ResponseEntity<?> getAllFeatures() {
        List<Map<String, Object>> features = featureRegistryService.getAllFeatures();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "success");
        response.put("count", features.size());
        response.put("features", features);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/features/sync
     * Full sync report — shows which platforms are missing which features.
     * This is the KEY endpoint for detecting feature drift.
     */
    @GetMapping("/sync")
    public ResponseEntity<?> getSyncReport() {
        Map<String, Object> report = featureRegistryService.getSyncReport();
        report.put("status", "success");
        return ResponseEntity.ok(report);
    }

    /**
     * GET /api/features/status
     * Quick sync status — for dashboard display.
     */
    @GetMapping("/status")
    public ResponseEntity<?> getQuickStatus() {
        Map<String, Object> status = featureRegistryService.getQuickStatus();
        status.put("status", "success");
        return ResponseEntity.ok(status);
    }

    /**
     * GET /api/features/{id}
     * Single feature details.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getFeature(@PathVariable String id) {
        return featureRegistryService.getFeature(id)
                .map(f -> ResponseEntity.ok(Map.of("status", "success", "feature", f)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/features/reload
     * Reload the registry from the JSON file.
     */
    @PostMapping("/reload")
    public ResponseEntity<?> reloadRegistry() {
        featureRegistryService.reload();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "success");
        response.put("message", "Feature registry reloaded");
        response.put("features", featureRegistryService.getAllFeatures().size());
        logger.info("🔄 Feature registry reloaded by admin");
        return ResponseEntity.ok(response);
    }
}
