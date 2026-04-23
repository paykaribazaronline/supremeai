package com.supremeai.controller;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/test")
public class ApiTestingController {

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/endpoint")
    public ResponseEntity<ApiTestResult> testEndpoint(@RequestBody ApiTestRequest request) {
        long startTime = System.currentTimeMillis();

        try {
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
