package com.supremeai.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(com.supremeai.config.test.TestFirebaseConfig.class)
public class SecurityConfigTest {

  @Autowired private MockMvc mockMvc;

  @Test
  public void testPublicEndpointsAccessibleWithoutAuth() throws Exception {
    // Test public endpoints
    mockMvc
        .perform(get("/api/auth/firebase-login"))
        .andExpect(status().isMethodNotAllowed()); // POST endpoint

    mockMvc
        .perform(get("/api/auth/register"))
        .andExpect(status().isMethodNotAllowed()); // POST endpoint

    mockMvc
        .perform(get("/public/test-nonexistent"))
        .andExpect(status().isNotFound()); // Endpoint might not exist, but should not be 401/403
  }

  @Test
  public void testAdminEndpointsRequireAdminRole() throws Exception {
    mockMvc.perform(get("/api/admin/dashboard/contract")).andExpect(status().isUnauthorized());

    mockMvc.perform(get("/api/v1/admin/users")).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "USER")
  public void testAdminEndpointsForbiddenForRegularUser() throws Exception {
    mockMvc.perform(get("/api/admin/dashboard/contract")).andExpect(status().isForbidden());

    mockMvc.perform(get("/api/v1/admin/users")).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  public void testAdminEndpointsAccessibleForAdmin() throws Exception {
    mockMvc.perform(get("/api/admin/dashboard/contract")).andExpect(status().isOk());

    mockMvc.perform(get("/api/v1/admin/users")).andExpect(status().isNotFound());
  }

  @Test
  public void testAuthenticatedEndpointsRequireAuthentication() throws Exception {
    // Test endpoints that require authentication
    mockMvc.perform(get("/api/user/profile")).andExpect(status().isUnauthorized());

    mockMvc.perform(get("/api/projects")).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "USER")
  public void testAuthenticatedEndpointsAccessibleWithAuth() throws Exception {
    // Test endpoints with authentication
    mockMvc
        .perform(get("/api/user/profile"))
        .andExpect(status().isNotFound()); // Endpoint might not exist, but should not be 401/403

    mockMvc
        .perform(get("/api/projects"))
        .andExpect(status().isOk()); // Endpoint exists and should be accessible (200 OK)
  }
}
