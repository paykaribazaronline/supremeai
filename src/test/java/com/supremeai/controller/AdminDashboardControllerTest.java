package com.supremeai.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminDashboardController.class)
public class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

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