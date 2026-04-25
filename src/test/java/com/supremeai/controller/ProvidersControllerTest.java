
package com.supremeai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supremeai.model.APIProvider;
import com.supremeai.service.AIProviderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProvidersController.class)
public class ProvidersControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AIProviderService providerService;

    @Autowired
    private ObjectMapper objectMapper;

    private APIProvider provider;

    @BeforeEach
    public void setUp() {
        provider = new APIProvider();
        provider.setId("provider1");
        provider.setName("OpenAI");
        provider.setType("openai");
        provider.setApiKey("sk-test-key");
        provider.setEnabled(true);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testGetAllProviders_Success() throws Exception {
        // Arrange
        List<APIProvider> providers = Arrays.asList(provider);
        when(providerService.getAllProviders()).thenReturn(providers);

        // Act & Assert
        mockMvc.perform(get("/api/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("provider1"))
                .andExpect(jsonPath("$[0].name").value("OpenAI"));
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testGetAllProviders_ForbiddenForRegularUser() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/providers"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testGetProviderById_Success() throws Exception {
        // Arrange
        when(providerService.getProviderById("provider1")).thenReturn(provider);

        // Act & Assert
        mockMvc.perform(get("/api/providers/provider1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("provider1"))
                .andExpect(jsonPath("$.name").value("OpenAI"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testCreateProvider_Success() throws Exception {
        // Arrange
        when(providerService.saveProvider(any(APIProvider.class))).thenReturn(provider);

        // Act & Assert
        mockMvc.perform(post("/api/providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(provider)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("provider1"))
                .andExpect(jsonPath("$.name").value("OpenAI"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testCreateProvider_InvalidData() throws Exception {
        // Arrange
        provider.setName(""); // Empty name

        // Act & Assert
        mockMvc.perform(post("/api/providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(provider)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testUpdateProvider_Success() throws Exception {
        // Arrange
        provider.setName("Updated Provider");
        when(providerService.saveProvider(any(APIProvider.class))).thenReturn(provider);

        // Act & Assert
        mockMvc.perform(put("/api/providers/provider1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(provider)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Provider"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testDeleteProvider_Success() throws Exception {
        // Arrange
        when(providerService.deleteProvider("provider1")).thenReturn(true);

        // Act & Assert
        mockMvc.perform(delete("/api/providers/provider1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testDeleteProvider_NotFound() throws Exception {
        // Arrange
        when(providerService.deleteProvider("nonexistent")).thenReturn(false);

        // Act & Assert
        mockMvc.perform(delete("/api/providers/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testGetEnabledProviders_Success() throws Exception {
        // Arrange
        List<APIProvider> providers = Arrays.asList(provider);
        when(providerService.getEnabledProviders()).thenReturn(providers);

        // Act & Assert
        mockMvc.perform(get("/api/providers/enabled"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].enabled").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testTestProviderConnection_Success() throws Exception {
        // Arrange
        when(providerService.testProviderConnection("openai")).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/api/providers/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"openai\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testTestProviderConnection_Failure() throws Exception {
        // Arrange
        when(providerService.testProviderConnection("openai")).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/api/providers/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"openai\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testGetProviderStats_Success() throws Exception {
        // Arrange
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRequests", 1000);
        stats.put("successfulRequests", 950);
        stats.put("failedRequests", 50);
        stats.put("averageResponseTime", 250);

        when(providerService.getProviderStats("provider1")).thenReturn(stats);

        // Act & Assert
        mockMvc.perform(get("/api/providers/provider1/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequests").value(1000))
                .andExpect(jsonPath("$.successfulRequests").value(950))
                .andExpect(jsonPath("$.failedRequests").value(50))
                .andExpect(jsonPath("$.averageResponseTime").value(250));
    }
}
