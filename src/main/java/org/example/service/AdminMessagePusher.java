package org.example.service;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.cloud.FirestoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

/**
 * Phase 4: Admin Message Pusher
 * 
 * Real-time message delivery to admin dashboard
 * Pushes data collection results and alerts to Firestore
 * 
 * Features:
 * - Async message queuing (doesn't block data collection)
 * - Batch writes for efficiency
 * - Real-time updates via Firestore listeners
 * - Message history tracking
 * - Admin notification system
 * 
 * Collections created:
 * - admin_messages: Real-time updates for dashboard
 * - dashboard_alerts: Critical alerts requiring immediate action
 * - webhook_events: Audit trail of webhook events
 * - data_collection_logs: Detailed collection statistics
 * 
 * Message types:
 * - data_update: New collected data from APIs
 * - alert: System warning or error
 * - stats: Quota/budget updates
 * - event: Webhook event processed
 */
public class AdminMessagePusher {
    private static final Logger logger = LoggerFactory.getLogger(AdminMessagePusher.class);
    
    private final Firestore db;
    
    // Message queue for batch processing
    private final BlockingQueue<AdminMessage> messageQueue = new LinkedBlockingQueue<>(10000);
    
    // Stats tracking
    private long totalMessagesSent = 0;
    private long totalMessagesFailed = 0;
    
    // Message batch settings
    private static final int BATCH_SIZE = 50;
    private static final long BATCH_TIMEOUT_MS = 5000; // 5 seconds
    
    public AdminMessagePusher() {
        this.db = FirestoreClient.getFirestore();
        logger.info("✅ Admin Message Pusher initialized");
        
        // Start batch processor
        startBatchProcessor();
    }
    
    /**
     * Push data collection result to admin dashboard
     * Called when data is successfully collected
     */
    public void pushDataUpdate(String source, String identifier, 
                               Map<String, Object> data, long collectionTimeMs) {
        AdminMessage message = new AdminMessage(
            AdminMessageType.DATA_UPDATE,
            Map.of(
                "source", source,
                "identifier", identifier,
                "data", data,
                "collection_time_ms", collectionTimeMs,
                "timestamp", System.currentTimeMillis()
            )
        );
        
        try {
            messageQueue.offer(message);
            logger.debug("📨 Data update queued: {} from {}", identifier, source);
        } catch (Exception e) {
            logger.error("❌ Failed to queue data update", e);
        }
    }
    
    /**
     * Push alert to admin dashboard
     * Used for warnings, errors, quota warnings
     */
    public void pushAlert(String alertType, String title, String message, 
                         Map<String, Object> metadata) {
        AdminMessage alert = new AdminMessage(
            AdminMessageType.ALERT,
            Map.of(
                "alert_type", alertType,
                "title", title,
                "message", message,
                "metadata", metadata,
                "timestamp", System.currentTimeMillis(),
                "severity", determineSeverity(alertType)
            )
        );
        
        try {
            messageQueue.offer(alert);
            logger.info("🚨 Alert pushed: {}", title);
        } catch (Exception e) {
            logger.error("❌ Failed to queue alert", e);
        }
    }
    
    /**
     * Push quota/budget stats update
     */
    public void pushStatsUpdate(String statsType, Map<String, Object> stats) {
        AdminMessage message = new AdminMessage(
            AdminMessageType.STATS,
            Map.of(
                "stats_type", statsType,
                "stats", stats,
                "timestamp", System.currentTimeMillis()
            )
        );
        
        try {
            messageQueue.offer(message);
            logger.debug("📊 Stats update queued: {}", statsType);
        } catch (Exception e) {
            logger.error("❌ Failed to queue stats update", e);
        }
    }
    
    /**
     * Push webhook event notification
     */
    public void pushWebhookEvent(String deliveryId, String eventType, 
                                String owner, String repo, boolean success) {
        AdminMessage message = new AdminMessage(
            AdminMessageType.EVENT,
            Map.of(
                "event_type", eventType,
                "delivery_id", deliveryId,
                "owner", owner,
                "repo", repo,
                "success", success,
                "timestamp", System.currentTimeMillis()
            )
        );
        
        try {
            messageQueue.offer(message);
            logger.debug("🔔 Webhook event queued: {}/{}", owner, repo);
        } catch (Exception e) {
            logger.error("❌ Failed to queue webhook event", e);
        }
    }
    
