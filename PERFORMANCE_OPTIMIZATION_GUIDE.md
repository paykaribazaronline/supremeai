# Performance Optimization - Complete Guide

## Overview

The Performance Optimization module provides tools for improving application performance through query optimization, connection pooling, and intelligent caching strategies. Designed to reduce latency, improve throughput, and optimize resource utilization.

## Architecture Components

### 1. QueryOptimizationService
**Purpose**: Database query performance monitoring and optimization recommendations

**Key Features**:
- Query execution tracking and profiling
- Slow query detection (>1 second threshold)
- Performance statistics calculation (min, max, average)
- Database index recommendations
- Automatic optimization suggestions

**Methods**:
```java
// Query tracking
void recordQueryExecution(String queryId, String sql, long executionTimeMs, int resultCount)

// Query analysis
QueryProfile getQueryProfile(String queryId)
Collection<QueryProfile> getAllQueryProfiles()
List<QueryProfile> getSlowQueries()
List<QueryProfile> getQueriesNeedingOptimization()

// Index management
void recommendIndex(String tableName, String columnName, String indexType, String reason)
List<IndexRecommendation> getIndexRecommendations()

// Reporting
Map<String, Object> generateOptimizationReport()
```

**Query Profile Model**:
```json
{
  "queryId": "query-123",
  "sql": "SELECT * FROM users WHERE email = ?",
  "executionCount": 1000,
  "avgExecutionTimeMs": 45.5,
  "minExecutionTimeMs": 10,
  "maxExecutionTimeMs": 200,
  "totalResultCount": 1000,
  "slowQueryCount": 5,
  "lastExecutedAt": 1711900000000
}
```

**Index Recommendation Model**:
```json
{
  "tableName": "users",
  "columnName": "email",
  "indexType": "BTREE",
  "reason": "Frequently filtered column for login queries",
  "recommendedAt": 1711900000000
}
```

### 2. ConnectionPoolService
**Purpose**: Database connection pooling and lifecycle management

**Key Features**:
- Connection pool initialization and management
- Dynamic connection creation up to max size
- Connection validation and health checking
- LRU (Least Recently Used) eviction policy
- Automatic idle connection cleanup
- Connection reuse tracking

**Methods**:
```java
// Connection management
DbConnection getConnection(long timeoutMs) throws InterruptedException
void releaseConnection(DbConnection conn)

// Pool maintenance
PoolStats getStats()
void validateConnections()
void shutdown()
```

**Pool Statistics Model**:
```json
{
  "poolName": "default-pool",
  "totalConnections": 8,
  "activeConnections": 3,
  "idleConnections": 5,
  "totalConnectionsCreated": 10,
  "totalConnectionsReused": 450,
  "maxSize": 20,
  "utilizationPercent": 40
}
```

**DbConnection Model**:
```json
{
  "connectionId": "conn-uuid",
  "active": true,
  "healthy": true,
  "queryCount": 45,
  "connectionAge": 5000,
  "lastUsedTime": 1711900000000
}
```

### 3. CacheService
**Purpose**: In-memory caching with TTL and eviction policies

**Key Features**:
- Multi-level caching with configurable TTL
- LRU (Least Recently Used) eviction when full
- Automatic expiration handling
- Cache hit/miss statistics
- Thread-safe operations with read-write locks
- Cache invalidation of expired entries

**Methods**:
```java
// Cache operations
void put(String key, Object value)
void put(String key, Object value, long ttlMs)
Object get(String key)
boolean containsKey(String key)
void remove(String key)
void clear()

// Statistics and maintenance
CacheStats getStats()
int invalidateExpired()
```

**Cache Statistics Model**:
```json
{
  "currentSize": 245,
  "hitCount": 5000,
  "missCount": 1200,
  "hitRate": "80.65%",
  "maxSize": 10000
}
```

## REST API Endpoints

### Query Optimization Endpoints

#### Record Query Execution
```
POST /api/performance-optimization/queries/execute
?queryId=query-123&sql=SELECT%20*%20FROM%20users&executionTimeMs=45&resultCount=100

Response:
{
  "status": "success",
  "queryId": "query-123",
  "executionTime": "45"
}
```

#### Get Query Profile
```
GET /api/performance-optimization/queries/{queryId}

Response:
{
  "queryId": "query-123",
  "sql": "SELECT * FROM users",
  "executionCount": 1000,
  "avgExecutionTimeMs": 45.5,
  "minExecutionTimeMs": 10,
  "maxExecutionTimeMs": 200,
  "slowQueryCount": 5
}
```

