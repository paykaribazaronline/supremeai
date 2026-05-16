package com.supremeai.controller;

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
                @SuppressWarnings("unchecked")
                Map<String, String> headerMap = (Map<String, String>) request.getHeaders();
                headerMap.forEach(headers::add);
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
                    .status(response.getStatusCode().value())
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

    public static class ApiTestRequest {
        private String url;
        private String method;
        private String body;
        private Map<String, String> headers;

        public ApiTestRequest() {
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, String> headers) {
            this.headers = headers;
        }
    }

    public static class ApiTestResult {
        private int status;
        private String responseBody;
        private long responseTimeMs;
        private Map<String, String> headers;
        private boolean success;
        private String error;

        public ApiTestResult() {
        }

        public ApiTestResult(int status, String responseBody, long responseTimeMs, Map<String, String> headers, boolean success, String error) {
            this.status = status;
            this.responseBody = responseBody;
            this.responseTimeMs = responseTimeMs;
            this.headers = headers;
            this.success = success;
            this.error = error;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public String getResponseBody() {
            return responseBody;
        }

        public void setResponseBody(String responseBody) {
            this.responseBody = responseBody;
        }

        public long getResponseTimeMs() {
            return responseTimeMs;
        }

        public void setResponseTimeMs(long responseTimeMs) {
            this.responseTimeMs = responseTimeMs;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, String> headers) {
            this.headers = headers;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public static ApiTestResultBuilder builder() {
            return new ApiTestResultBuilder();
        }

        public static class ApiTestResultBuilder {
            private int status;
            private String responseBody;
            private long responseTimeMs;
            private Map<String, String> headers;
            private boolean success;
            private String error;

            ApiTestResultBuilder() {
            }

            public ApiTestResultBuilder status(int status) {
                this.status = status;
                return this;
            }

            public ApiTestResultBuilder responseBody(String responseBody) {
                this.responseBody = responseBody;
                return this;
            }

            public ApiTestResultBuilder responseTimeMs(long responseTimeMs) {
                this.responseTimeMs = responseTimeMs;
                return this;
            }

            public ApiTestResultBuilder headers(Map<String, String> headers) {
                this.headers = headers;
                return this;
            }

            public ApiTestResultBuilder success(boolean success) {
                this.success = success;
                return this;
            }

            public ApiTestResultBuilder error(String error) {
                this.error = error;
                return this;
            }

            public ApiTestResult build() {
                return new ApiTestResult(status, responseBody, responseTimeMs, headers, success, error);
            }
        }
    }
}
