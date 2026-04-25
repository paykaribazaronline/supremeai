
package com.supremeai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supremeai.model.ActivityLog;
import com.supremeai.model.User;
import com.supremeai.service.AdminDashboardService;
import com.supremeai.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminDashboardController.class)
public class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminDashboardService adminDashboardService;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private User adminUser;
    private List<ActivityLog> activityLogs;

    @BeforeEach
    public void setUp() {
        adminUser = new User("admin-uid", "admin@example.com", "Admin User");
        adminUser.setTier(com.supremeai.model.UserTier.ADMIN);

        ActivityLog log1 = new ActivityLog();
        log1.setId("log1");
        log1.setUserId("user1");
        log1.setAction("LOGIN");
        log1.setTimestamp(LocalDateTime.now());
        log1.setDetails("User logged in");

        ActivityLog log2 = new ActivityLog();
        log2.setId("log2");
        log2.setUserId("user1");
        log2.setAction("CREATE_PROJECT");
        log2.setTimestamp(LocalDateTime.now());
        log2.setDetails("User created a new project");

        activityLogs = Arrays.asList(log1, log2);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testGetDashboardStats_Success() throws Exception {
        // Arrange
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", 100);
        stats.put("activeUsers", 75);
        stats.put("totalProjects", 50);
        stats.put("activeProjects", 40);

        when(adminDashboardService.getDashboardStats()).thenReturn(stats);

        // Act & Assert
        mockMvc.perform(get("/api/admin/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(100))
                .andExpect(jsonPath("$.activeUsers").value(75))
                .andExpect(jsonPath("$.totalProjects").value(50))
                .andExpect(jsonPath("$.activeProjects").value(40));
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testGetDashboardStats_ForbiddenForRegularUser() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/dashboard/stats"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testGetDashboardStats_UnauthorizedForUnauthenticatedUser() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/dashboard/stats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testGetRecentActivity_Success() throws Exception {
        // Arrange
        when(adminDashboardService.getRecentActivity(anyInt())).thenReturn(activityLogs);

        // Act & Assert
        mockMvc.perform(get("/api/admin/dashboard/activity?limit=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("LOGIN"))
                .andExpect(jsonPath("$[1].action").value("CREATE_PROJECT"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testGetUserActivity_Success() throws Exception {
        // Arrange
        when(adminDashboardService.getUserActivity(eq("user1"))).thenReturn(activityLogs);

        // Act & Assert
        mockMvc.perform(get("/api/admin/dashboard/users/user1/activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value("user1"))
                .andExpect(jsonPath("$[1].userId").value("user1"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testGetUserActivity_NotFound() throws Exception {
        // Arrange
        when(adminDashboardService.getUserActivity(eq("nonexistent"))).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/admin/dashboard/users/nonexistent/activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testGetSystemHealth_Success() throws Exception {
        // Arrange
        Map<String, Object> health = new HashMap<>();
        health.put("status", "HEALTHY");
        health.put("database", "CONNECTED");
        health.put("redis", "CONNECTED");
        health.put("cpu", "45%");
        health.put("memory", "60%");

        when(adminDashboardService.getSystemHealth()).thenReturn(health);

        // Act & Assert
        mockMvc.perform(get("/api/admin/dashboard/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HEALTHY"))
                .andExpect(jsonPath("$.database").value("CONNECTED"))
                .andExpect(jsonPath("$.redis").value("CONNECTED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testGetUserStats_Success() throws Exception {
        // Arrange
        Map<String, Object> userStats = new HashMap<>();
        userStats.put("totalProjects", 5);
        userStats.put("activeProjects", 3);
        userStats.put("apiCalls", 150);
        userStats.put("lastLogin", LocalDateTime.now().toString());

        when(adminDashboardService.getUserStats(eq("user1"))).thenReturn(userStats);

        // Act & Assert
        mockMvc.perform(get("/api/admin/dashboard/users/user1/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProjects").value(5))
                .andExpect(jsonPath("$.activeProjects").value(3))
                .andExpect(jsonPath("$.apiCalls").value(150));
    }
}
