package org.example.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for QueryOptimizationService
 */
public class QueryOptimizationServiceTest {
    
    private QueryOptimizationService service;
    
    @BeforeEach
    public void setUp() {
        service = new QueryOptimizationService();
    }
    
    @Test
    public void testRecordQueryExecution() {
        service.recordQueryExecution("query1", "SELECT * FROM users", 50, 100);
        
        QueryOptimizationService.QueryProfile profile = service.getQueryProfile("query1");
        assertNotNull(profile);
        assertEquals("query1", profile.queryId);
        assertEquals(1, profile.executionCount);
    }
    
    @Test
    public void testRecordMultipleExecutions() {
        for (int i = 0; i < 10; i++) {
            service.recordQueryExecution("query1", "SELECT * FROM users", 50 + i, 100);
        }
        
        QueryOptimizationService.QueryProfile profile = service.getQueryProfile("query1");
        assertEquals(10, profile.executionCount);
    }
    
    @Test
    public void testQueryProfileStats() {
        service.recordQueryExecution("query1", "SELECT * FROM users", 100, 50);
        service.recordQueryExecution("query1", "SELECT * FROM users", 200, 75);
        service.recordQueryExecution("query1", "SELECT * FROM users", 150, 60);
        
        QueryOptimizationService.QueryProfile profile = service.getQueryProfile("query1");
        
        assertEquals(3, profile.executionCount);
        assertEquals(100, profile.minExecutionTimeMs);
        assertEquals(200, profile.maxExecutionTimeMs);
        assertTrue(profile.avgExecutionTimeMs > 100 && profile.avgExecutionTimeMs < 200);
    }
    
    @Test
    public void testDetectSlowQueries() {
        service.recordQueryExecution("fast_query", "SELECT * FROM users WHERE id = 1", 100, 1);
        service.recordQueryExecution("slow_query", "SELECT * FROM users", 2000, 1000);
        
        List<QueryOptimizationService.QueryProfile> slowQueries = service.getSlowQueries();
        
        assertTrue(slowQueries.size() > 0);
        assertTrue(slowQueries.stream().anyMatch(q -> q.queryId.equals("slow_query")));
    }
    
    @Test
    public void testGetAllQueryProfiles() {
        service.recordQueryExecution("query1", "SELECT * FROM users", 50, 10);
        service.recordQueryExecution("query2", "SELECT * FROM posts", 60, 20);
        service.recordQueryExecution("query3", "SELECT * FROM comments", 70, 30);
        
        Collection<QueryOptimizationService.QueryProfile> profiles = service.getAllQueryProfiles();
        assertEquals(3, profiles.size());
    }
    
    @Test
    public void testIndexRecommendation() {
        service.recommendIndex("users", "email", "BTREE", "Frequently filtered column");
        service.recommendIndex("posts", "user_id", "BTREE", "Foreign key column");
        
        List<QueryOptimizationService.IndexRecommendation> recommendations = service.getIndexRecommendations();
        assertEquals(2, recommendations.size());
    }
    
    @Test
    public void testIndexRecommendationDetails() {
        service.recommendIndex("users", "email", "BTREE", "Email lookups");
        
        List<QueryOptimizationService.IndexRecommendation> recommendations = service.getIndexRecommendations();
        
        assertNotNull(recommendations);
        assertEquals(1, recommendations.size());
        assertEquals("users", recommendations.get(0).tableName);
        assertEquals("email", recommendations.get(0).columnName);
    }
    
    @Test
    public void testClearRecommendations() {
        service.recommendIndex("users", "email", "BTREE", "test");
        service.recommendIndex("posts", "id", "HASH", "test");
        
        assertEquals(2, service.getIndexRecommendations().size());
        
        service.clearIndexRecommendations();
        
        assertEquals(0, service.getIndexRecommendations().size());
    }
    
    @Test
    public void testOptimizationReport() {
        service.recordQueryExecution("query1", "SELECT * FROM users", 50, 10);
        service.recordQueryExecution("query2", "SELECT * FROM users", 2000, 100);
        service.recommendIndex("users", "name", "BTREE", "test");
        
        Map<String, Object> report = service.generateOptimizationReport();
        
        assertNotNull(report);
        assertTrue(report.containsKey("totalQueries"));
        assertTrue(report.containsKey("slowQueriesCount"));
        assertTrue(report.containsKey("indexRecommendations"));
        assertTrue(report.containsKey("generatedAt"));
    }
    
    @Test
    public void testSlowQueryCountIncrement() {
        service.recordQueryExecution("query1", "SELECT * FROM users", 500, 10);
        service.recordQueryExecution("query1", "SELECT * FROM users", 1500, 10); // Slow
        service.recordQueryExecution("query1", "SELECT * FROM users", 2000, 10); // Slow
        
        QueryOptimizationService.QueryProfile profile = service.getQueryProfile("query1");
        assertEquals(2, profile.slowQueryCount);
    }
    
    @Test
    public void testQueriesNeedingOptimization() {
        // Record a fast query
        service.recordQueryExecution("fast", "SELECT id FROM users WHERE id = 1", 50, 1);
        
        // Record a slow query
        service.recordQueryExecution("slow", "SELECT * FROM users", 1500, 100);
        
        List<QueryOptimizationService.QueryProfile> needsOpt = service.getQueriesNeedingOptimization();
        
        assertTrue(needsOpt.size() > 0);
        assertTrue(needsOpt.stream().anyMatch(q -> q.queryId.equals("slow")));
    }
    
    @Test
    public void testTotalResultCount() {
        service.recordQueryExecution("query1", "SELECT * FROM users", 100, 50);
        service.recordQueryExecution("query1", "SELECT * FROM users", 100, 75);
        
        QueryOptimizationService.QueryProfile profile = service.getQueryProfile("query1");
        assertEquals(125, profile.totalResultCount);
    }
    
    @Test
    public void testQueryProfileLastExecutedTime() {
        service.recordQueryExecution("query1", "SELECT * FROM users", 100, 10);
        
        QueryOptimizationService.QueryProfile profile = service.getQueryProfile("query1");
        assertNotNull(profile.lastExecutedAt);
        assertTrue(profile.lastExecutedAt <= System.currentTimeMillis());
    }
    
    @Test
    public void testMultipleSlowQueries() {
        service.recordQueryExecution("slow1", "SELECT * FROM users", 2000, 100);
        service.recordQueryExecution("slow2", "SELECT * FROM posts", 3000, 200);
        service.recordQueryExecution("slow3", "SELECT * FROM comments", 1500, 150);
        
        List<QueryOptimizationService.QueryProfile> slowQueries = service.getSlowQueries();
        assertEquals(3, slowQueries.size());
    }
    
    @Test
    public void testQueryProfileMinMaxExecutionTime() {
        service.recordQueryExecution("query", "SELECT * FROM users", 100, 10);
        service.recordQueryExecution("query", "SELECT * FROM users", 500, 10);
        service.recordQueryExecution("query", "SELECT * FROM users", 250, 10);
        
        QueryOptimizationService.QueryProfile profile = service.getQueryProfile("query");
        assertEquals(100, profile.minExecutionTimeMs);
        assertEquals(500, profile.maxExecutionTimeMs);
    }
}
