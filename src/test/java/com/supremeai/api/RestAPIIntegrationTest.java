package com.supremeai.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for core REST API endpoints.
 * Uses MockMvc to test the full request-response cycle against the real server context.
 * addFilters=false is used to bypass web security in isolated integration tests.
 */
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class RestAPIIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
        assertNotNull(mockMvc, "MockMvc should be injected");
    }

    @Test
    void healthEndpoint_ReturnsHealthy() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void serverStatus_ReturnsOk() throws Exception {
        // ServerStatusController: @RequestMapping("/api/status"), GET -> JSON status
        mockMvc.perform(get("/api/status"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void providersEndpoint_ReturnsList() throws Exception {
        mockMvc.perform(get("/api/admin/providers/configured"))
                .andExpect(status().isOk());
    }

    @Test
    void metricsEndpoint_ReturnsMetrics() throws Exception {
        // Actuator metrics – base path /actuator (exposed via management properties)
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void configEndpoint_ReturnsConfig() throws Exception {
        // ConfigController exposes /api/config/firebase (root /api/config has no handler)
        mockMvc.perform(get("/api/config/firebase"))
                .andExpect(status().isOk());
    }
}
