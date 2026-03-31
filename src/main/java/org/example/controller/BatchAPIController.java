package org.example.controller;

import org.example.service.RequestBatchingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Batch API Controller
 * Provides endpoints for batch request operations
 */
@RestController
@RequestMapping("/api/v2/batch")
public class BatchAPIController {
    
    @Autowired
    private RequestBatchingService batchingService;
    
    /**
     * Create a new batch
     * POST /api/v2/batch
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createBatch(@RequestBody Map<String, String> request) {
        String batchName = request.getOrDefault("name", "batch-" + System.currentTimeMillis());
        String batchId = batchingService.createBatch(batchName);
        
        return ResponseEntity.ok(Map.of(
            "batchId", batchId,
            "batchName", batchName,
            "status", "created"
        ));
    }
    
    /**
     * Add request to batch
     * POST /api/v2/batch/{batchId}/requests
     */
    @PostMapping("/{batchId}/requests")
    public ResponseEntity<Map<String, Object>> addRequestToBatch(
            @PathVariable String batchId,
            @RequestBody Map<String, Object> request) {
        
        RequestBatchingService.BatchOperation batch = batchingService.getBatch(batchId);
        if (batch == null) {
            return ResponseEntity.notFound().build();
        }
        
        batchingService.addRequestToBatch(batchId, request);
        
        return ResponseEntity.ok(Map.of(
            "batchId", batchId,
            "requestAdded", true,
            "totalRequests", batch.getRequestCount() + 1
        ));
    }
    
    /**
     * Get batch details
     * GET /api/v2/batch/{batchId}
     */
    @GetMapping("/{batchId}")
    public ResponseEntity<Map<String, Object>> getBatch(@PathVariable String batchId) {
        RequestBatchingService.BatchOperation batch = batchingService.getBatch(batchId);
        
        if (batch == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(Map.ofEntries(
            Map.entry("batchId", batchId),
            Map.entry("name", batch.getName()),
            Map.entry("requestCount", batch.getRequestCount()),
            Map.entry("responseCount", batch.getResponseCount()),
            Map.entry("completed", batch.isCompleted()),
            Map.entry("cancelled", batch.isCancelled()),
            Map.entry("createdAt", batch.getCreatedAt()),
            Map.entry("completedAt", batch.getCompletedAt())
        ));
    }
    
    /**
     * Execute batch
     * POST /api/v2/batch/{batchId}/execute
     */
    @PostMapping("/{batchId}/execute")
    public ResponseEntity<Map<String, Object>> executeBatch(@PathVariable String batchId) {
        RequestBatchingService.BatchOperation batch = batchingService.getBatch(batchId);
        
        if (batch == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> result = batchingService.executeBatch(batchId);
        return ResponseEntity.ok(result);
    }
    
    /**
     * Cancel batch
     * POST /api/v2/batch/{batchId}/cancel
     */
    @PostMapping("/{batchId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelBatch(@PathVariable String batchId) {
        RequestBatchingService.BatchOperation batch = batchingService.getBatch(batchId);
        
        if (batch == null) {
            return ResponseEntity.notFound().build();
        }
        
        batchingService.cancelBatch(batchId);
        
        return ResponseEntity.ok(Map.of(
            "batchId", batchId,
            "cancelled", true,
            "status", "cancelled"
        ));
    }
    
    /**
     * List all batches
     * GET /api/v2/batch
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listBatches() {
        List<RequestBatchingService.BatchOperation> batches = batchingService.listBatches();
        
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (RequestBatchingService.BatchOperation b : batches) {
            Map<String, Object> batchMap = new java.util.HashMap<>();
            batchMap.put("batchId", b.getId());
            batchMap.put("name", b.getName());
            batchMap.put("requestCount", b.getRequestCount());
            batchMap.put("completed", b.isCompleted());
            batchMap.put("cancelled", b.isCancelled());
            result.add(batchMap);
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Clear completed batches
     * DELETE /api/v2/batch/completed
     */
    @DeleteMapping("/completed")
    public ResponseEntity<Map<String, Object>> clearCompletedBatches() {
        int cleared = batchingService.clearCompletedBatches();
        
        return ResponseEntity.ok(Map.of(
            "clearedCount", cleared,
            "message", "Completed batches cleared"
        ));
    }
}
