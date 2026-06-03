package com.supremeai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages active simulator runtime sessions.
 * Tracks session metadata, heartbeat, and provides lookup.
 */
@Service
public class SimulatorSessionService {

    private static final Logger logger = LoggerFactory.getLogger(SimulatorSessionService.class);

    private final ConcurrentHashMap<String, RuntimeSession> sessions = new ConcurrentHashMap<>();

    private final AtomicLong sessionCounter = new AtomicLong(0);

    /**
     * Register a new session.
     */
    public String registerSession(String appId, String deviceType) {
        String sessionId = "sess_" + System.currentTimeMillis() + "_" + sessionCounter.incrementAndGet();
        RuntimeSession session = new RuntimeSession(sessionId, appId, deviceType);
        sessions.put(sessionId, session);
        logger.info("[SESSION] Registered new session: {} for app={} device={}", sessionId, appId, deviceType);
        return sessionId;
    }

    /**
     * Unregister a session (cleanup).
     */
    public void unregisterSession(String sessionId) {
        RuntimeSession removed = sessions.remove(sessionId);
        if (removed != null) {
            logger.info("[SESSION] Unregistered session: {}", sessionId);
        }
    }

    /**
     * Check if session is active.
     */
    public boolean isActive(String sessionId) {
        RuntimeSession session = sessions.get(sessionId);
        if (session == null) return false;

        // Check expiration (30 minutes idle)
        if (System.currentTimeMillis() - session.getLastHeartbeat() > 30 * 60 * 1000) {
            sessions.remove(sessionId);
            return false;
        }
        return true;
    }

    /**
     * Refresh heartbeat for a session.
     */
    public void refreshHeartbeat(String sessionId) {
        RuntimeSession session = sessions.get(sessionId);
        if (session != null) {
            session.refreshHeartbeat();
        }
    }

    /**
     * Get session info.
     */
    public RuntimeSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * Get all active sessions count.
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }

    /**
     * Cleanup expired sessions (call periodically).
     */
    public void cleanupExpired() {
        long now = System.currentTimeMillis();
        sessions.entrySet().removeIf(entry -> {
            RuntimeSession s = entry.getValue();
            return now - s.getLastHeartbeat() > 30 * 60 * 1000; // 30 min TTL
        });
    }

    /**
     * Inner model representing an active runtime session.
     */
    public static class RuntimeSession {
        private final String sessionId;
        private final String appId;
        private final String deviceType;
        private final long createdAt;
        private volatile long lastHeartbeat;

        public SimulatorSessionService(String sessionId, String appId, String deviceType) {
            this.sessionId = sessionId;
            this.appId = appId;
            this.deviceType = deviceType;
            this.createdAt = System.currentTimeMillis();
            this.lastHeartbeat = createdAt;
        }

        public void refreshHeartbeat() {
            this.lastHeartbeat = System.currentTimeMillis();
        }

        public String getSessionId() { return sessionId; }
        public String getAppId() { return appId; }
        public String getDeviceType() { return deviceType; }
        public long getCreatedAt() { return createdAt; }
        public long getLastHeartbeat() { return lastHeartbeat; }
    }
}
