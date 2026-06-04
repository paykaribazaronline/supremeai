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
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.junit.jupiter.api.Disabled("Requires running Firestore Emulator")
public class AppLifecycleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private String authToken;

    @BeforeEach
    public void setUp() {
        // Clean up before each test
        userRepository.deleteAll().block();

        // Create a test user
        testUser = new User("test-uid", "test@example.com", "Test User");
        testUser.setTier(com.supremeai.model.UserTier.FREE);
        testUser = userRepository.save(testUser).block();

        // Generate auth token
        authToken = jwtUtil.generateToken(testUser.getFirebaseUid(), "USER");
    }

    @Test
    public void testCompleteUserAndProjectLifecycle() throws Exception {
        // 1. User registration
        Map<String, String> registerRequest = new HashMap<>();
        registerRequest.put("email", "newuser@example.com");
        registerRequest.put("password", "SecurePassword123!");
        registerRequest.put("displayName", "New User");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.email").value("newuser@example.com"));

        // 2. Fetch projects (initially empty)
        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        // 3. Create a project
        Map<String, Object> projectRequest = new HashMap<>();
        projectRequest.put("name", "Test Project");
        projectRequest.put("description", "A test project");

        MvcResult result = mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + authToken)
                        .content(objectMapper.writeValueAsString(projectRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Test Project"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
        Map<String, Object> dataMap = (Map<String, Object>) responseMap.get("data");
        String projectId = (String) dataMap.get("id");

        // 4. Update project status
        mockMvc.perform(put("/api/projects/" + projectId + "/status")
                        .param("status", "ACTIVE")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        // 5. Delete project
        mockMvc.perform(delete("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    public void testAdminWorkflow() throws Exception {
        // Create an admin user
        User adminUser = new User("admin-uid", "admin@example.com", "Admin User");
        adminUser.setTier(com.supremeai.model.UserTier.ADMIN);
        adminUser = userRepository.save(adminUser).block();

        String adminToken = jwtUtil.generateToken(adminUser.getFirebaseUid(), "ADMIN");

        // 1. Get plans
        mockMvc.perform(get("/api/admin/plans")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.plans").exists());

        // 2. Get provider rankings
        mockMvc.perform(get("/api/admin/providers/rankings")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rankings").exists());
    }
}
