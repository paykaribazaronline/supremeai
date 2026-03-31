package org.example.service;

import java.util.*;
import java.util.concurrent.*;

/**
 * Connection Pool Service
 * Manages database connection pooling and utilization
 */
public class ConnectionPoolService {
    
    private final String poolName;
    private final int initialSize;
    private final int maxSize;
    private final long maxIdleTimeMs;
    
    private final BlockingQueue<DbConnection> availableConnections;
    private final Set<DbConnection> allConnections;
    private long totalConnectionsCreated = 0;
    private long totalConnectionsReused = 0;
    private long waitTimeMs = 0;
    
    public ConnectionPoolService(String poolName, int initialSize, int maxSize, long maxIdleTimeMs) {
        this.poolName = poolName;
        this.initialSize = initialSize;
        this.maxSize = maxSize;
        this.maxIdleTimeMs = maxIdleTimeMs;
        
        this.availableConnections = new LinkedBlockingQueue<>(maxSize);
        this.allConnections = ConcurrentHashMap.newKeySet();
        
        // Initialize pool
        for (int i = 0; i < initialSize; i++) {
            DbConnection conn = new DbConnection(UUID.randomUUID().toString());
            availableConnections.offer(conn);
            allConnections.add(conn);
            totalConnectionsCreated++;
        }
    }
    
    /**
     * Get a connection from pool
     */
    public DbConnection getConnection(long timeoutMs) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        DbConnection conn = availableConnections.poll(timeoutMs, TimeUnit.MILLISECONDS);
        
        if (conn == null) {
            // Try to create new connection if pool not full
            if (allConnections.size() < maxSize) {
                conn = new DbConnection(UUID.randomUUID().toString());
                allConnections.add(conn);
                totalConnectionsCreated++;
            } else {
                throw new RuntimeException("Connection pool exhausted");
            }
        } else {
            totalConnectionsReused++;
        }
        
        if (conn != null) {
            conn.acquire();
            waitTimeMs += (System.currentTimeMillis() - startTime);
        }
        
        return conn;
    }
    
    /**
     * Return connection to pool
     */
    public void releaseConnection(DbConnection conn) {
        if (conn != null && allConnections.contains(conn)) {
            conn.release();
            
            // Check if connection is healthy before adding back to pool
            if (conn.isHealthy() && !conn.isExpired(maxIdleTimeMs)) {
                availableConnections.offer(conn);
            } else {
                // Remove unhealthy connection
                allConnections.remove(conn);
                conn.close();
            }
        }
    }
    
    /**
     * Get pool statistics
     */
    public PoolStats getStats() {
        int activeConnections = (int) allConnections.stream()
                .filter(DbConnection::isActive)
                .count();
        
        int idleConnections = availableConnections.size();
        
        return new PoolStats(
                poolName,
                allConnections.size(),
                activeConnections,
                idleConnections,
                totalConnectionsCreated,
                totalConnectionsReused,
                maxSize,
                availableConnections.size() == 0 ? 100 : 
                    (int) (((float) availableConnections.size() / maxSize) * 100)
        );
    }
    
    /**
     * Validate pool
     */
    public void validateConnections() {
        List<DbConnection> toRemove = new ArrayList<>();
        
        for (DbConnection conn : allConnections) {
            if (!conn.isHealthy() || conn.isExpired(maxIdleTimeMs)) {
                toRemove.add(conn);
                availableConnections.remove(conn);
            }
        }
        
        toRemove.forEach(conn -> {
            allConnections.remove(conn);
            conn.close();
        });
        
        // Replenish if below initial size
        while (allConnections.size() < initialSize) {
            DbConnection conn = new DbConnection(UUID.randomUUID().toString());
            availableConnections.offer(conn);
            allConnections.add(conn);
            totalConnectionsCreated++;
        }
    }
    
    /**
     * Shutdown pool
     */
    public void shutdown() {
        for (DbConnection conn : allConnections) {
            conn.close();
        }
        allConnections.clear();
        availableConnections.clear();
    }
    
    /**
     * Database Connection
     */
    public static class DbConnection {
        public String connectionId;
        private boolean active = false;
        private long lastUsedTime = System.currentTimeMillis();
        private long createdAt = System.currentTimeMillis();
        private boolean healthy = true;
        private int queryCount = 0;
        
        public DbConnection(String connectionId) {
            this.connectionId = connectionId;
        }
        
        public void acquire() {
            active = true;
        }
        
        public void release() {
            active = false;
            lastUsedTime = System.currentTimeMillis();
        }
        
        public boolean isActive() {
            return active;
        }
        
        public boolean isHealthy() {
            return healthy;
        }
        
        public void setHealthy(boolean healthy) {
            this.healthy = healthy;
        }
        
        public boolean isExpired(long maxIdleTimeMs) {
            return !active && (System.currentTimeMillis() - lastUsedTime) > maxIdleTimeMs;
        }
        
        public void recordQuery() {
            queryCount++;
        }
        
        public int getQueryCount() {
            return queryCount;
        }
        
        public void close() {
            active = false;
            healthy = false;
        }
        
        public long getConnectionAge() {
            return System.currentTimeMillis() - createdAt;
        }
    }
    
    /**
     * Pool Statistics
     */
    public static class PoolStats {
        public String poolName;
        public int totalConnections;
        public int activeConnections;
        public int idleConnections;
        public long totalConnectionsCreated;
        public long totalConnectionsReused;
        public int maxSize;
        public int utilizationPercent;
        
        public PoolStats(String poolName, int totalConnections, int activeConnections, 
                        int idleConnections, long totalConnectionsCreated, long totalConnectionsReused,
                        int maxSize, int utilizationPercent) {
            this.poolName = poolName;
            this.totalConnections = totalConnections;
            this.activeConnections = activeConnections;
            this.idleConnections = idleConnections;
            this.totalConnectionsCreated = totalConnectionsCreated;
            this.totalConnectionsReused = totalConnectionsReused;
            this.maxSize = maxSize;
            this.utilizationPercent = utilizationPercent;
        }
    }
}
