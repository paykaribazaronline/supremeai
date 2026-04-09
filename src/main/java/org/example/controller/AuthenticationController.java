package org.example.controller;

import org.example.model.AuthToken;
import org.example.service.AuthenticationService;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Authentication Controller - FIREBASE ONLY (ENFORCED)
 * 
 * ⚠️ MASTER RULE: ONLY Firebase Authentication is allowed
 * ⚠️ NO alternative authentication methods permitted
 * ⚠️ NO JWT-only auth, NO API Keys for user auth, NO custom tokens
 * 
 * ENFORCED RULES:
 * 1. ALL user authentication MUST use Firebase
 * 2. ALL tokens MUST be Firebase ID Tokens
 * 3. ALL API calls MUST include Authorization: Bearer {firebaseIdToken}
 * 4. NO exceptions, NO alternative auth methods
 * 
 * Token Flow:
 *   Firebase Client SDK → Firebase ID Token → POST /api/auth/firebase-login 
 *   → Backend validates with Firebase Admin SDK → Return backend JWT
 * 
 * REST API endpoints:
 * - POST /api/auth/firebase-login - Firebase Authentication ONLY
 *
 * @author SupremeAI Security Team
 * @since 2026-04-09 (Master Rules Enforcement)
 */
@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);
    
    @Autowired
    private AuthenticationService authService;
    
    /**
     * POST /api/auth/firebase-login
     * Exchange a Firebase Authentication ID token for a SupremeAI backend session JWT.
     *
     * Flow (all three clients):
     *   1. Client signs in with Firebase Auth SDK using email + password
     *   2. Client calls getIdToken() to obtain a short-lived Firebase ID token
     *   3. Client POSTs that token here
     *   4. Backend verifies the token with Firebase Admin SDK
     *   5. Backend looks up the matching SupremeAI user by email
     *   6. Backend returns a backend JWT + sets the admin cookie
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

            logger.info("✅ Firebase Auth login successful for: {}",
                authToken.getUser().getEmail());
            return ResponseEntity.ok(resp);

        } catch (Exception e) {
            logger.error("❌ Firebase login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}
