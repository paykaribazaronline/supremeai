package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/**
 * Error DLQ Service - Phase 1 Optimization (#7)
 * Dead-Letter Queue for error tracking with probabilistic sampling:
 * - Logs ALL errors to in-memory queue (fast)
 * - Writes 10% sample to Firebase (cost-effective)
 * - Statistical analysis of error patterns
 * - Automatic cleanup of old entries
 * 
 * Benefits:
 * - Full visibility into errors
 * - Only 10% Firebase writes (~$0.05/mo)
 * - Still captures patterns statistically
 * - Prevents data loss during failures
 */
@Service
public class ErrorDLQService {
    private static final Logger logger = LoggerFactory.getLogger(ErrorDLQService.class);
    
    @Autowired(required = false)
    private FirebaseService firebaseService;
    
    // Configuration
    private static final int MAX_QUEUE_SIZE = 10_000;
    private static final long RETENTION_MS = 24 * 60 * 60 * 1000L; // 24 hours
    private static final double FIREBASE_WRITE_SAMPLE_RATE = 0.10; // 10%
    private static final long CLEANUP_INTERVAL_MS = 3 * 60 * 60 * 1000L; // 3 hours
    
    public static class ErrorEvent {
        public String id;
        public String errorType;
        public String message;
        public String exceptionClass;
        public String stackTrace;
        public String source; // Service that reported the error
        public Map<String, String> context; // Additional context
        public LocalDateTime timestamp;
        public boolean writtenToFirebase;
        
        public ErrorEvent(String errorType, String message, String source) {
            this.id = UUID.randomUUID().toString();
            this.errorType = errorType;
            this.message = message;
            this.source = source;
            this.timestamp = LocalDateTime.now();
            this.context = new HashMap<>();
            this.writtenToFirebase = false;
        }
    }
    
    private final Queue<ErrorEvent> dlq = new ConcurrentLinkedQueue<>();
    private final BlockingQueue<ErrorEvent> firebaseWriteQueue = new LinkedBlockingQueue<>();
    private final Map<String, ErrorStats> errorStats = new ConcurrentHashMap<>();
    
