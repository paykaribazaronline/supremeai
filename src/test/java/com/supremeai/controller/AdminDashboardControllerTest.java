package com.supremeai.controller;

import com.supremeai.repository.UserApiRepository;
import com.supremeai.repository.UserRepository;
import com.supremeai.service.QuotaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
public class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserApiRepository userApiRepository;

    @MockBean
    private QuotaService quotaService;

    @Test
    public void testGetContract() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/contract"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contractVersion").value("2026-04-09-unified"))
                .andExpect(jsonPath("$.title").value("SupremeAI Admin Dashboard"))
                .andExpect(jsonPath("$.stats.activeAIAgents").value(12))
                .andExpect(jsonPath("$.navigation").isArray())
                .andExpect(jsonPath("$.navigation[0].key").value("overview"));
    }
}