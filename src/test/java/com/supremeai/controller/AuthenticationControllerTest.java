package com.supremeai.controller;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthenticationController.class)
public class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testFirebaseLoginSuccess() throws Exception {
        // Mock FirebaseToken
        FirebaseToken mockToken = mock(FirebaseToken.class);
        when(mockToken.getUid()).thenReturn("test-uid");
        when(mockToken.getEmail()).thenReturn("test@example.com");
        when(mockToken.getClaims()).thenReturn(Map.of("name", "Test User"));

        // Mock FirebaseAuth
        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        when(mockAuth.verifyIdToken("valid-token")).thenReturn(mockToken);

        try (MockedStatic<FirebaseAuth> mockedStatic = mockStatic(FirebaseAuth.class)) {
            mockedStatic.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            Map<String, String> request = new HashMap<>();
            request.put("idToken", "valid-token");

            mockMvc.perform(post("/api/auth/firebase-login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"idToken\":\"valid-token\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.user.id").value("test-uid"));
        }
    }

    @Test
    public void testFirebaseLoginInvalidToken() throws Exception {
        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        FirebaseAuthException mockException = mock(FirebaseAuthException.class);
        when(mockException.getMessage()).thenReturn("Invalid token");
        when(mockAuth.verifyIdToken("invalid-token")).thenThrow(mockException);

        try (MockedStatic<FirebaseAuth> mockedStatic = mockStatic(FirebaseAuth.class)) {
            mockedStatic.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            mockMvc.perform(post("/api/auth/firebase-login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"idToken\":\"invalid-token\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("error"));
        }
    }
}