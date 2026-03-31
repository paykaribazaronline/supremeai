package org.example.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ConnectionPoolService
 */
public class ConnectionPoolServiceTest {
    
    private ConnectionPoolService pool;
    
    @BeforeEach
    public void setUp() {
        pool = new ConnectionPoolService("test-pool", 5, 10, 300000);
    }
    
    @Test
    public void testPoolInitialization() {
        ConnectionPoolService.PoolStats stats = pool.getStats();
        
        assertNotNull(stats);
        assertEquals(5, stats.totalConnections);
        assertEquals(0, stats.activeConnections);
        assertEquals(5, stats.idleConnections);
    }
    
    @Test
    public void testGetConnection() throws InterruptedException {
        ConnectionPoolService.DbConnection conn = pool.getConnection(1000);
        
        assertNotNull(conn);
        assertTrue(conn.isActive());
        assertNotNull(conn.connectionId);
    }
    
    @Test
    public void testReleaseConnection() throws InterruptedException {
        ConnectionPoolService.DbConnection conn = pool.getConnection(1000);
        
        assertTrue(conn.isActive());
        
        pool.releaseConnection(conn);
        
        assertFalse(conn.isActive());
    }
    
    @Test
    public void testMultipleConnections() throws InterruptedException {
        ConnectionPoolService.DbConnection conn1 = pool.getConnection(1000);
        ConnectionPoolService.DbConnection conn2 = pool.getConnection(1000);
        ConnectionPoolService.DbConnection conn3 = pool.getConnection(1000);
        
        assertNotNull(conn1);
        assertNotNull(conn2);
        assertNotNull(conn3);
        
        assertNotEquals(conn1.connectionId, conn2.connectionId);
        assertNotEquals(conn2.connectionId, conn3.connectionId);
    }
    
    @Test
    public void testPoolExhaustion() throws InterruptedException {
        // Acquire all connections
        for (int i = 0; i < 10; i++) {
            pool.getConnection(1000);
        }
        
        // Try to exceed max size
        assertThrows(RuntimeException.class, () -> pool.getConnection(100));
    }
    
    @Test
    public void testConnectionReuse() throws InterruptedException {
        ConnectionPoolService.DbConnection conn1 = pool.getConnection(1000);
        String connectionId = conn1.connectionId;
        
        pool.releaseConnection(conn1);
        
        ConnectionPoolService.DbConnection conn2 = pool.getConnection(1000);
        
        // May reuse same connection
        assertNotNull(conn2);
    }
    
    @Test
    public void testPoolStats() throws InterruptedException {
        pool.getConnection(1000);
        pool.getConnection(1000);
        
        ConnectionPoolService.PoolStats stats = pool.getStats();
        
        assertNotNull(stats);
        assertEquals(2, stats.activeConnections);
        assertTrue(stats.utilizationPercent > 0);
    }
    
    @Test
    public void testConnectionHealthCheck() throws InterruptedException {
        ConnectionPoolService.DbConnection conn = pool.getConnection(1000);
        
        assertTrue(conn.isHealthy());
        
        conn.setHealthy(false);
        
        assertFalse(conn.isHealthy());
    }
    
    @Test
    public void testConnectionAge() throws InterruptedException {
        ConnectionPoolService.DbConnection conn = pool.getConnection(1000);
        
        long age = conn.getConnectionAge();
        assertTrue(age >= 0);
    }
    
    @Test
    public void testRecordQuery() throws InterruptedException {
        ConnectionPoolService.DbConnection conn = pool.getConnection(1000);
        
        assertEquals(0, conn.getQueryCount());
        
        conn.recordQuery();
        conn.recordQuery();
        
        assertEquals(2, conn.getQueryCount());
    }
    
    @Test
    public void testValidateConnections() throws InterruptedException {
        ConnectionPoolService.DbConnection conn = pool.getConnection(1000);
        
        conn.setHealthy(false);
        
        pool.releaseConnection(conn);
        pool.validateConnections();
        
        // Pool should be restored to initial size
        ConnectionPoolService.PoolStats stats = pool.getStats();
        assertEquals(5, stats.totalConnections);
    }
    
    @Test
    public void testConnectionAcquireRelease() throws InterruptedException {
        ConnectionPoolService.DbConnection conn = pool.getConnection(1000);
        
        assertTrue(conn.isActive());
        conn.release();
        assertFalse(conn.isActive());
        
        conn.acquire();
        assertTrue(conn.isActive());
    }
    
    @Test
    public void testPoolShutdown() throws InterruptedException {
        ConnectionPoolService.DbConnection conn = pool.getConnection(1000);
        
        pool.shutdown();
        
        ConnectionPoolService.PoolStats stats = pool.getStats();
        assertEquals(0, stats.totalConnections);
    }
    
    @Test
    public void testTotalConnectionsCreated() throws InterruptedException {
        ConnectionPoolService.PoolStats statsInitial = pool.getStats();
        long initialCreated = statsInitial.totalConnectionsCreated;
        
        // Exhaust and try to get more (will create new ones)
        for (int i = 0; i < 8; i++) {
            pool.getConnection(1000);
        }
        
        ConnectionPoolService.PoolStats stats = pool.getStats();
        assertTrue(stats.totalConnectionsCreated >= initialCreated + 3);
    }
    
    @Test
    public void testIdleConnections() throws InterruptedException {
        ConnectionPoolService.DbConnection conn1 = pool.getConnection(1000);
        ConnectionPoolService.DbConnection conn2 = pool.getConnection(1000);
        
        pool.releaseConnection(conn1);
        pool.releaseConnection(conn2);
        
        ConnectionPoolService.PoolStats stats = pool.getStats();
        assertTrue(stats.idleConnections > 0);
    }
    
    @Test
    public void testConnectionClose() throws InterruptedException {
        ConnectionPoolService.DbConnection conn = pool.getConnection(1000);
        
        assertTrue(conn.isHealthy());
        
        conn.close();
        
        assertFalse(conn.isHealthy());
        assertFalse(conn.isActive());
    }
}
