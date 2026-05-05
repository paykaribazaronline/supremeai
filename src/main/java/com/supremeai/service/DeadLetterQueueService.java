package com.supremeai.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Dead letter queue service for failed requests with retry scheduling.
 * Stores failed requests and schedules retries with exponential backoff.
 */
@Service
public class DeadLetterQueueService {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterQueueService.class);
    private static final String DLQ_KEY_PREFIX = "dlq:request:";
    private static final String DLQ_SCHEDULED_KEY = "dlq:scheduled";

    private final StringRedisTemplate redisTemplate;
    private final ScheduledExecutorService scheduler;

    public DeadLetterQueueService(StringRedisTemplate redisTemplate, 
                                 ScheduledExecutorService scheduler) {
        this.redisTemplate = redisTemplate;
        this.scheduler = scheduler;
    }

    /**
     * Queue a failed request for retry.
     */
    public void queueFailedRequest(String requestId, 
                                    String provider, 
                                    String requestData, 
                                    String errorMessage,
                                    int retryCount) {
        FailedRequest failedRequest = new FailedRequest(
            requestId,
            provider,
            requestData,
            errorMessage,
            retryCount,
            Instant.now()
        );

        String key = DLQ_KEY_PREFIX + requestId;
        redisTemplate.opsForValue().set(key, serialize(failedRequest));
        
        // Schedule retry
        scheduleRetry(requestId, retryCount);
        
        log.info("Queued failed request {} for provider {} (retry #{})", 
                 requestId, provider, retryCount);
    }

    /**
     * Get all failed requests.
     */
    public List<FailedRequest> getAllFailedRequests() {
        // This would typically use Redis keys and values
        // For simplicity, returning empty list - implement based on actual needs
        return List.of();
    }

    /**
     * Process a failed request (retry).
     */
    public CompletableFuture<Boolean> processFailedRequest(String requestId, 
                                                           java.util.function.Supplier<Boolean> retryOperation) {
        return CompletableFuture.supplyAsync(() -> {
            String key = DLQ_KEY_PREFIX + requestId;
            String value = redisTemplate.opsForValue().get(key);
            
            if (value == null) {
                return false;
            }

            try {
                FailedRequest request = deserialize(value);
                boolean success = retryOperation.get();
                
                if (success) {
                    // Remove from DLQ on success
                    redisTemplate.delete(key);
                    log.info("Successfully retried request {}", requestId);
                } else {
                    // Re-queue with incremented retry count
                    if (request.retryCount < 5) {
                        queueFailedRequest(
                            requestId,
                            request.provider,
                            request.requestData,
                            "Retry failed",
                            request.retryCount + 1
                        );
                    } else {
                        log.error("Max retries exceeded for request {}", requestId);
                    }
                }
                
                return success;
            } catch (Exception e) {
                log.error("Error processing failed request {}", requestId, e);
                return false;
            }
        });
    }

    /**
     * Schedule a retry with exponential backoff.
     */
    private void scheduleRetry(String requestId, int retryCount) {
        // Calculate delay: 1min, 2min, 4min, 8min, 16min
        long delayMinutes = (long) Math.pow(2, Math.min(retryCount, 4));
        
        scheduler.schedule(() -> {
            // Move to scheduled queue for processing
            redisTemplate.opsForSet().add(DLQ_SCHEDULED_KEY, requestId);
            log.debug("Scheduled retry for request {} in {} minutes", requestId, delayMinutes);
        }, delayMinutes, TimeUnit.MINUTES);
    }

    /**
     * Get scheduled requests ready for retry.
     */
    public List<String> getScheduledRequests() {
        return redisTemplate.opsForSet().members(DLQ_SCHEDULED_KEY)
            .stream()
            .toList();
    }

    /**
     * Clear a request from the scheduled queue.
     */
    public void clearScheduledRequest(String requestId) {
        redisTemplate.opsForSet().remove(DLQ_SCHEDULED_KEY, requestId);
    }

    // Simple serialization (in production, use Jackson)
    private String serialize(FailedRequest request) {
        return String.format("%s|%s|%s|%s|%d|%s",
            request.requestId,
            request.provider,
            request.requestData,
            request.errorMessage,
            request.retryCount,
            request.timestamp.toString()
        );
    }

    private FailedRequest deserialize(String data) {
        String[] parts = data.split("\\|", 6);
        return new FailedRequest(
            parts[0],
            parts[1],
            parts[2],
            parts[3],
            Integer.parseInt(parts[4]),
            Instant.parse(parts[5])
        );
    }

    /**
     * Failed request record.
     */
    public static class FailedRequest {
        public final String requestId;
        public final String provider;
        public final String requestData;
        public final String errorMessage;
        public final int retryCount;
        public final Instant timestamp;

        @JsonCreator
        public FailedRequest(
                @JsonProperty("requestId") String requestId,
                @JsonProperty("provider") String provider,
                @JsonProperty("requestData") String requestData,
                @JsonProperty("errorMessage") String errorMessage,
                @JsonProperty("retryCount") int retryCount,
                @JsonProperty("timestamp") Instant timestamp) {
            this.requestId = requestId;
            this.provider = provider;
            this.requestData = requestData;
            this.errorMessage = errorMessage;
            this.retryCount = retryCount;
            this.timestamp = timestamp;
        }
    }
}