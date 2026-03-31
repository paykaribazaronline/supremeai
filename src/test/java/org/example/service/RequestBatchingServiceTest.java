package org.example.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Request Batching Service Test
 * Tests batch creation, request aggregation, and execution
 */
@DisplayName("Request Batching Service Tests")
public class RequestBatchingServiceTest {
    
    private RequestBatchingService batchingService;
    
    @BeforeEach
    public void setUp() {
        batchingService = new RequestBatchingService();
    }
    
    @Test
    @DisplayName("Create batch operation")
    public void testCreateBatch() {
        String batchId = batchingService.createBatch("test-batch");
        
        assertNotNull(batchId);
        assertNotNull(batchingService.getBatch(batchId));
    }
    
    @Test
    @DisplayName("Add requests to batch")
    public void testAddRequestToBatch() {
        String batchId = batchingService.createBatch("test-batch");
        
        Map<String, Object> request1 = Map.of("id", 1, "name", "request1");
        Map<String, Object> request2 = Map.of("id", 2, "name", "request2");
        
        batchingService.addRequestToBatch(batchId, request1);
        batchingService.addRequestToBatch(batchId, request2);
        
        RequestBatchingService.BatchOperation batch = batchingService.getBatch(batchId);
        assertEquals(2, batch.getRequestCount());
    }
    
    @Test
    @DisplayName("Execute batch returns all responses")
    public void testExecuteBatch() {
        String batchId = batchingService.createBatch("test-batch");
        
        batchingService.addRequestToBatch(batchId, Map.of("id", 1, "type", "project"));
        batchingService.addRequestToBatch(batchId, Map.of("id", 2, "type", "agent"));
        
        Map<String, Object> result = batchingService.executeBatch(batchId);
        
        assertEquals(batchId, result.get("batchId"));
        assertEquals("completed", result.get("status"));
        assertEquals(2, result.get("requestCount"));
    }
    
    @Test
    @DisplayName("Execute batch marks batch as completed")
    public void testBatchCompletionFlag() {
        String batchId = batchingService.createBatch("test-batch");
        batchingService.addRequestToBatch(batchId, Map.of("id", 1));
        
        batchingService.executeBatch(batchId);
        
        RequestBatchingService.BatchOperation batch = batchingService.getBatch(batchId);
        assertTrue(batch.isCompleted());
    }
    
    @Test
    @DisplayName("Cannot execute already completed batch")
    public void testCannotExecuteCompletedBatch() {
        String batchId = batchingService.createBatch("test-batch");
        batchingService.addRequestToBatch(batchId, Map.of("id", 1));
        
        batchingService.executeBatch(batchId);
        Map<String, Object> result = batchingService.executeBatch(batchId);
        
        assertEquals("Batch already completed", result.get("error"));
    }
    
    @Test
    @DisplayName("Get non-existent batch returns null")
    public void testGetNonExistentBatch() {
        RequestBatchingService.BatchOperation batch = batchingService.getBatch("non-existent");
        assertNull(batch);
    }
    
    @Test
    @DisplayName("Cancel batch operation")
    public void testCancelBatch() {
        String batchId = batchingService.createBatch("test-batch");
        batchingService.addRequestToBatch(batchId, Map.of("id", 1));
        
        batchingService.cancelBatch(batchId);
        
        RequestBatchingService.BatchOperation batch = batchingService.getBatch(batchId);
        assertTrue(batch.isCompleted());
        assertTrue(batch.isCancelled());
    }
    
    @Test
    @DisplayName("List batches returns all batches")
    public void testListBatches() {
        batchingService.createBatch("batch-1");
        batchingService.createBatch("batch-2");
        batchingService.createBatch("batch-3");
        
        List<RequestBatchingService.BatchOperation> batches = batchingService.listBatches();
        
        assertTrue(batches.size() >= 3);
    }
    
    @Test
    @DisplayName("Clear completed batches")
    public void testClearCompletedBatches() {
        String batchId1 = batchingService.createBatch("batch-1");
        String batchId2 = batchingService.createBatch("batch-2");
        
        batchingService.addRequestToBatch(batchId1, Map.of("id", 1));
        batchingService.addRequestToBatch(batchId2, Map.of("id", 2));
        
        batchingService.executeBatch(batchId1);
        
        int cleared = batchingService.clearCompletedBatches();
        
        assertTrue(cleared >= 1);
        assertNull(batchingService.getBatch(batchId1));
    }
    
    @Test
    @DisplayName("Batch tracks creation time")
    public void testBatchCreationTime() {
        String batchId = batchingService.createBatch("test-batch");
        RequestBatchingService.BatchOperation batch = batchingService.getBatch(batchId);
        
        assertTrue(batch.getCreatedAt() > 0);
        assertTrue(batch.getCreatedAt() <= System.currentTimeMillis());
    }
    
    @Test
    @DisplayName("Batch tracks completion time")
    public void testBatchCompletionTime() {
        String batchId = batchingService.createBatch("test-batch");
        batchingService.addRequestToBatch(batchId, Map.of("id", 1));
        
        batchingService.executeBatch(batchId);
        
        RequestBatchingService.BatchOperation batch = batchingService.getBatch(batchId);
        assertTrue(batch.getCompletedAt() > 0);
        assertTrue(batch.getCompletedAt() >= batch.getCreatedAt());
    }
    
    @Test
    @DisplayName("Multiple requests in batch are all processed")
    public void testMultipleRequestProcessing() {
        String batchId = batchingService.createBatch("test-batch");
        
        for (int i = 0; i < 5; i++) {
            batchingService.addRequestToBatch(batchId, Map.of("id", i, "index", i));
        }
        
        batchingService.executeBatch(batchId);
        
        RequestBatchingService.BatchOperation batch = batchingService.getBatch(batchId);
        assertEquals(5, batch.getRequestCount());
        assertEquals(5, batch.getResponseCount());
    }
    
    @Test
    @DisplayName("Batch responses contain processed flag")
    public void testBatchResponsesProcessed() {
        String batchId = batchingService.createBatch("test-batch");
        batchingService.addRequestToBatch(batchId, Map.of("id", 1));
        
        batchingService.executeBatch(batchId);
        
        RequestBatchingService.BatchOperation batch = batchingService.getBatch(batchId);
        List<Map<String, Object>> responses = batch.getResponses();
        
        assertTrue(responses.size() > 0);
        assertTrue((boolean) responses.get(0).get("processed"));
    }
}