    private final ScheduledExecutorService executor = 
        Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "ErrorDLQ-" + Thread.currentThread().getName());
            t.setDaemon(true);
            return t;
        });
    
    public static class ErrorStats {
        public String errorType;
        public int count;
        public int firebaseWrites;
        public long firstOccurrence;
        public long lastOccurrence;
        public Set<String> sources;
        
        public ErrorStats(String errorType) {
            this.errorType = errorType;
            this.count = 0;
            this.firebaseWrites = 0;
            this.firstOccurrence = System.currentTimeMillis();
            this.lastOccurrence = System.currentTimeMillis();
            this.sources = ConcurrentHashMap.newKeySet();
        }
    }
    
    private int totalErrors = 0;
    private int totalFirebaseWrites = 0;
    
    public ErrorDLQService() {
        // Start background tasks
        executor.scheduleAtFixedRate(
            this::cleanupOldEntries,
            CLEANUP_INTERVAL_MS,
            CLEANUP_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        );
        
        executor.scheduleAtFixedRate(
            this::flushToFirebase,
            5_000, // Start after 5 seconds
            30_000, // Every 30 seconds
            TimeUnit.MILLISECONDS
        );
    }
    
    /**
     * Log an error
     */
    public void logError(String errorType, String message, String source) {
        ErrorEvent event = new ErrorEvent(errorType, message, source);
        logError(event);
    }
    
    /**
     * Log an error with exception details
     */
    public void logError(String errorType, String message, Throwable throwable, String source) {
        ErrorEvent event = new ErrorEvent(errorType, message, source);
        
        if (throwable != null) {
            event.exceptionClass = throwable.getClass().getName();
            event.stackTrace = getStackTrace(throwable);
        }
        
        logError(event);
    }
    
    /**
     * Log an error with context
     */
    private void logError(ErrorEvent event) {
        // Add to DLQ
        if (dlq.size() >= MAX_QUEUE_SIZE) {
            dlq.poll(); // Remove oldest
        }
        dlq.offer(event);
        totalErrors++;
        
        // Update statistics
        ErrorStats stats = errorStats.computeIfAbsent(
            event.errorType, 
            ErrorStats::new
        );
        stats.count++;
        stats.lastOccurrence = System.currentTimeMillis();
        stats.sources.add(event.source);
        
        logger.debug("📋 Error logged: {} (total: {})", event.errorType, totalErrors);
        
        // Probabilistically decide if we should write to Firebase
        if (shouldWriteToFirebase()) {
            firebaseWriteQueue.offer(event);
            logger.debug("📤 Queued for Firebase: {}", event.id);
        }
    }
    
    /**
     * Flush errors to Firebase (10% sample)
     */
    private void flushToFirebase() {
        if (firebaseService == null || !firebaseService.isInitialized()) {
            return;
        }
        
        List<ErrorEvent> toWrite = new ArrayList<>();
        firebaseWriteQueue.drainTo(toWrite);
        
        if (toWrite.isEmpty()) {
            return;
        }
        
        logger.debug("💾 Flushing {} errors to Firebase", toWrite.size());
        
        for (ErrorEvent event : toWrite) {
            try {
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("id", event.id);
                errorData.put("timestamp", event.timestamp.toString());
                errorData.put("type", event.errorType);
                errorData.put("message", event.message);
                errorData.put("source", event.source);
                
                if (event.exceptionClass != null) {
                    errorData.put("exceptionClass", event.exceptionClass);
                    errorData.put("stackTrace", event.stackTrace);
                }
                
                if (!event.context.isEmpty()) {
                    errorData.put("context", event.context);
                }
                
                // Write to Firebase under error DLQ
                firebaseService.saveSystemConfig("error-dlq/" + event.id, errorData);
                
                event.writtenToFirebase = true;
                totalFirebaseWrites++;
                
                ErrorStats stats = errorStats.get(event.errorType);
                if (stats != null) {
                    stats.firebaseWrites++;
                }
                
                logger.debug("✅ Error {} written to Firebase", event.id);
                
            } catch (Exception e) {
                logger.warn("❌ Failed to write error to Firebase: {}", e.getMessage());
                // Re-queue for retry
                firebaseWriteQueue.offer(event);
            }
        }
    }
    
    /**
     * Clean up old entries (24h retention)
     */
    private void cleanupOldEntries() {
        long cutoffTime = System.currentTimeMillis() - RETENTION_MS;
        int removed = 0;
        
        // Can't iterate and remove from ConcurrentLinkedQueue safely
        // So we'll rebuild it
        List<ErrorEvent> fresh = new ArrayList<>();
        ErrorEvent event;
        while ((event = dlq.poll()) != null) {
            // Convert LocalDateTime to long milliseconds for comparison
            long eventTime = java.time.ZonedDateTime.of(
                event.timestamp, 
                java.time.ZoneId.systemDefault()
            ).toInstant().toEpochMilli();
            
            if (eventTime > cutoffTime) {
                fresh.add(event);
            } else {
                removed++;
            }
        }
        
        // Re-add fresh entries
        fresh.forEach(dlq::offer);
        
        logger.debug("🧹 Cleaned {} old error entries (retention: {}h)", 
            removed, RETENTION_MS / (60 * 60 * 1000));
    }
    
    /**
     * Get DLQ statistics
     */
    public Map<String, Object> getStats() {
        return Map.ofEntries(
            Map.entry("totalErrors", totalErrors),
            Map.entry("totalFirebaseWrites", totalFirebaseWrites),
            Map.entry("firebaseSampleRate", FIREBASE_WRITE_SAMPLE_RATE * 100 + "%"),
            Map.entry("currentQueueSize", dlq.size()),
            Map.entry("pendingFirebaseWrites", firebaseWriteQueue.size()),
            Map.entry("uniqueErrorTypes", errorStats.size()),
            Map.entry("estimatedMonthlyFirebaseCost", String.format("$%.2f", totalFirebaseWrites * 0.18 / 100_000)),
            Map.entry("errorsByType", new TreeMap<>(
                errorStats.entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        e -> Map.ofEntries(
                            Map.entry("count", e.getValue().count),
                            Map.entry("firebaseWrites", e.getValue().firebaseWrites),
                            Map.entry("sources", e.getValue().sources)
                        )
                    ))
            ))
        );
    }
    
    /**
     * Get recent errors (for debugging)
     */
    public List<ErrorEvent> getRecentErrors(int limit) {
        List<ErrorEvent> result = new ArrayList<>(dlq);
        // Most recent first
        result.sort((a, b) -> b.timestamp.compareTo(a.timestamp));
        return result.stream().limit(limit).toList();
    }
    
    /**
     * Probabilistic Firebase write decision (10%)
     */
    private boolean shouldWriteToFirebase() {
        return Math.random() < FIREBASE_WRITE_SAMPLE_RATE;
    }
    
    /**
     * Get stack trace as string
     */
    private String getStackTrace(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString();
    }
}
