package com.supremeai.controller;

import com.supremeai.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Simple base controller to eliminate duplicated CRUD boilerplate.
 * Generic, type-safe, and keeps child controllers focused on business logic.
 */
public abstract class BaseAdminController<T, ID> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Convert Flux<T> to Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> wrapping list in a named map.
     */
    protected Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> wrapList(Flux<T> flux, String key) {
        return flux.collectList()
                .map(list -> ResponseEntity.ok(ApiResponse.ok(Map.of(key, (Object)list))))
                .onErrorResume(e -> handleError("Failed to fetch " + key, e));
    }

    /**
     * Standard success wrapper for single entity save operations.
     */
    protected <R> Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> wrapSave(Mono<R> mono, String messageKey) {
        return mono.map(saved -> ResponseEntity.ok(ApiResponse.ok(Map.of(messageKey, (Object)saved))))
                .onErrorResume(e -> handleError("Failed during " + messageKey, e));
    }

    /**
     * Standard success wrapper for delete operations.
     */
    protected Mono<ResponseEntity<ApiResponse<String>>> wrapDelete(Mono<Void> mono, String successMessage) {
        return mono.then(Mono.just(ResponseEntity.ok(ApiResponse.ok(successMessage))))
                .onErrorResume(e -> e instanceof RuntimeException 
                    ? handleError("Failed during delete operation", e).map(re -> ResponseEntity.status(re.getStatusCode()).body(ApiResponse.error(e.getMessage())))
                    : handleError("Failed during delete operation", e).map(re -> ResponseEntity.status(re.getStatusCode()).body(ApiResponse.error(e.getMessage()))));
    }

    /**
     * Generic error handler - consistent 500 responses with logging.
     */
    protected Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> handleError(String context, Throwable e) {
        logger.error(context, e);
        return Mono.just(ResponseEntity.status(500).body(ApiResponse.error(context + ": " + e.getMessage())));
    }
}
