package com.supremeai.controller;

import com.supremeai.service.UnifiedDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/data")
public class DataController {

    private final UnifiedDataService unifiedDataService;

    public DataController(UnifiedDataService unifiedDataService) {
        this.unifiedDataService = unifiedDataService;
    }

    @PostMapping("/collect")
    public ResponseEntity<Map<String, String>> collectData(@RequestBody Map<String, Object> request) {
        String source = (String) request.get("source");
        Object data = request.get("data");
        if (source == null || source.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Source is required"));
        }
        unifiedDataService.collectData(source, data);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Data collected from " + source));
    }

    @PostMapping("/purge")
    public ResponseEntity<Map<String, String>> purgeData() {
        unifiedDataService.purgeOldData();
        return ResponseEntity.ok(Map.of("status", "success", "message", "Old data purged"));
    }

    @PostMapping("/query")
    public ResponseEntity<Map<String, Object>> queryData(@RequestBody Map<String, String> request) {
        String query = request.get("query");
        Map<String, Object> result = unifiedDataService.getCollectedData(query);
        return ResponseEntity.ok(Map.of("status", "success", "result", result));
    }
}
