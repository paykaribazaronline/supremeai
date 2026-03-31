package org.example.service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Query Optimization Service
 * Database query performance tuning and optimization
 */
public class QueryOptimizationService {
    
    private final Map<String, QueryProfile> queryProfiles = new ConcurrentHashMap<>();
    private final List<IndexRecommendation> indexRecommendations = Collections.synchronizedList(new ArrayList<>());
    private final long slowQueryThresholdMs = 1000; // 1 second
    
    /**
     * Record query execution
     */
    public void recordQueryExecution(String queryId, String sql, long executionTimeMs, int resultCount) {
        QueryProfile profile = queryProfiles.computeIfAbsent(
                queryId,
                k -> new QueryProfile(queryId, sql)
        );
        
        profile.recordExecution(executionTimeMs, resultCount);
        
        // Detect slow queries
        if (executionTimeMs > slowQueryThresholdMs) {
            profile.incrementSlowQueryCount();
        }
    }
    
    /**
     * Get query profile
     */
    public QueryProfile getQueryProfile(String queryId) {
        return queryProfiles.get(queryId);
    }
    
    /**
     * List all query profiles
     */
    public Collection<QueryProfile> getAllQueryProfiles() {
        return new ArrayList<>(queryProfiles.values());
    }
    
    /**
     * Get slow queries
     */
    public List<QueryProfile> getSlowQueries() {
        return queryProfiles.values().stream()
                .filter(p -> p.avgExecutionTimeMs > slowQueryThresholdMs)
                .sorted((a, b) -> Double.compare(b.avgExecutionTimeMs, a.avgExecutionTimeMs))
                .collect(Collectors.toList());
    }
    
    /**
     * Get queries needing optimization
     */
    public List<QueryProfile> getQueriesNeedingOptimization() {
        return queryProfiles.values().stream()
                .filter(p -> p.slowQueryCount > 0 || p.executionCount > 1000)
                .sorted((a, b) -> Integer.compare(b.slowQueryCount, a.slowQueryCount))
                .collect(Collectors.toList());
    }
    
    /**
     * Add index recommendation
     */
    public void recommendIndex(String tableName, String columnName, String indexType, String reason) {
        indexRecommendations.add(new IndexRecommendation(
                tableName,
                columnName,
                indexType,
                reason,
                System.currentTimeMillis()
        ));
    }
    
    /**
     * Get index recommendations
     */
    public List<IndexRecommendation> getIndexRecommendations() {
        return new ArrayList<>(indexRecommendations);
    }
    
    /**
     * Clear index recommendations
     */
    public void clearIndexRecommendations() {
        indexRecommendations.clear();
    }
    
    /**
     * Generate optimization report
     */
    public Map<String, Object> generateOptimizationReport() {
        Map<String, Object> report = new HashMap<>();
        
        List<QueryProfile> slowQueries = getSlowQueries();
        List<QueryProfile> needsOptimization = getQueriesNeedingOptimization();
        
        report.put("totalQueries", queryProfiles.size());
        report.put("slowQueriesCount", slowQueries.size());
        report.put("queriesNeedingOptimization", needsOptimization.size());
        report.put("indexRecommendations", indexRecommendations.size());
        
        // Query statistics
        List<Map<String, Object>> slowQueryDetails = new ArrayList<>();
        for (QueryProfile profile : slowQueries.stream().limit(10).collect(Collectors.toList())) {
            Map<String, Object> detail = new HashMap<>();
            detail.put("queryId", profile.queryId);
            detail.put("avgExecutionTime", profile.avgExecutionTimeMs);
            detail.put("executionCount", profile.executionCount);
            detail.put("slowQueryCount", profile.slowQueryCount);
            slowQueryDetails.add(detail);
        }
        report.put("slowQueryDetails", slowQueryDetails);
        
        report.put("generatedAt", System.currentTimeMillis());
        return report;
    }
    
    /**
     * Query Profile
     */
    public static class QueryProfile {
        public String queryId;
        public String sql;
        public long executionCount = 0;
        public long totalExecutionTimeMs = 0;
        public double avgExecutionTimeMs = 0;
        public long minExecutionTimeMs = Long.MAX_VALUE;
        public long maxExecutionTimeMs = Long.MIN_VALUE;
        public long totalResultCount = 0;
        public int slowQueryCount = 0;
        public long lastExecutedAt = 0;
        
        public QueryProfile(String queryId, String sql) {
            this.queryId = queryId;
            this.sql = sql;
        }
        
        public void recordExecution(long executionTimeMs, int resultCount) {
            executionCount++;
            totalExecutionTimeMs += executionTimeMs;
            avgExecutionTimeMs = (double) totalExecutionTimeMs / executionCount;
            minExecutionTimeMs = Math.min(minExecutionTimeMs, executionTimeMs);
            maxExecutionTimeMs = Math.max(maxExecutionTimeMs, executionTimeMs);
            totalResultCount += resultCount;
            lastExecutedAt = System.currentTimeMillis();
        }
        
        public void incrementSlowQueryCount() {
            slowQueryCount++;
        }
    }
    
    /**
     * Index Recommendation
     */
    public static class IndexRecommendation {
        public String tableName;
        public String columnName;
        public String indexType; // BTREE, HASH, etc.
        public String reason;
        public long recommendedAt;
        
        public IndexRecommendation(String tableName, String columnName, String indexType, String reason, long recommendedAt) {
            this.tableName = tableName;
            this.columnName = columnName;
            this.indexType = indexType;
            this.reason = reason;
            this.recommendedAt = recommendedAt;
        }
    }
}
