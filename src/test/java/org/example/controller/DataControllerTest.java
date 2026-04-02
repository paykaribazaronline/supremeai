package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.service.DataCollectorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
public class DataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DataCollectorService dataCollectorService;

    private Map<String, Object> mockGitHubData;
    private Map<String, Object> mockVercelData;
    private Map<String, Object> mockHealthData;

    @BeforeEach
    void setUp() {
        // Setup mock data
        mockGitHubData = new HashMap<>();
        mockGitHubData.put("owner", "supremeai");
        mockGitHubData.put("repo", "core");
        mockGitHubData.put("stars", 150);
        mockGitHubData.put("forks", 25);
        mockGitHubData.put("issues", 3);
        mockGitHubData.put("pulls", 2);

        mockVercelData = new HashMap<>();
        mockVercelData.put("projectId", "proj_123");
        mockVercelData.put("status", "ready");
        mockVercelData.put("domains", 2);
        mockVercelData.put("deployments", 45);

        mockHealthData = new HashMap<>();
        mockHealthData.put("status", "UP");
        mockHealthData.put("timestamp", System.currentTimeMillis());
        mockHealthData.put("uptime", 3600);
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testGetGitHubDataSuccess() throws Exception {
        // Given
        when(dataCollectorService.getGitHubData("supremeai", "core"))
                .thenReturn(mockGitHubData);

        // When & Then
        mockMvc.perform(get("/api/v1/data/github/supremeai/core")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.owner", is("supremeai")))
                .andExpect(jsonPath("$.data.repo", is("core")))
                .andExpect(jsonPath("$.data.stars", is(150)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    void testGetGitHubDataNotFound() throws Exception {
        // Given
        when(dataCollectorService.getGitHubData("nonexistent", "repo"))
                .thenThrow(new IllegalArgumentException("Repository not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/data/github/nonexistent/repo")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Repository not found")));
    }

    @Test
    void testGetVercelStatusSuccess() throws Exception {
        // Given
        when(dataCollectorService.getVercelStatus("proj_123"))
                .thenReturn(mockVercelData);

        // When & Then
        mockMvc.perform(get("/api/v1/data/vercel/proj_123")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("ready")))
                .andExpect(jsonPath("$.data.deployments", is(45)));
    }

    @Test
    void testGetFirebaseStatusSuccess() throws Exception {
        // Given
        Map<String, Object> firebaseData = new HashMap<>();
        firebaseData.put("collections", 12);
        firebaseData.put("totalDocuments", 5420);
        firebaseData.put("storageGB", 2.5);

        when(dataCollectorService.getFirebaseStatus())
                .thenReturn(firebaseData);

        // When & Then
        mockMvc.perform(get("/api/v1/data/firebase")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.collections", is(12)))
                .andExpect(jsonPath("$.data.totalDocuments", is(5420)));
    }

    @Test
    void testGetHealthCheckSuccess() throws Exception {
        // Given
        when(dataCollectorService.getSystemHealth())
                .thenReturn(mockHealthData);

        // When & Then
        mockMvc.perform(get("/api/v1/data/health")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("UP")))
                .andExpect(jsonPath("$.data.uptime", is(3600)));
    }

    @Test
    void testGetStatsSuccess() throws Exception {
        // Given
        Map<String, Object> statsData = new HashMap<>();
        statsData.put("totalRequests", 10250);
        statsData.put("averageResponseTime", 245);
        statsData.put("errorRate", 0.02);

        when(dataCollectorService.getRequestStats())
                .thenReturn(statsData);

        // When & Then
        mockMvc.perform(get("/api/v1/data/stats")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRequests", is(10250)))
                .andExpect(jsonPath("$.data.errorRate", is(0.02)));
    }

    @Test
    void testCacheClearSuccess() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/data/cache/clear")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("cleared")));
    }

    @Test
    void testUnauthenticatedRequestRejected() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/data/health")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()); // Health check is public

        mockMvc.perform(get("/api/v1/data/stats")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testInvalidTokenRejected() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/data/stats")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testResponseFormatConsistency() throws Exception {
        // Given
        when(dataCollectorService.getSystemHealth())
                .thenReturn(mockHealthData);

        // When
        MvcResult result = mockMvc.perform(get("/api/v1/data/health")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        
        // Then - verify response structure
        mockMvc.perform(get("/api/v1/data/health")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.message").exists());
    }
}
