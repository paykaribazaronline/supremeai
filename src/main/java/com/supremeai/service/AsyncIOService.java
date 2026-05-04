package com.supremeai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Async non-blocking I/O service for external calls.
 * Uses Java 11+ HttpClient for efficient async operations.
 */
@Service
public class AsyncIOService {

    private static final Logger log = LoggerFactory.getLogger(AsyncIOService.class);

    private final HttpClient httpClient;

    public AsyncIOService() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .version(HttpClient.Version.HTTP_2)
            .build();
    }

    /**
     * Execute async HTTP GET request.
     */
    public CompletableFuture<String> getAsync(String url, Duration timeout) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(timeout)
            .GET()
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(HttpResponse::body)
            .exceptionally(throwable -> {
                log.error("Async GET request failed for {}", url, throwable);
                throw new RuntimeException(throwable);
            });
    }

    /**
     * Execute async HTTP POST request.
     */
    public CompletableFuture<String> postAsync(String url, String body, Duration timeout) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(timeout)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .header("Content-Type", "application/json")
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(HttpResponse::body)
            .exceptionally(throwable -> {
                log.error("Async POST request failed for {}", url, throwable);
                throw new RuntimeException(throwable);
            });
    }

    /**
     * Execute async HTTP request with custom headers.
     */
    public CompletableFuture<String> requestAsync(
            String method, 
            String url, 
            String body, 
            Duration timeout,
            java.util.Map<String, String> headers) {
        
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(timeout);

        headers.forEach(builder::header);

        if (body != null && !body.isEmpty()) {
            builder.POST(HttpRequest.BodyPublishers.ofString(body));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }

        return httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
            .thenApply(HttpResponse::body)
            .exceptionally(throwable -> {
                log.error("Async request failed for {} {}", method, url, throwable);
                throw new RuntimeException(throwable);
            });
    }

    /**
     * Execute multiple async requests in parallel.
     */
    public CompletableFuture<java.util.List<String>> getAllAsync(
            java.util.List<String> urls, 
            Duration timeout) {
        
        return CompletableFuture.allOf(
            urls.stream()
                .map(url -> getAsync(url, timeout))
                .toArray(CompletableFuture[]::new)
        ).thenApply(v -> 
            urls.stream()
                .map(url -> getAsync(url, timeout).join())
                .toList()
        );
    }
}