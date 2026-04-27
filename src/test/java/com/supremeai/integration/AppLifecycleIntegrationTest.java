
package com.supremeai.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supremeai.model.User;
import com.supremeai.repository.UserRepository;
import com.supremeai.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Disabled("Integration tests require proper infrastructure setup")
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
    public void testCompleteUserLifecycle() throws Exception {
        // 1. User registration
        Map<String, String> registerRequest = new HashMap<>();
        registerRequest.put("email", "newuser@example.com");
        registerRequest.put("password", "SecurePassword123!");
        registerRequest.put("username", "New User");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // 2. User login
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", "test@example.com");
        loginRequest.put("password", "SecurePassword123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());

        // 3. Access protected endpoint
        mockMvc.perform(get("/api/user/profile")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"));

        // 4. Create a project
        Map<String, Object> projectRequest = new HashMap<>();
        projectRequest.put("name", "Test Project");
        projectRequest.put("description", "A test project");

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + authToken)
                        .content(objectMapper.writeValueAsString(projectRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Project"));

        // 5. Send a chat message
        Map<String, String> chatRequest = new HashMap<>();
        chatRequest.put("content", "Hello, AI!");
        chatRequest.put("role", "user");

        mockMvc.perform(post("/api/chat/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + authToken)
                        .content(objectMapper.writeValueAsString(chatRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists());

        // 6. Get chat history
        mockMvc.perform(get("/api/chat/history")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // 7. User logout
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        // 8. Verify logout - access should be denied
        mockMvc.perform(get("/api/user/profile")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testAdminWorkflow() throws Exception {
        // Create an admin user
        User adminUser = new User("admin-uid", "admin@example.com", "Admin User");
        adminUser.setTier(com.supremeai.model.UserTier.ADMIN);
        adminUser = userRepository.save(adminUser).block();

        String adminToken = jwtUtil.generateToken(adminUser.getFirebaseUid(), "ADMIN");

        // 1. Access admin dashboard
        mockMvc.perform(get("/api/admin/dashboard/stats")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").exists());

        // 2. Get all users
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // 3. Get recent activity
        mockMvc.perform(get("/api/admin/dashboard/activity")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // 4. Check system health
        mockMvc.perform(get("/api/admin/dashboard/health")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    public void testAIConsensusWorkflow() throws Exception {
        // 1. Submit a question to AI consensus
        Map<String, Object> consensusRequest = new HashMap<>();
        consensusRequest.put("question", "What is the best programming language for web development?");
        consensusRequest.put("providers", java.util.List.of("groq", "openai"));
        consensusRequest.put("timeout", 5000);

        mockMvc.perform(post("/api/consensus/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + authToken)
                        .content(objectMapper.writeValueAsString(consensusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consensusAnswer").exists())
                .andExpect(jsonPath("$.votes").isArray());

        // 2. Get consensus history
        mockMvc.perform(get("/api/consensus/history")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    public void testIntelligenceWorkflow() throws Exception {
        // 1. Analyze a query
        Map<String, Object> analyzeRequest = new HashMap<>();
        analyzeRequest.put("query", "What is the best database for a small web app?");
        analyzeRequest.put("context", "web application");

        mockMvc.perform(post("/api/intelligence/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + authToken)
                        .content(objectMapper.writeValueAsString(analyzeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysis").exists())
                .andExpect(jsonPath("$.confidence").exists());

        // 2. Get recommendations
        mockMvc.perform(get("/api/intelligence/recommendations/web-app")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations").isArray());

        // 3. Get insights
        mockMvc.perform(get("/api/intelligence/insights/user-behavior")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.insights").isArray());
    }
}
