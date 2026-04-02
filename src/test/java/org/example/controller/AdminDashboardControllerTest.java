package org.example.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AdminDashboardControllerTest {

    private final AdminDashboardController controller = new AdminDashboardController();

    @Test
    void contractContainsCanonicalSectionsAndStats() {
        ResponseEntity<?> response = controller.getDashboardContract();

        assertEquals(200, response.getStatusCode().value());
        assertInstanceOf(Map.class, response.getBody());

        @SuppressWarnings("unchecked")
        Map<String, Object> contract = (Map<String, Object>) response.getBody();
        assertEquals("2026-04-03", contract.get("contractVersion"));
        assertEquals("/admin.html", contract.get("entryPath"));
        assertTrue(contract.containsKey("stats"));
        assertTrue(contract.containsKey("navigation"));
        assertTrue(contract.containsKey("orchestration"));

        @SuppressWarnings("unchecked")
        Map<String, Object> stats = (Map<String, Object>) contract.get("stats");
        assertTrue(stats.containsKey("activeAIAgents"));
        assertTrue(stats.containsKey("systemHealthScore"));
        assertTrue(stats.containsKey("systemHealthStatus"));
        assertTrue(stats.containsKey("lastSyncTime"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> navigation = (List<Map<String, Object>>) contract.get("navigation");
        assertTrue(navigation.stream().anyMatch(item -> "overview".equals(item.get("key"))));
        assertTrue(navigation.stream().anyMatch(item -> "provider-coverage".equals(item.get("key"))));
        assertTrue(navigation.stream().anyMatch(item -> "api-keys".equals(item.get("key"))));
    }
}