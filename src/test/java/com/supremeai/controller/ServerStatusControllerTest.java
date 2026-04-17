package com.supremeai.controller;

import com.supremeai.security.ApiKeyFilter;
import com.supremeai.service.QuotaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ServerStatusController.class)
public class ServerStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApiKeyFilter apiKeyFilter;

    @MockBean
    private QuotaService quotaService;

    @Test
    public void testGetStatus() throws Exception {
        mockMvc.perform(get("/api/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.message").value("SupremeAI Backend is running"))
                .andExpect(jsonPath("$.version").value("6.0.0"));
    }
}