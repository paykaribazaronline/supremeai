package com.supremeai.teaching.controllers;

import org.example.model.AuthToken;
import org.example.service.AuthenticationService;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.context.annotation.Profile;

import java.util.*;

/**
 * Teaching Module - Authentication Controller - FIREBASE ONLY
 * 
 * ⚠️ LOCAL AUTHENTICATION REMOVED
 * 
 * REST API endpoints:
 * - POST /api/auth/firebase-login - Firebase Authentication only
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@Profile("teaching")
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    private AuthenticationService authService;
    
    /**
     * POST /api/auth/firebase-login
     * Exchange Firebase ID token for backend JWT (Firebase only)
     * 
     * Body: { "idToken": "<Firebase ID token>" }
     */
    @PostMapping("/firebase-login")
    public ResponseEntity<?> firebaseLogin(@RequestBody @NotNull Map<String, String> request) {
        try {
            String idToken = request.get("idToken");
            if (idToken == null || idToken.isBlank()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "idToken is required"));
            }

            AuthToken authToken = authService.loginWithFirebaseToken(idToken);

            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "success");
            resp.put("token", authToken.getToken());
            resp.put("refreshToken", authToken.getRefreshToken());
            resp.put("type", authToken.getType());
            resp.put("expiresIn", authToken.getExpiresIn());
            resp.put("user", new AuthToken.UserResponse(authToken.getUser()));

            logger.info("✅ Firebase Auth login successful for: {}", authToken.getUser().getEmail());
            return ResponseEntity.ok(resp);

        } catch (Exception e) {
            logger.error("❌ Firebase login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}
