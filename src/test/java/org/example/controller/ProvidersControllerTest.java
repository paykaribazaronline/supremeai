package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.service.ProviderManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProvidersControllerTest {

    @Mock
    private ProviderManagementService providerManagementService;

    @InjectMocks
    private ProvidersController providersController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(providersController).build();
    }

    @Test
    void rotatesProviderViaDashboardEndpoint() throws Exception {
        when(providerManagementService.rotateProvider(eq("google-gemini"), anyMap()))
            .thenReturn(Map.of("success", true, "provider", Map.of("id", "google-gemini")));

        mockMvc.perform(post("/api/providers/rotate/google-gemini")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of("apiKey", "new-key", "reason", "security"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.provider.id").value("google-gemini"));
    }

    @Test
    void returnsAuditEventsForLogsDashboard() throws Exception {
        when(providerManagementService.getAuditEvents(50))
            .thenReturn(List.of());

        mockMvc.perform(get("/api/providers/audit").param("limit", "50"))
            .andExpect(status().isOk());
    }

    @Test
    void removesProviderWithActorFromDashboard() throws Exception {
        when(providerManagementService.removeProvider("google-gemini", "admin@supremeai.com")).thenReturn(true);

        mockMvc.perform(post("/api/providers/remove")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of("id", "google-gemini", "actedBy", "admin@supremeai.com"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }
}