#### List All Queries
```
GET /api/performance-optimization/queries

Response:
{
  "queries": [...],
  "count": 42
}
```

#### Get Slow Queries
```
GET /api/performance-optimization/queries/slow

Response:
{
  "slowQueries": [
    {
      "queryId": "slow-query-1",
      "avgExecutionTimeMs": 2500.0,
      "executionCount": 150,
      "slowQueryCount": 145
    }
  ],
  "count": 5
}
```

#### Get Queries Needing Optimization
```
GET /api/performance-optimization/queries/optimization-needed

Response:
{
  "queriesNeedingOptimization": [...],
  "count": 8
}
```

#### Recommend Index
```
POST /api/performance-optimization/indexes/recommend
?tableName=users&columnName=email&indexType=BTREE&reason=Email%20lookups

Response:
{
  "status": "success",
  "tableName": "users",
  "columnName": "email"
}
```

#### Get Index Recommendations
```
GET /api/performance-optimization/indexes/recommendations

Response:
{
  "recommendations": [
    {
      "tableName": "users",
      "columnName": "email",
      "indexType": "BTREE",
      "reason": "Frequently filtered column"
    }
  ],
  "count": 3
}
```

#### Generate Optimization Report
```
GET /api/performance-optimization/queries/report

Response:
{
  "totalQueries": 50,
  "slowQueriesCount": 8,
  "queriesNeedingOptimization": 12,
  "indexRecommendations": 5,
  "slowQueryDetails": [...],
  "generatedAt": 1711900000000
}
```

### Connection Pool Endpoints

#### Get Pool Statistics
```
GET /api/performance-optimization/connection-pool/stats

Response:
{
  "poolName": "default-pool",
  "totalConnections": 8,
  "activeConnections": 3,
  "idleConnections": 5,
  "totalConnectionsCreated": 10,
  "totalConnectionsReused": 450,
  "maxSize": 20,
  "utilizationPercent": 40
}
```

#### Validate Pool Connections
```
POST /api/performance-optimization/connection-pool/validate

Response:
{
  "status": "success",
  "totalConnections": 5,
  "activeConnections": 0
}
```

### Cache Endpoints

#### Put Value in Cache
```
POST /api/performance-optimization/cache/put
?key=user:123&value={"name":"John","email":"john@example.com"}&ttlMs=300000

Response:
{
  "status": "success",
  "key": "user:123"
}
```

#### Get Value from Cache
```
GET /api/performance-optimization/cache/get/{key}

Response (Hit):
{
  "status": "hit",
  "key": "user:123",
  "value": "{\"name\":\"John\"}"
}

Response (Miss):
{
  "status": "miss",
  "key": "user:123",
  "value": null
}
```

#### Check Cache Key Exists
```
GET /api/performance-optimization/cache/contains/{key}

Response:
{
  "key": "user:123",
  "exists": true
}
```

#### Remove from Cache
```
DELETE /api/performance-optimization/cache/{key}

Response:
{
  "status": "success",
  "key": "user:123"
}
```

#### Clear All Cache
```
POST /api/performance-optimization/cache/clear

Response:
{
  "status": "success"
}
```

#### Get Cache Statistics
```
GET /api/performance-optimization/cache/stats

Response:
{
  "currentSize": 245,
  "hitCount": 5000,
  "missCount": 1200,
  "hitRate": "80.65%",
  "maxSize": 10000
}
```

#### Invalidate Expired Entries
```
POST /api/performance-optimization/cache/invalidate-expired

Response:
{
  "status": "success",
  "invalidatedCount": 12
}
```

## Performance Best Practices

### Query Optimization
1. **Monitor Slow Queries**: Regularly check slow query reports
2. **Create Indexes**: Act on index recommendations for frequently filtered columns
3. **Use Query Profiling**: Record execution metrics for all database queries
4. **Optimize WHERE Clauses**: Index columns used in WHERE conditions
5. **Batch Operations**: Group multiple statements when possible
6. **Cache Results**: Use CacheService for expensive queries with stable results

### Connection Pooling
1. **Pool Sizing**: Set initial size to expected concurrent users
2. **Max Size**: Set max size to 2-3x initial size for spikes
3. **Idle Timeout**: Configure reasonable idle timeout (e.g., 5 minutes)
4. **Regular Validation**: Schedule periodic pool validation
5. **Monitor Utilization**: Keep utilization below 80% under normal load
6. **Graceful Degradation**: Scale connections as load increases

