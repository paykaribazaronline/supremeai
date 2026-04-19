package com.supremeai.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitingFilter implements WebFilter {

    @Value("${rate.limit.per.minute:100}")
    private int rateLimitPerMinute;

    // Simple in-memory rate limiter: ip -> (count, timestamp)
    private static class RateLimitInfo {
        AtomicInteger count = new AtomicInteger(0);
        Instant windowStart = Instant.now();
    }

    private final Map<String, RateLimitInfo> rateLimitCache = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        
        // Skip rate limiting for health checks and static assets
        if (path.startsWith("/api/health") ||
            path.startsWith("/api/status") ||
            path.startsWith("/static/") ||
            path.startsWith("/css/") ||
            path.startsWith("/js/")) {
            return chain.filter(exchange);
        }

        String key = getKey(exchange);
        RateLimitInfo info = rateLimitCache.computeIfAbsent(key, k -> new RateLimitInfo());
        
        // Reset window if older than 1 minute
        if (Duration.between(info.windowStart, Instant.now()).toMinutes() >= 1) {
            info.count.set(0);
            info.windowStart = Instant.now();
        }
        
        if (info.count.incrementAndGet() <= rateLimitPerMinute) {
            return chain.filter(exchange);
        } else {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            
            String body = "{\"error\":\"Rate limit exceeded\",\"limit\":" + rateLimitPerMinute + "}";
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes());
            return exchange.getResponse().writeWith(Mono.just(buffer));
        }
    }

    private String getKey(ServerWebExchange exchange) {
        String ip = exchange.getRequest().getRemoteAddress() != null ? 
                   exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : "unknown";
        String uri = exchange.getRequest().getURI().getPath();
        return ip + ":" + uri;
    }
}