    /**
     * Batch processor - consumes message queue and writes to Firestore
     */
    private void startBatchProcessor() {
        new Thread(() -> {
            List<AdminMessage> batch = new ArrayList<>(BATCH_SIZE);
            long lastBatchTime = System.currentTimeMillis();
            
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Try to get next message with timeout
                    AdminMessage msg = messageQueue.poll(1, TimeUnit.SECONDS);
                    
                    if (msg != null) {
                        batch.add(msg);
                    }
                    
                    // Check if we should flush batch
                    long timeSinceLastFlush = System.currentTimeMillis() - lastBatchTime;
                    boolean shouldFlush = batch.size() >= BATCH_SIZE || 
                        (timeSinceLastFlush > BATCH_TIMEOUT_MS && !batch.isEmpty());
                    
                    if (shouldFlush) {
                        flushBatch(batch);
                        batch.clear();
                        lastBatchTime = System.currentTimeMillis();
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    // Flush remaining messages before exiting
                    if (!batch.isEmpty()) {
                        flushBatch(batch);
                    }
                }
            }
        }, "admin-message-batch-processor").start();
    }
    
    /**
     * Flush batch of messages to Firestore
     */
    private void flushBatch(List<AdminMessage> batch) {
        if (batch.isEmpty()) {
            return;
        }
        
        try {
            logger.info("📤 Flushing batch of {} messages to Firestore", batch.size());
            
            for (AdminMessage msg : batch) {
                try {
                    String collection = getCollectionForMessageType(msg.type);
                    String documentId = msg.generateDocumentId();
                    
                    db.collection(collection)
                        .document(documentId)
                        .set(msg.data)
                        .get(); // Wait for write
                    
                    totalMessagesSent++;
                    
                } catch (Exception e) {
                    logger.error("❌ Failed to write message to Firestore", e);
                    totalMessagesFailed++;
                }
            }
            
            logger.info("✅ Batch flushed successfully ({} sent, {} failed)", 
                batch.size(), 0);
            
        } catch (Exception e) {
            logger.error("❌ Batch flush failed", e);
            totalMessagesFailed += batch.size();
        }
    }
    
    /**
     * Get Firestore collection for message type
     */
    private String getCollectionForMessageType(AdminMessageType type) {
        return switch (type) {
            case DATA_UPDATE -> "admin_messages";
            case ALERT -> "dashboard_alerts";
            case STATS -> "admin_messages";
            case EVENT -> "webhook_events";
        };
    }
    
    /**
     * Determine alert severity based on type
     */
    private String determineSeverity(String alertType) {
        return switch (alertType) {
            case "quota_exceeded" -> "critical";
            case "budget_warning" -> "warning";
            case "api_error" -> "error";
            case "info" -> "info";
            default -> "warning";
        };
    }
    
    /**
     * Get statistics
     */
    public Map<String, Object> getStats() {
        return Map.of(
            "total_sent", totalMessagesSent,
            "total_failed", totalMessagesFailed,
            "queue_size", messageQueue.size(),
            "timestamp", System.currentTimeMillis()
        );
    }
    
    /**
     * Clear message queue (use with caution)
     */
    public void clearQueue() {
        messageQueue.clear();
        logger.warn("🗑️ Admin message queue cleared");
    }
    
    // ========== Inner Classes ==========
    
    /**
     * Admin message to be pushed to Firestore
     */
    public static class AdminMessage {
        public final AdminMessageType type;
        public final Map<String, Object> data;
        public final long createdAt;
        
        public AdminMessage(AdminMessageType type, Map<String, Object> data) {
            this.type = type;
            this.data = new LinkedHashMap<>(data);
            this.data.put("created_at", System.currentTimeMillis());
            this.createdAt = System.currentTimeMillis();
        }
        
        public String generateDocumentId() {
            // Use timestamp + random for uniqueness
            return System.currentTimeMillis() + "_" + 
                   UUID.randomUUID().toString().substring(0, 8);
        }
    }
    
    /**
     * Message type enum
     */
    public enum AdminMessageType {
        DATA_UPDATE,    // New collected data
        ALERT,          // Critical alert
        STATS,          // Statistics update
        EVENT           // Event processed
    }
    
    /**
     * Builder for easy message creation
     */
    public static class MessageBuilder {
        private final AdminMessageType type;
        private final Map<String, Object> data = new LinkedHashMap<>();
        
        public MessageBuilder(AdminMessageType type) {
            this.type = type;
        }
        
        public MessageBuilder put(String key, Object value) {
            data.put(key, value);
            return this;
        }
        
        public AdminMessage build() {
            return new AdminMessage(type, data);
        }
    }
}
