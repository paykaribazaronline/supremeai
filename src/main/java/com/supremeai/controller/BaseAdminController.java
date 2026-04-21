package com.supremeai.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Simple base controller to eliminate duplicated CRUD boilerplate.
 * Generic, type-safe, and keeps child controllers focused on business logic.
 */
public abstract class BaseAdminController<T, ID> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Convert Flux<T> to Mono<ResponseEntity<Object>> wrapping list in a named map.
     */
    protected Mono<ResponseEntity<Object>> wrapList(Flux<T> flux, String key) {
        return flux.collectList()
                .map(list -> ResponseEntity.ok((Object) Map.of(key, list)))
                .onErrorResume(e -> handleError("Failed to fetch " + key, e));
    }

    /**
     * Standard success wrapper for single entity save operations.
     */
    protected <R> Mono<ResponseEntity<Object>> wrapSave(Mono<R> mono, String messageKey, R entity) {
        return mono.map(saved -> ResponseEntity.ok((Object) Map.of(messageKey, saved)))
                .onErrorResume(e -> handleError("Failed during " + messageKey, e));
    }

    /**
     * Standard success wrapper for delete operations.
     */
    protected Mono<ResponseEntity<Object>> wrapDelete(Mono<Void> mono, String successMessage) {
        return mono.then(Mono.just(ResponseEntity.ok((Object) Map.of("message", successMessage))))
                .onErrorResume(e -> handleError("Failed during delete operation", e));
    }

    /**
     * Generic error handler - consistent 500 responses with logging.
     */
    private Mono<ResponseEntity<Object>> handleError(String context, Throwable e) {
        logger.error(context, e);
        Map<String, Object> errorBody = Map.of("error", context + ": " + e.getMessage());
        return Mono.just(ResponseEntity.status(500).body((Object) errorBody));
    }
}
