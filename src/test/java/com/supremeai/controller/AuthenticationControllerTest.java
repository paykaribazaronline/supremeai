package com.supremeai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supremeai.model.User;
import com.supremeai.model.UserTier;
import com.supremeai.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for this test
public class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private FirebaseAuth firebaseAuth;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testFirebaseLogin_InvalidToken() throws Exception {
        // Mock static Firebase Auth to throw exception
        try (MockedStatic<FirebaseAuth> mockedAuth = mockStatic(FirebaseAuth.class)) {
            mockedAuth.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
            when(firebaseAuth.verifyIdToken(anyString())).thenThrow(new com.google.firebase.auth.FirebaseAuthException(
                new com.google.firebase.FirebaseException(ErrorCode.INVALID_ARGUMENT, "Invalid token", new Exception())
            ));

            Map<String, String> request = new HashMap<>();
            request.put("idToken", "invalid-token");

            mockMvc.perform(post("/api/auth/firebase-login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Auth failed")));
        }
    }

    @Test
    void testFirebaseLogin_ExistingUser() throws Exception {
        // Mocking Firebase decoded token
        FirebaseToken decodedToken = mock(FirebaseToken.class);
        when(decodedToken.getUid()).thenReturn("test-uid");
        when(decodedToken.getEmail()).thenReturn("test@example.com");
        Map<String, Object> claims = new HashMap<>();
        claims.put("name", "Test User");
        when(decodedToken.getClaims()).thenReturn(claims);

        // Mocking User in database
        User existingUser = new User("test-uid", "test@example.com", "Test User");
        existingUser.setTier(UserTier.FREE);
        when(userRepository.findByFirebaseUid("test-uid")).thenReturn(Optional.of(existingUser));

        try (MockedStatic<FirebaseAuth> mockedAuth = mockStatic(FirebaseAuth.class)) {
            mockedAuth.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
            when(firebaseAuth.verifyIdToken(anyString())).thenReturn(decodedToken);

            Map<String, String> request = new HashMap<>();
            request.put("idToken", "valid-token");

            mockMvc.perform(post("/api/auth/firebase-login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.isNewUser").value(false))
                    .andExpect(jsonPath("$.user.email").value("test@example.com"));

            verify(userRepository).save(any(User.class));
        }
    }
}
