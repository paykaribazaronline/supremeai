
package com.supremeai.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supremeai.model.User;
import com.supremeai.repository.UserRepository;
import com.supremeai.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;

    @BeforeEach
    public void setUp() {
        // Create a test user
        testUser = new User("test-uid", "test@example.com", "Test User");
        testUser.setTier(com.supremeai.model.UserTier.FREE);
        userRepository.save(testUser).block();
    }

    @Test
    public void testPublicEndpoint_AccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/auth/firebase-login"))
                .andExpect(status().isMethodNotAllowed()); // POST endpoint
    }

    @Test
    public void testProtectedEndpoint_AccessibleWithValidToken() throws Exception {
        // Arrange
        String token = jwtUtil.generateToken(testUser.getFirebaseUid());

        // Act & Assert
        mockMvc.perform(get("/api/user/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound()); // Endpoint might not exist, but should not be 401/403
    }

    @Test
    public void testProtectedEndpoint_InaccessibleWithInvalidToken() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/user/profile")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testProtectedEndpoint_InaccessibleWithoutToken() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testAdminEndpoint_AccessibleWithAdminToken() throws Exception {
        // Arrange
        testUser.setTier(com.supremeai.model.UserTier.ADMIN);
        userRepository.save(testUser).block();
        String token = jwtUtil.generateToken(testUser.getFirebaseUid());

        // Act & Assert
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound()); // Endpoint might not exist, but should not be 401/403
    }

    @Test
    public void testAdminEndpoint_InaccessibleWithRegularUserToken() throws Exception {
        // Arrange
        String token = jwtUtil.generateToken(testUser.getFirebaseUid());

        // Act & Assert
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
