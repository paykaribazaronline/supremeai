package com.supremeai.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.supremeai.repository.MonitoringLogRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class ProductionHealthMonitorTest {

  private MeterRegistry meterRegistry;
  private ProductionHealthMonitor healthMonitor;

  @Mock private com.supremeai.mcp.MCPClientManager mcpClientManager;

  @Mock private SimpMessagingTemplate messagingTemplate;

  @Mock private MonitoringLogRepository monitoringLogRepository;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    healthMonitor = new ProductionHealthMonitor(meterRegistry);

    ReflectionTestUtils.setField(healthMonitor, "mcpClientManager", mcpClientManager);
    ReflectionTestUtils.setField(healthMonitor, "messagingTemplate", messagingTemplate);
    ReflectionTestUtils.setField(healthMonitor, "monitoringLogRepository", monitoringLogRepository);

    lenient()
        .when(monitoringLogRepository.save(any()))
        .thenReturn(Mono.just(new com.supremeai.model.MonitoringLog()));
  }

  // ==================== Record Success Tests ====================

  @Test
  void recordSuccess_IncrementsCounters() {
    healthMonitor.recordSuccess("openai", 150L);

    Map<String, Object> status = healthMonitor.getHealthStatus();
    Map<String, Object> metrics = (Map<String, Object>) status.get("metrics");

    assertEquals(1L, metrics.get("totalRequests"));
    assertEquals(1L, metrics.get("successfulRequests"));
    assertEquals(0L, metrics.get("failedRequests"));
    assertEquals(150L, metrics.get("avgResponseTimeMs"));
  }

  @Test
  void recordSuccess_MultipleCalls_CalculatesAverageResponseTime() {
    healthMonitor.recordSuccess("openai", 100L);
    healthMonitor.recordSuccess("openai", 200L);
    healthMonitor.recordSuccess("gemini", 50L);

    Map<String, Object> status = healthMonitor.getHealthStatus();
    Map<String, Object> metrics = (Map<String, Object>) status.get("metrics");

    assertEquals(3L, metrics.get("totalRequests"));
    assertEquals(3L, metrics.get("successfulRequests"));
    assertEquals(116L, metrics.get("avgResponseTimeMs")); // (100+200+50)/3 = 116
  }

  // ==================== Record Failure Tests ====================

  @Test
  void recordFailure_IncrementsFailureCounters() {
    healthMonitor.recordFailure("openai", "Connection timeout");

    Map<String, Object> status = healthMonitor.getHealthStatus();
    Map<String, Object> metrics = (Map<String, Object>) status.get("metrics");

    assertEquals(1L, metrics.get("totalRequests"));
    assertEquals(0L, metrics.get("successfulRequests"));
    assertEquals(1L, metrics.get("failedRequests"));
  }

  @Test
  void recordFailure_MultipleFailures_TracksPerProvider() {
    healthMonitor.recordFailure("openai", "Error 1");
    healthMonitor.recordFailure("openai", "Error 2");
    healthMonitor.recordFailure("gemini", "Error 3");

    Map<String, Object> status = healthMonitor.getHealthStatus();
    Map<String, Object> providers = (Map<String, Object>) status.get("providerHealth");

    Map<String, Object> openaiHealth = (Map<String, Object>) providers.get("openai");
    Map<String, Object> geminiHealth = (Map<String, Object>) providers.get("gemini");

    assertEquals(2L, openaiHealth.get("errorCount"));
    assertEquals(1L, geminiHealth.get("errorCount"));
  }

  // ==================== Get Health Status Tests ====================

  @Test
  void getHealthStatus_ReturnsValidStructure() {
    Map<String, Object> status = healthMonitor.getHealthStatus();

    assertNotNull(status.get("timestamp"));
    assertNotNull(status.get("overallStatus"));
    assertNotNull(status.get("uptimeSeconds"));
    assertNotNull(status.get("metrics"));
    assertNotNull(status.get("providerHealth"));
    assertNotNull(status.get("alerts"));
  }

  @Test
  void getHealthStatus_InitialState_IsHealthy() {
    Map<String, Object> status = healthMonitor.getHealthStatus();
    assertEquals("HEALTHY", status.get("overallStatus"));
  }

  // ==================== Alerts Tests ====================

  @Test
  void getHealthStatus_LowSuccessRate_GeneratesAlert() {
    // Simulate many failures to trigger alert
    for (int i = 0; i < 96; i++) {
      healthMonitor.recordFailure("openai", "Error");
    }
    for (int i = 0; i < 5; i++) {
      healthMonitor.recordSuccess("openai", 100L);
    }

    Map<String, Object> status = healthMonitor.getHealthStatus();
    @SuppressWarnings("unchecked")
    java.util.List<Map<String, Object>> alerts =
        (java.util.List<Map<String, Object>>) status.get("alerts");

    assertTrue(alerts.size() > 0);
    Map<String, Object> firstAlert = alerts.get(0);
    assertEquals("HIGH", firstAlert.get("severity"));
    assertEquals("success_rate", firstAlert.get("type"));
  }

  @Test
  void getHealthStatus_HighResponseTime_GeneratesAlert() {
    // Simulate slow responses
    for (int i = 0; i < 10; i++) {
      healthMonitor.recordSuccess("openai", 600L);
    }

    Map<String, Object> status = healthMonitor.getHealthStatus();
    @SuppressWarnings("unchecked")
    java.util.List<Map<String, Object>> alerts =
        (java.util.List<Map<String, Object>>) status.get("alerts");

    boolean hasResponseTimeAlert =
        alerts.stream().anyMatch(a -> "response_time".equals(a.get("type")));
    assertTrue(hasResponseTimeAlert);
  }

  @Test
  void getHealthStatus_ManyProviderErrors_GeneratesAlert() {
    for (int i = 0; i < 15; i++) {
      healthMonitor.recordFailure("openai", "Error " + i);
    }

    Map<String, Object> status = healthMonitor.getHealthStatus();
    @SuppressWarnings("unchecked")
    java.util.List<Map<String, Object>> alerts =
        (java.util.List<Map<String, Object>>) status.get("alerts");

    boolean hasProviderErrorAlert =
        alerts.stream().anyMatch(a -> "provider_errors".equals(a.get("type")));
    assertTrue(hasProviderErrorAlert);
  }

  @Test
  void getHealthStatus_NoAlerts_WhenHealthy() {
    for (int i = 0; i < 10; i++) {
      healthMonitor.recordSuccess("openai", 100L);
    }

    Map<String, Object> status = healthMonitor.getHealthStatus();
    @SuppressWarnings("unchecked")
    java.util.List<Map<String, Object>> alerts =
        (java.util.List<Map<String, Object>>) status.get("alerts");

    assertTrue(alerts.isEmpty());
  }

  // ==================== Update System Metrics Tests ====================

  @Test
  void updateSystemMetrics_UpdatesValues() {
    healthMonitor.updateSystemMetrics(45.5, 62.3, 100);

    Map<String, Object> status = healthMonitor.getHealthStatus();
    Map<String, Object> metrics = (Map<String, Object>) status.get("metrics");

    assertEquals(45.5, metrics.get("cpuUsage"));
    assertEquals(62.3, metrics.get("memoryUsage"));
    assertEquals(100, metrics.get("activeSessions"));
  }

  // ==================== Get Dashboard Summary Tests ====================

  @Test
  void getDashboardSummary_ReturnsCorrectFormat() {
    healthMonitor.recordSuccess("openai", 100L);
    healthMonitor.recordSuccess("gemini", 200L);

    Map<String, Object> summary = healthMonitor.getDashboardSummary();

    assertNotNull(summary.get("status"));
    assertNotNull(summary.get("uptime"));
    assertEquals(2L, summary.get("requests"));
    assertEquals(2, summary.get("providerCount"));
    assertEquals(0, summary.get("activeUsers"));
  }

  @Test
  void getDashboardSummary_OverallStatusReflectsHealth() {
    // Initially healthy
    assertEquals("HEALTHY", healthMonitor.getDashboardSummary().get("status"));

    // Add failures to make it degraded
    for (int i = 0; i < 200; i++) {
      healthMonitor.recordFailure("openai", "Error");
    }
    healthMonitor.updateUptime();

    String status = (String) healthMonitor.getDashboardSummary().get("status");
    assertTrue(List.of("DEGRADED", "WARNING").contains(status));
  }

  // ==================== Format Uptime Tests ====================

  @Test
  void formatUptime_ReturnsCorrectFormat() {
    // Use reflection to set uptimeSeconds
    java.lang.reflect.Field field;
    try {
      field = ProductionHealthMonitor.class.getDeclaredField("uptimeSeconds");
      field.setAccessible(true);
      field.set(healthMonitor, 94664L); // 1 day + 2 hours + 17 min + 44 sec
    } catch (Exception e) {
      fail("Failed to set uptimeSeconds: " + e.getMessage());
    }

    String uptime = (String) healthMonitor.getDashboardSummary().get("uptime");
    assertTrue(uptime.contains("1d"));
    assertTrue(uptime.contains("2h"));
    assertTrue(uptime.contains("17m"));
    assertTrue(uptime.contains("44s"));
  }

  // ==================== IsProviderHealthy Tests ====================

  @Test
  void isProviderHealthy_ReturnsTrueForHealthyProvider() {
    healthMonitor.recordSuccess("openai", 100L);
    assertTrue(healthMonitor.isProviderHealthy("openai"));
  }

  @Test
  void isProviderHealthy_ReturnsFalseForUnknownProvider() {
    assertFalse(healthMonitor.isProviderHealthy("unknown"));
  }

  @Test
  void isProviderHealthy_ReturnsFalseForFailedProvider() {
    healthMonitor.recordFailure("openai", "Connection error");
    assertFalse(healthMonitor.isProviderHealthy("openai"));
  }

  // ==================== Reset Metrics Tests ====================

  @Test
  void resetMetrics_ClearsAllCounters() {
    healthMonitor.recordSuccess("openai", 100L);
    healthMonitor.recordFailure("gemini", "Error");
    healthMonitor.resetMetrics();

    Map<String, Object> status = healthMonitor.getHealthStatus();
    Map<String, Object> metrics = (Map<String, Object>) status.get("metrics");

    assertEquals(0L, metrics.get("totalRequests"));
    assertEquals(0L, metrics.get("successfulRequests"));
    assertEquals(0L, metrics.get("failedRequests"));
    assertEquals(0, ((java.util.List<?>) status.get("alerts")).size());
  }

  // ==================== Update Uptime Tests ====================

  @Test
  void updateUptime_ChangesOverallStatus() {
    // Call updateUptime to trigger status recalculation
    healthMonitor.updateUptime();

    Map<String, Object> status = healthMonitor.getHealthStatus();
    assertNotNull(status.get("overallStatus"));
  }
}
