package com.supremeai.controller;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/test")
public class ApiTestingController {

    private final RestTemplate restTemplate = new RestTemplate();

    private static final Set<String> ALLOWED_HOSTS = Set.of(
            "api.openai.com",
            "api.anthropic.com",
            "api.gemini.google.com",
            "localhost",
            "127.0.0.1"
    );

    @PostMapping("/endpoint")
    public ResponseEntity<ApiTestResult> testEndpoint(@RequestBody ApiTestRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            if (!isUrlAllowed(request.getUrl())) {
                return ResponseEntity.ok(ApiTestResult.builder()
                        .success(false)
                        .error("URL not allowed. Only whitelisted hosts are permitted.")
                        .responseTimeMs(System.currentTimeMillis() - startTime)
                        .build());
            }

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            if (request.getHeaders() != null) {
                request.getHeaders().forEach(headers::add);
            }

            org.springframework.http.HttpEntity<String> entity =
                    new org.springframework.http.HttpEntity<>(request.getBody(), headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    request.getUrl(),
                    org.springframework.http.HttpMethod.valueOf(request.getMethod()),
                    entity,
                    String.class
            );

            long duration = System.currentTimeMillis() - startTime;

            return ResponseEntity.ok(ApiTestResult.builder()
                    .status(response.getStatusCodeValue())
                    .responseBody(response.getBody())
                    .responseTimeMs(duration)
                    .headers(response.getHeaders().toSingleValueMap())
                    .success(true)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.ok(ApiTestResult.builder()
                    .success(false)
                    .error(e.getMessage())
                    .responseTimeMs(System.currentTimeMillis() - startTime)
                    .build());
        }
    }

    private boolean isUrlAllowed(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            return host != null && ALLOWED_HOSTS.contains(host);
        } catch (Exception e) {
            return false;
        }
    }

    @Data
    public static class ApiTestRequest {
        private String url;
        private String method;
        private String body;
        private Map<String, String> headers;
    }

    @Data
    @Builder
    public static class ApiTestResult {
        private int status;
        private String responseBody;
        private long responseTimeMs;
        private Map<String, String> headers;
        private boolean success;
        private String error;
    }
}
