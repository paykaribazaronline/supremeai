package org.example.controller;

import org.example.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Performance Optimization Controller
 * REST endpoints for query optimization and connection pooling
 */
@RestController
@RequestMapping("/api/performance-optimization")
public class PerformanceOptimizationController {
    
    @Autowired
    private QueryOptimizationService queryOptimizationService;
    
    @Autowired
    private ConnectionPoolService connectionPoolService;
    
    // ============ Query Optimization Endpoints ============
    
    /**
     * Record query execution
     */
    @PostMapping("/queries/execute")
    public ResponseEntity<Map<String, String>> recordQueryExecution(
            @RequestParam String queryId,
            @RequestParam String sql,
            @RequestParam long executionTimeMs,
            @RequestParam int resultCount) {
        queryOptimizationService.recordQueryExecution(queryId, sql, executionTimeMs, resultCount);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "queryId", queryId,
                "executionTime", Long.toString(executionTimeMs)
        ));
    }
    
    /**
     * Get query profile
     */
    @GetMapping("/queries/{queryId}")
    public ResponseEntity<Map<String, Object>> getQueryProfile(@PathVariable String queryId) {
        QueryOptimizationService.QueryProfile profile = queryOptimizationService.getQueryProfile(queryId);
        
        if (profile == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("queryId", profile.queryId);
        response.put("sql", profile.sql);
        response.put("executionCount", profile.executionCount);
        response.put("avgExecutionTimeMs", profile.avgExecutionTimeMs);
        response.put("minExecutionTimeMs", profile.minExecutionTimeMs);
        response.put("maxExecutionTimeMs", profile.maxExecutionTimeMs);
        response.put("slowQueryCount", profile.slowQueryCount);
        response.put("totalResultCount", profile.totalResultCount);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get all queries
     */
    @GetMapping("/queries")
    public ResponseEntity<Map<String, Object>> getAllQueries() {
        var profiles = queryOptimizationService.getAllQueryProfiles();
        
        List<Map<String, Object>> queries = new ArrayList<>();
        for (QueryOptimizationService.QueryProfile profile : profiles) {
            Map<String, Object> q = new HashMap<>();
            q.put("queryId", profile.queryId);
            q.put("executionCount", profile.executionCount);
            q.put("avgExecutionTimeMs", profile.avgExecutionTimeMs);
            q.put("slowQueryCount", profile.slowQueryCount);
            queries.add(q);
        }
        
        return ResponseEntity.ok(Map.of("queries", queries, "count", queries.size()));
    }
    
    /**
     * Get slow queries
     */
    @GetMapping("/queries/slow")
    public ResponseEntity<Map<String, Object>> getSlowQueries() {
        var slowQueries = queryOptimizationService.getSlowQueries();
        
        List<Map<String, Object>> queries = new ArrayList<>();
        for (QueryOptimizationService.QueryProfile profile : slowQueries) {
            Map<String, Object> q = new HashMap<>();
            q.put("queryId", profile.queryId);
            q.put("avgExecutionTimeMs", profile.avgExecutionTimeMs);
            q.put("executionCount", profile.executionCount);
            q.put("slowQueryCount", profile.slowQueryCount);
            queries.add(q);
        }
        
        return ResponseEntity.ok(Map.of("slowQueries", queries, "count", queries.size()));
    }
    
    /**
     * Get queries needing optimization
     */
    @GetMapping("/queries/optimization-needed")
    public ResponseEntity<Map<String, Object>> getQueriesNeedingOptimization() {
        var needsOpt = queryOptimizationService.getQueriesNeedingOptimization();
        
        List<Map<String, Object>> queries = new ArrayList<>();
        for (QueryOptimizationService.QueryProfile profile : needsOpt) {
            Map<String, Object> q = new HashMap<>();
            q.put("queryId", profile.queryId);
            q.put("slowQueryCount", profile.slowQueryCount);
            q.put("executionCount", profile.executionCount);
            queries.add(q);
        }
        
        return ResponseEntity.ok(Map.of("queriesNeedingOptimization", queries, "count", queries.size()));
    }
    
    /**
     * Recommend index
     */
    @PostMapping("/indexes/recommend")
    public ResponseEntity<Map<String, String>> recommendIndex(
            @RequestParam String tableName,
            @RequestParam String columnName,
            @RequestParam String indexType,
            @RequestParam String reason) {
        queryOptimizationService.recommendIndex(tableName, columnName, indexType, reason);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "tableName", tableName,
                "columnName", columnName
        ));
    }
    
    /**
     * Get index recommendations
     */
    @GetMapping("/indexes/recommendations")
    public ResponseEntity<Map<String, Object>> getIndexRecommendations() {
        var recommendations = queryOptimizationService.getIndexRecommendations();
        
        List<Map<String, Object>> recs = new ArrayList<>();
        for (QueryOptimizationService.IndexRecommendation rec : recommendations) {
            Map<String, Object> r = new HashMap<>();
            r.put("tableName", rec.tableName);
            r.put("columnName", rec.columnName);
            r.put("indexType", rec.indexType);
            r.put("reason", rec.reason);
            recs.add(r);
        }
        
        return ResponseEntity.ok(Map.of("recommendations", recs, "count", recs.size()));
    }
    
    /**
     * Generate optimization report
     */
    @GetMapping("/queries/report")
    public ResponseEntity<Map<String, Object>> generateOptimizationReport() {
        return ResponseEntity.ok(queryOptimizationService.generateOptimizationReport());
    }
    
    // ============ Connection Pool Endpoints ============
    
    /**
     * Get pool statistics
     */
    @GetMapping("/connection-pool/stats")
    public ResponseEntity<Map<String, Object>> getPoolStats() {
        ConnectionPoolService.PoolStats stats = connectionPoolService.getStats();
        
        Map<String, Object> response = new HashMap<>();
        response.put("poolName", stats.poolName);
        response.put("totalConnections", stats.totalConnections);
        response.put("activeConnections", stats.activeConnections);
        response.put("idleConnections", stats.idleConnections);
        response.put("totalConnectionsCreated", stats.totalConnectionsCreated);
        response.put("totalConnectionsReused", stats.totalConnectionsReused);
        response.put("maxSize", stats.maxSize);
        response.put("utilizationPercent", stats.utilizationPercent);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Validate pool connections
     */
    @PostMapping("/connection-pool/validate")
    public ResponseEntity<Map<String, Object>> validatePoolConnections() {
        connectionPoolService.validateConnections();
        
        ConnectionPoolService.PoolStats stats = connectionPoolService.getStats();
        
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "totalConnections", stats.totalConnections,
                "activeConnections", stats.activeConnections
        ));
    }
}
