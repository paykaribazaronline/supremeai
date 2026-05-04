package com.supremeai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Reactive streams service with backpressure support.
 * Handles high-volume scenarios with proper flow control.
 */
@Service
public class ReactiveStreamService {

    private static final Logger log = LoggerFactory.getLogger(ReactiveStreamService.class);

    /**
     * Process items with backpressure using Reactor.
     * Automatically handles flow control.
     */
    public <T, R> Flux<R> processWithBackpressure(
            List<T> items,
            Function<T, Mono<R>> processor,
            int concurrency) {
        
        return Flux.fromIterable(items)
            .flatMap(item -> processor.apply(item), concurrency)
            .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Process items with rate limiting.
     */
    public <T, R> Flux<R> processWithRateLimit(
            List<T> items,
            Function<T, Mono<R>> processor,
            int maxConcurrency,
            int rateLimitPerSecond) {
        
        return Flux.fromIterable(items)
            .window(rateLimitPerSecond)
            .concatMap(window -> 
                window.flatMap(item -> processor.apply(item), maxConcurrency)
            )
            .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Convert CompletableFuture to Mono.
     */
    public <T> Mono<T> fromCompletableFuture(CompletableFuture<T> future) {
        return Mono.fromFuture(future)
            .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Process with parallel execution and collect results.
     */
    public <T, R> Mono<List<R>> processParallel(
            List<T> items,
            Function<T, Mono<R>> processor,
            int parallelism) {
        
        return Flux.fromIterable(items)
            .flatMap(item -> processor.apply(item), parallelism)
            .collectList()
            .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Stream processing with error handling.
     */
    public <T, R> Flux<R> processWithErrorHandling(
            List<T> items,
            Function<T, Mono<R>> processor,
            Function<Throwable, ? extends R> errorHandler) {
        
        return Flux.fromIterable(items)
            .flatMap(item -> 
                processor.apply(item)
                    .onErrorResume(e -> Mono.just(errorHandler.apply(e)))
            )
            .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Batch processing with backpressure.
     */
    public <T, R> Flux<List<R>> processBatches(
            List<T> items,
            Function<List<T>, Mono<List<R>>> batchProcessor,
            int batchSize) {
        
        return Flux.fromIterable(items)
            .buffer(batchSize)
            .flatMap(batchProcessor::apply)
            .subscribeOn(Schedulers.boundedElastic());
    }
}