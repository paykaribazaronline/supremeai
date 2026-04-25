
package com.supremeai.security;

import com.supremeai.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testPublicEndpointsAccessibleWithoutAuth() throws Exception {
        // Test public endpoints
        mockMvc.perform(get("/api/auth/firebase-login"))
                .andExpect(status().isMethodNotAllowed()); // POST endpoint

        mockMvc.perform(get("/api/auth/register"))
                .andExpect(status().isMethodNotAllowed()); // POST endpoint

        mockMvc.perform(get("/api/chat/test"))
                .andExpect(status().isNotFound()); // Endpoint might not exist, but should not be 401/403
    }

    @Test
    public void testAdminEndpointsRequireAdminRole() throws Exception {
        // Test admin endpoints without authentication
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testAdminEndpointsForbiddenForRegularUser() throws Exception {
        // Test admin endpoints with regular user role
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testAdminEndpointsAccessibleForAdmin() throws Exception {
        // Test admin endpoints with admin role
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isNotFound()); // Endpoint might not exist, but should not be 401/403

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isNotFound()); // Endpoint might not exist, but should not be 401/403
    }

    @Test
    public void testAuthenticatedEndpointsRequireAuthentication() throws Exception {
        // Test endpoints that require authentication
        mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testAuthenticatedEndpointsAccessibleWithAuth() throws Exception {
        // Test endpoints with authentication
        mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().isNotFound()); // Endpoint might not exist, but should not be 401/403

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isNotFound()); // Endpoint might not exist, but should not be 401/403
    }
}
