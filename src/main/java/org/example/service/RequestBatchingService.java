package org.example.service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Request Batching Service
 * Allows batching multiple API requests into a single operation
 */
public class RequestBatchingService {
    
    private static final int DEFAULT_BATCH_SIZE = 10;
    private static final long DEFAULT_BATCH_TIMEOUT_MS = 5000; // 5 seconds
    
    private final Map<String, BatchOperation> batches = new ConcurrentHashMap<>();
    
    /**
     * Create a new batch operation
     */
    public String createBatch(String batchName) {
        String batchId = UUID.randomUUID().toString();
        BatchOperation batch = new BatchOperation(batchId, batchName);
        batches.put(batchId, batch);
        return batchId;
    }
    
    /**
     * Add request to a batch
     */
    public void addRequestToBatch(String batchId, Map<String, Object> request) {
        BatchOperation batch = batches.get(batchId);
        if (batch != null && !batch.isCompleted()) {
            batch.addRequest(request);
        }
    }
    
    /**
     * Get batch for execution
     */
    public BatchOperation getBatch(String batchId) {
        return batches.get(batchId);
    }
    
    /**
     * Execute batch operations
     */
    public Map<String, Object> executeBatch(String batchId) {
        BatchOperation batch = batches.get(batchId);
        if (batch == null) {
            return Map.of("error", "Batch not found", "batchId", batchId);
        }
        
        if (batch.isCompleted()) {
            return Map.of("error", "Batch already completed", "batchId", batchId);
        }
        
        // Process all requests in batch
        List<Map<String, Object>> responses = new ArrayList<>();
        for (Map<String, Object> request : batch.getRequests()) {
            Map<String, Object> response = processRequest(request);
            responses.add(response);
        }
        
        // Mark batch as completed
        batch.setCompleted(true);
        batch.setCompletedAt(System.currentTimeMillis());
        batch.setResponses(responses);
        
        return Map.ofEntries(
            Map.entry("batchId", batchId),
            Map.entry("status", "completed"),
            Map.entry("requestCount", batch.getRequests().size()),
            Map.entry("responseCount", responses.size()),
            Map.entry("completedAt", batch.getCompletedAt())
        );
    }
    
    /**
     * Process individual request (simplified)
     */
    private Map<String, Object> processRequest(Map<String, Object> request) {
        Map<String, Object> response = new ConcurrentHashMap<>(request);
        response.put("processed", true);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
    
    /**
     * Cancel batch operation
     */
    public void cancelBatch(String batchId) {
        BatchOperation batch = batches.get(batchId);
        if (batch != null) {
            batch.setCompleted(true);
            batch.setCancelled(true);
        }
    }
    
    /**
     * Get all batches
     */
    public List<BatchOperation> listBatches() {
        return new ArrayList<>(batches.values());
    }
    
    /**
     * Clear completed batches
     */
    public int clearCompletedBatches() {
        List<String> completedIds = new ArrayList<>();
        for (Map.Entry<String, BatchOperation> entry : batches.entrySet()) {
            if (entry.getValue().isCompleted()) {
                completedIds.add(entry.getKey());
            }
        }
        
        for (String id : completedIds) {
            batches.remove(id);
        }
        
        return completedIds.size();
    }
    
    /**
     * Batch Operation Model
     */
    public static class BatchOperation {
        private String id;
        private String name;
        private List<Map<String, Object>> requests = new ArrayList<>();
        private List<Map<String, Object>> responses = new ArrayList<>();
        private boolean completed = false;
        private boolean cancelled = false;
        private long createdAt;
        private long completedAt;
        
        public BatchOperation(String id, String name) {
            this.id = id;
            this.name = name;
            this.createdAt = System.currentTimeMillis();
        }
        
        // Getters and setters
        public String getId() {
            return id;
        }
        
        public String getName() {
            return name;
        }
        
        public List<Map<String, Object>> getRequests() {
            return requests;
        }
        
        public void addRequest(Map<String, Object> request) {
            this.requests.add(request);
        }
        
        public List<Map<String, Object>> getResponses() {
            return responses;
        }
        
        public void setResponses(List<Map<String, Object>> responses) {
            this.responses = responses;
        }
        
        public boolean isCompleted() {
            return completed;
        }
        
        public void setCompleted(boolean completed) {
            this.completed = completed;
        }
        
        public boolean isCancelled() {
            return cancelled;
        }
        
        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }
        
        public long getCreatedAt() {
            return createdAt;
        }
        
        public long getCompletedAt() {
            return completedAt;
        }
        
        public void setCompletedAt(long completedAt) {
            this.completedAt = completedAt;
        }
        
        public int getRequestCount() {
            return requests.size();
        }
        
        public int getResponseCount() {
            return responses.size();
        }
    }
}
