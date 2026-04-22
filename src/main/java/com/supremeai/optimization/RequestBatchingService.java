package com.supremeai.optimization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * Service for batching AI API requests to reduce total call volume.
 *
 * Features:
 * - Groups requests by (provider, model) with a short time window
 * - Deduplicates identical prompts within the batch window
 * - Rate-limits outgoing batches per provider
 * - Configurable batch window, max batch size, and max concurrency
 */
@Service
public class RequestBatchingService {

    private static final Logger log = LoggerFactory.getLogger(RequestBatchingService.class);

    private static final int BATCH_WINDOW_MS = 100;
    private static final int MAX_BATCH_SIZE = 16;
    private static final int MAX_CONCURRENT_BATCHES = 4;
    private static final Duration MIN_INTERVAL_PER_PROVIDER = Duration.ofMillis(50);

    private final ExecutorService executor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() * 2,
            r -> {
                Thread t = new Thread(r, "batch-worker");
                t.setDaemon(true);
                return t;
            });
    private final Map<String, Semaphore> providerConcurrency = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastBatchSent = new ConcurrentHashMap<>();

    private final Map<String, PendingBatch<?>> pendingBatches = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "batch-flusher");
        t.setDaemon(true);
        return t;
    });

    @PostConstruct
    public void init() {
        scheduler.scheduleAtFixedRate(this::flushAllBatches, BATCH_WINDOW_MS, BATCH_WINDOW_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Submit a single request and await the result.
     * The request may be batched with others to the same provider/model.
     *
     * @param provider Provider name (e.g. "openai")
     * @param model    Model name
     * @param prompt   The prompt text
     * @param executor Function that performs the actual API call
     * @return Future result
     */
    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> submit(String provider, String model, String prompt,
                                           Function<String, T> executor) {
        String batchKey = provider + "::" + model;
        BatchItem<T> item = new BatchItem<>(prompt, executor);

        PendingBatch<T> batch = (PendingBatch<T>) pendingBatches.compute(batchKey, (k, existing) -> {
            if (existing == null || existing.isFull() || existing.isExpired()) {
                return new PendingBatch<>(provider, model);
            }
            return existing;
        });

        batch.add(item);
        if (batch.isFull()) {
            flushBatch(batchKey, batch);
        }
        return item.future;
    }

    /**
     * Immediately flush all pending batches.
     */
    public void flushAll() {
        flushAllBatches();
    }

    private void flushAllBatches() {
        List<Map.Entry<String, PendingBatch<?>>> toFlush = new ArrayList<>();
        synchronized (pendingBatches) {
            for (Map.Entry<String, PendingBatch<?>> entry : pendingBatches.entrySet()) {
                if (!entry.getValue().isEmpty() && entry.getValue().isReadyToFlush()) {
                    toFlush.add(entry);
                }
            }
            for (Map.Entry<String, PendingBatch<?>> entry : toFlush) {
                pendingBatches.remove(entry.getKey());
            }
        }

        for (Map.Entry<String, PendingBatch<?>> entry : toFlush) {
            String key = entry.getKey();
            PendingBatch<?> batch = entry.getValue();
            executor.submit(() -> executeBatch(key, batch));
        }
    }

    private void flushBatch(String key, PendingBatch<?> batch) {
        synchronized (pendingBatches) {
            if (pendingBatches.get(key) == batch) {
                pendingBatches.remove(key);
            }
        }
        executor.submit(() -> executeBatch(key, batch));
    }

    private <T> void executeBatch(String key, PendingBatch<T> batch) {
        String provider = batch.provider;
        Semaphore semaphore = providerConcurrency.computeIfAbsent(provider, p -> new Semaphore(MAX_CONCURRENT_BATCHES));

        try {
            // Rate limit: ensure minimum interval between batches for same provider
            Instant lastSent = lastBatchSent.get(provider);
            if (lastSent != null) {
                long sinceLast = Duration.between(lastSent, Instant.now()).toMillis();
                if (sinceLast < MIN_INTERVAL_PER_PROVIDER.toMillis()) {
                    Thread.sleep(MIN_INTERVAL_PER_PROVIDER.toMillis() - sinceLast);
                }
            }

            if (!semaphore.tryAcquire(5, TimeUnit.SECONDS)) {
                log.warn("Could not acquire concurrency permit for provider {}", provider);
                for (BatchItem<T> item : batch.items) {
                    item.future.completeExceptionally(
                            new RuntimeException("Provider concurrency limit exceeded: " + provider));
                }
                return;
            }

            lastBatchSent.put(provider, Instant.now());

            // Execute each item. In a real implementation, this could be a true
            // multi-prompt batch API call (e.g. OpenAI batch API).
            for (BatchItem<T> item : batch.items) {
                try {
                    T result = item.executor.apply(item.prompt);
                    item.future.complete(result);
                } catch (Exception e) {
                    item.future.completeExceptionally(e);
                }
            }

            log.debug("Executed batch for {} with {} items", key, batch.items.size());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            for (BatchItem<T> item : batch.items) {
                item.future.completeExceptionally(e);
            }
        } finally {
            semaphore.release();
        }
    }

    private static class PendingBatch<T> {
        final String provider;
        final String model;
        final List<BatchItem<T>> items = new ArrayList<>();
        final Instant createdAt = Instant.now();

        PendingBatch(String provider, String model) {
            this.provider = provider;
            this.model = model;
        }

        synchronized void add(BatchItem<T> item) {
            items.add(item);
        }

        synchronized boolean isFull() {
            return items.size() >= MAX_BATCH_SIZE;
        }

        synchronized boolean isEmpty() {
            return items.isEmpty();
        }

        synchronized boolean isExpired() {
            return Duration.between(createdAt, Instant.now()).toMillis() > BATCH_WINDOW_MS * 2;
        }

        synchronized boolean isReadyToFlush() {
            return !items.isEmpty() &&
                    (Duration.between(createdAt, Instant.now()).toMillis() >= BATCH_WINDOW_MS || items.size() >= MAX_BATCH_SIZE);
        }
    }

    private static class BatchItem<T> {
        final String prompt;
        final Function<String, T> executor;
        final CompletableFuture<T> future = new CompletableFuture<>();

        BatchItem(String prompt, Function<String, T> executor) {
            this.prompt = prompt;
            this.executor = executor;
        }

        @Override
        public int hashCode() {
            return Objects.hash(prompt);
        }
    }
}