### Caching Strategy
1. **Identify Hot Data**: Cache frequently accessed, infrequently changing data
2. **Set Appropriate TTL**: Balance freshness vs. cache efficiency
3. **Monitor Hit Rate**: Aim for >80% cache hit rate
4. **Avoid Cache Stampede**: Use cache-aside pattern with timeout
5. **Invalidate On Update**: Clear cache when underlying data changes
6. **Size Limits**: Monitor cache size to prevent memory issues

## Usage Examples

### Example 1: Track Database Query Performance

```java
// Record query execution
queryOptimizer.recordQueryExecution(
    "user-lookup",
    "SELECT * FROM users WHERE email = ?",
    45,  // execution time in ms
    1    // result count
);

// Get profile to identify slow queries
QueryOptimizationService.QueryProfile profile = queryOptimizer.getQueryProfile("user-lookup");

if (profile.avgExecutionTimeMs > 100) {
    // Recommend index on email column
    queryOptimizer.recommendIndex("users", "email", "BTREE", "Email lookup optimization");
}

// Get optimization report
Map<String, Object> report = queryOptimizer.generateOptimizationReport();
```

### Example 2: Manage Connection Pool

```java
// Get connection
ConnectionPoolService.DbConnection conn = pool.getConnection(5000); // 5 second timeout

try {
    // Execute queries
    conn.recordQuery();
    conn.recordQuery();
} finally {
    // Always release connection back to pool
    pool.releaseConnection(conn);
}

// Monitor pool health
ConnectionPoolService.PoolStats stats = pool.getStats();
if (stats.utilizationPercent > 80) {
    System.out.println("Connection pool approaching capacity");
}

// Regular maintenance
pool.validateConnections(); // Remove unhealthy or expired connections
```

### Example 3: Implement Smart Caching

```java
// Try to get from cache first
Object cachedUser = cache.get("user:123");

if (cachedUser == null) {
    // Cache miss - fetch from database
    User user = database.getUserById(123);
    
    // Cache for 5 minutes
    cache.put("user:123", user, 300000);
    cachedUser = user;
}

// Use cached result
User result = (User) cachedUser;

// Monitor cache effectiveness
CacheService.CacheStats stats = cache.getStats();
double hitRate = stats.hitRate * 100;
System.out.println("Cache hit rate: " + hitRate + "%");
```

## Configuration

### CacheService Properties
```properties
# Cache configuration
cache.maxSize=10000
cache.defaultTtlMs=300000  # 5 minutes
cache.evictionPolicy=LRU
```

### ConnectionPoolService Properties
```properties
# Connection pool configuration
pool.initialSize=5
pool.maxSize=20
pool.maxIdleTimeMs=300000  # 5 minutes
pool.timeout=5000  # 5 seconds
```

### QueryOptimizationService Properties
```properties
# Query optimization configuration
query.slowQueryThresholdMs=1000  # 1 second
query.enableProfiling=true
```

## Monitoring & Metrics

### Key Metrics to Monitor

**Query Performance**:
- Average query execution time
- 95th percentile latency
- Slow query count
- Query throughput (queries/sec)

**Connection Pool**:
- Active connections
- Idle connections
- Pool utilization percentage
- Connection creation rate
- Connection wait time

**Cache**:
- Hit rate (target: >80%)
- Miss rate
- Cache size
- Eviction rate

## Testing

All optimization services include comprehensive test suites:
- **QueryOptimizationServiceTest**: 16 tests
- **ConnectionPoolServiceTest**: 18 tests
- **PerformanceOptimizationController**: Integrated tests

Run tests with:
```bash
./gradlew test --tests "*Optimization*"
```

## Troubleshooting

### Slow Queries
1. Check query execution times via API
2. Review slow query log
3. Create recommended indexes
4. Analyze query execution plans

### Connection Pool Issues
- Pool exhaustion: Increase max size or reduce timeout
- Stale connections: Enable automatic validation
- Memory leaks: Ensure all connections are released

### Low Cache Hit Rate
- Verify data is being cached
- Check TTL is not too aggressive
- Monitor for cache invalidation frequency
- Verify cache size is sufficient

## Performance Targets

| Metric | Target | Action |
|--------|--------|--------|
| Average query time | <100ms | Optimize slow queries |
| 95th percentile latency | <500ms | Add indexes, check pool size |
| Cache hit rate | >80% | Review TTL strategy |
| Connection pool util | <80% | Scale based on load |
| Cache memory usage | <50% of max | Monitor eviction rate |

## Version History

- v1.0.0 (April 2026): Initial release
  - Query optimization and profiling
  - Connection pooling with health checks
  - Multi-level caching with TTL
  - Complete REST API integration
