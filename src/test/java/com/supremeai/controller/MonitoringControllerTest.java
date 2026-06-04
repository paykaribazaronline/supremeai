package com.supremeai.controller;

import com.supremeai.service.ProductionHealthMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import reactor.test.StepVerifier;
import org.mockito.InjectMocks;

@ExtendWith(MockitoExtension.class)
class MonitoringControllerTest {

    @Mock
    private ProductionHealthMonitor healthMonitor;

    @InjectMocks
    private MonitoringController monitoringController;

    @BeforeEach
    void setUp() {
        // MockitoExtension handles @InjectMocks
    }

    // ==================== getHealthStatus Tests ====================

    @Test
    void getHealthStatus_ReturnsHealthMap() {
        Map<String, Object> mockHealth = Map.of(
                "overallStatus", "HEALTHY",
                "uptimeSeconds", 3600L,
                "metrics", Map.of("totalRequests", 100L, "successfulRequests", 95L)
        );
        when(healthMonitor.getHealthStatus()).thenReturn(mockHealth);

        Mono<ResponseEntity<Map<String, Object>>> result = monitoringController.getHealthStatus();

        StepVerifierUtil.verifyOkResponse(result, response -> {
            assertEquals("HEALTHY", response.get("overallStatus"));
            assertEquals(3600L, response.get("uptimeSeconds"));
            assertNotNull(response.get("metrics"));
        });
    }

    // ==================== getDashboardSummary Tests ====================

    @Test
    void getDashboardSummary_ReturnsSummaryMap() {
        Map<String, Object> mockSummary = Map.of(
                "status", "HEALTHY",
                "uptime", "0d 1h 0m 0s",
                "requests", 100L,
                "successRate", "95.0%",
                "providerCount", 5,
                "activeUsers", 10
        );
        when(healthMonitor.getDashboardSummary()).thenReturn(mockSummary);

        Mono<ResponseEntity<Map<String, Object>>> result = monitoringController.getDashboardSummary();

        StepVerifierUtil.verifyOkResponse(result, response -> {
            assertEquals("HEALTHY", response.get("status"));
            assertEquals(100L, response.get("requests"));
            assertEquals("95.0%", response.get("successRate"));
        });
    }

    // ==================== resetMetrics Tests ====================

    @Test
    void resetMetrics_CallsMonitorAndReturnsSuccess() {
        doNothing().when(healthMonitor).resetMetrics();

        Mono<ResponseEntity<Map<String, Object>>> result = monitoringController.resetMetrics();

        StepVerifierUtil.verifyOkResponse(result, response -> {
            assertEquals("OK", response.get("status"));
            assertEquals("Metrics reset successfully", response.get("message"));
        });

        verify(healthMonitor).resetMetrics();
    }

    // ==================== testSentry Tests ====================

    @Test
    void testSentry_ReturnsErrorResponse() {
        Mono<ResponseEntity<Map<String, Object>>> result = monitoringController.testSentry();

        StepVerifier.create(result)
                .expectNextMatches(response ->
                        response.getStatusCode().is5xxServerError() &&
                        "ERROR".equals(response.getBody().get("status")) &&
                        response.getBody().containsKey("error"))
                .verifyComplete();
    }

    // ==================== Helper StepVerifierUtil ====================

    private static class StepVerifierUtil {
        static void verifyOkResponse(Mono<ResponseEntity<Map<String, Object>>> mono,
                                      java.util.function.Consumer<Map<String, Object>> assertions) {
            StepVerifier.create(mono)
                    .expectNextMatches(response -> {
                        assertEquals(200, response.getStatusCode().value());
                        assertions.accept(response.getBody());
                        return true;
                    })
                    .verifyComplete();
        }
    }
}