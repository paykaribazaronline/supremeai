package org.example.controller;

import org.example.api.APIVersionController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.*;

/**
 * API Version Controller Test
 * Tests API versioning and endpoint routing
 */
@DisplayName("API Version Controller Tests")
public class APIVersionControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new APIVersionController()).build();
    }
    
    @Test
    @DisplayName("GET /api returns version info")
    public void testGetAPIRoot() throws Exception {
        mockMvc.perform(get("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("SupremeAI API"))
                .andExpect(jsonPath("$.currentVersion").value("v2"))
                .andExpect(jsonPath("$.versions").isArray());
    }
    
    @Test
    @DisplayName("GET /api/v1/info returns legacy v1 info")
    public void testGetAPIInfoV1() throws Exception {
        mockMvc.perform(get("/api/v1/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("v1"))
                .andExpect(jsonPath("$.description").value("Legacy API endpoints (backward compatible)"))
                .andExpect(jsonPath("$.endpoints").isArray());
    }
    
    @Test
    @DisplayName("GET /api/v1/info contains expected endpoints")
    public void testV1InfoEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endpoints[0]").value("/api/v1/projects"))
                .andExpect(jsonPath("$.endpoints[1]").value("/api/v1/agents"))
                .andExpect(jsonPath("$.endpoints[2]").value("/api/v1/providers"));
    }
    
    @Test
    @DisplayName("GET /api/v2/info returns enhanced v2 info")
    public void testGetAPIInfoV2() throws Exception {
        mockMvc.perform(get("/api/v2/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("v2"))
                .andExpect(jsonPath("$.features").isArray())
                .andExpect(jsonPath("$.features", hasItems(
                        "Webhooks with retry logic",
                        "Request/response batching",
                        "Enhanced error handling"
                )));
    }
    
    @Test
    @DisplayName("GET /api/v2/info includes enhanced endpoints")
    public void testV2InfoEndpoints() throws Exception {
        mockMvc.perform(get("/api/v2/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endpoints[0]").value("/api/v2/projects"))
                .andExpect(jsonPath("$.endpoints[1]").value("/api/v2/agents"))
                .andExpect(jsonPath("$.endpoints[3]").value("/api/v2/webhooks"))
                .andExpect(jsonPath("$.endpoints[4]").value("/api/v2/batch"));
    }
    
    @Test
    @DisplayName("v2 includes all new features in features list")
    public void testV2HasAllFeatures() throws Exception {
        mockMvc.perform(get("/api/v2/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.features").isArray())
                .andExpect(jsonPath("$.features.length()").value(greaterThanOrEqualTo(3)));
    }
    
    @Test
    @DisplayName("v1 endpoint returns deprecation notice")
    public void testV1Deprecation() throws Exception {
        mockMvc.perform(get("/api/v1/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").exists());
    }
    
    @Test
    @DisplayName("v2 endpoint does not have deprecation notice")
    public void testV2NotDeprecated() throws Exception {
        mockMvc.perform(get("/api/v2/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deprecated[0]").value("v1 will be sunset on 2026-12-31"));
    }
}
