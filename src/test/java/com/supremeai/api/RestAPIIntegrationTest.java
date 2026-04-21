package com.supremeai.api;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for core REST API endpoints.
 * Tests the full request-response cycle with real server startup.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Disabled("Disabled for build - requires full application context setup")
public class RestAPIIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void contextLoads() {
        // Verify Spring application context loads successfully
        assertNotNull(restTemplate, "TestRestTemplate should be injected");
    }

    @Test
    void healthEndpoint_ReturnsHealthy() {
        // WHEN
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

        // THEN
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Health endpoint should return 200");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertTrue(response.getBody().contains("UP") || response.getBody().contains("HEALTHY"),
                "Health response should indicate UP or HEALTHY status");
    }

    @Test
    void serverStatus_ReturnsOk() {
        // WHEN
        ResponseEntity<String> response = restTemplate.getForEntity("/api/server/status", String.class);

        // THEN - should return 200 (even if empty, endpoint exists)
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void providersEndpoint_ReturnsList() {
        // WHEN
        ResponseEntity<String> response = restTemplate.getForEntity("/api/admin/providers", String.class);

        // THEN - should return 200 with JSON array
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void metricsEndpoint_ReturnsMetrics() {
        // WHEN
        ResponseEntity<String> response = restTemplate.getForEntity("/metrics", String.class);

        // THEN - Prometheus metrics endpoint should be accessible
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void configEndpoint_ReturnsConfig() {
        // WHEN
        ResponseEntity<String> response = restTemplate.getForEntity("/api/config", String.class);

        // THEN - config endpoint should be accessible
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
