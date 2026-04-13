package org.example.service;

import org.example.model.User;
import org.example.model.AuthToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.Claims;
import java.security.Key;
import java.util.*;
import java.util.Locale;

/**
 * Authentication Service - FIREBASE ONLY (STRICT ENFORCEMENT)
 * 
 * ⚠️ MASTER RULE: NO fallback, NO local authentication
 * ⚠️ ONLY Firebase Authentication is supported
 * ⚠️ NO local user store (auth/users.json deleted)
 * ⚠️ NO BCrypt password hashing
 * ⚠️ NO rate limiting (Firebase handles that)
 * ⚠️ NO MFA generation (Firebase handles that)
 * 
 * Handles ONLY:
 * 1. Firebase ID token exchange → Backend JWT
 * 2. JWT token generation for sessions
 * 3. JWT token validation
 * 4. Token refresh
 * 5. User provisioning from Firebase tokens
 *
 * ALL authentication MUST use Firebase:
 * - Client: Sign in with Firebase Auth SDK (email + password)
 * - Client: Get Firebase ID token
 * - Client: POST /api/auth/firebase-login with ID token
 * - Backend: Verify with Firebase Admin SDK
 * - Backend: Return backend JWT session token
 *
 * @author SupremeAI Security Team
 * @since 2026-04-09 (FIREBASE-ONLY ENFORCEMENT)
 */
@Service
public class AuthenticationService {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    
    // Backend JWT Configuration
    private static final String JWT_SECRET = System.getenv("JWT_SECRET") != null 
        ? System.getenv("JWT_SECRET") 
        : "supremeai-secret-key-change-in-production-32-char-minimum";
    
    private static final long TOKEN_EXPIRATION_HOURS = 24;
    private static final long REFRESH_TOKEN_EXPIRATION_DAYS = 7;
    
    @Autowired
    private FirebaseService firebaseService;

    /**
     * Exchange Firebase ID Token for Backend JWT
     * 
     * This is the ONLY authentication entry point.
     * 
     * Flow (all clients: web, mobile, desktop):
     *   1. Client signs in with Firebase Auth SDK using email + password
     *   2. Client gets Firebase ID token via getIdToken()
     *   3. Client POSTs token here: POST /api/auth/firebase-login with {"idToken": "..."}
     *   4. Backend verifies with Firebase Admin SDK
     *   5. Backend finds or creates matching SupremeAI user
     *   6. Backend returns session JWT + refresh token
     * 
     * @param idToken Firebase ID token from Firebase Auth SDK
     * @return AuthToken containing backend JWT session
     * @throws IllegalArgumentException if token is invalid, user disabled, etc.
     */
    public AuthToken loginWithFirebaseToken(String idToken) throws Exception {
        if (idToken == null || idToken.isBlank()) {
            throw new IllegalArgumentException("Firebase ID token is required");
        }

        // Verify token with Firebase Admin SDK
        com.google.firebase.auth.FirebaseToken decoded;
        try {
            decoded = com.google.firebase.auth.FirebaseAuth.getInstance().verifyIdToken(idToken);
        } catch (Exception e) {
            logger.warn("Firebase ID token verification failed: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid Firebase ID token");
        }

        String email = decoded.getEmail();
        String firebaseUid = decoded.getUid();
        String firebaseDisplayName = decoded.getName();

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Firebase token does not contain an email address");
        }

        // Find matching SupremeAI user (or auto-provision)
        User user = getUserByEmail(email);
        if (user == null) {
            user = provisionFirebaseUser(firebaseUid, email, firebaseDisplayName);
            logger.info("Auto-provisioned SupremeAI user from Firebase Auth: {}", email);
        }

        if (!user.isActive()) {
            throw new IllegalArgumentException("User account is disabled");
        }

        // Update last login timestamp
        user.setLastLogin(System.currentTimeMillis());
        firebaseService.updateUser(user);

        // Generate backend session JWT
        String token = generateJWT(user);
        String refreshToken = generateRefreshToken(user);

        logger.info("Firebase Auth login successful: email={}, uid={}, role={}", 
            email, firebaseUid, user.getRole());
        
        return new AuthToken(token, refreshToken, user, TOKEN_EXPIRATION_HOURS * 3600);
    }

    /**
     * Auto-provision a new SupremeAI user from Firebase token data
     * Called when a user logs in via Firebase for the first time
     */
    private User provisionFirebaseUser(String firebaseUid, String email, String displayName) {
        // Sanitize username from email or display name
        String username = displayName != null && !displayName.isBlank()
            ? displayName
            : email.split("@")[0];
        
        String sanitizedUsername = username
            .trim()
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9._-]", "_");

        // Create new user
        User user = new User();
        user.setId(firebaseUid);
        user.setUsername(sanitizedUsername.isBlank() ? "firebase_user" : sanitizedUsername);
        user.setEmail(email);
        user.setPasswordHash("FIREBASE_AUTH_ONLY");  // Marker: no local password
        user.setActive(true);
        user.setRole("FREE");  // ✅ FIXED: Default to FREE tier, NOT admin
        user.setCreatedAt(System.currentTimeMillis());
        user.setLastLogin(System.currentTimeMillis());
        user.setPermissions(new ArrayList<>());
        
        // Log audit trail
        logger.info("✅ New user provisioned from Firebase: {} (tier: FREE, requires admin promotion)", email);
        
        return user;
    }

    /**
     * Get user by email from Firebase
     */
    public User getUserByEmail(String email) throws Exception {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }

        if (!firebaseService.isInitialized()) {
            logger.warn("Firebase not initialized");
            return null;
        }

        return firebaseService.getUserByEmail(email);
    }

    /**
     * Get user by ID from Firebase
     */
    public User getUserById(String userId) throws Exception {
        if (userId == null || userId.trim().isEmpty()) {
            return null;
        }

        if (!firebaseService.isInitialized()) {
            logger.warn("Firebase not initialized");
            return null;
        }

        return firebaseService.getUserById(userId);
    }

    /**
     * Get user by username from Firebase
     */
    public User getUserByUsername(String username) throws Exception {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }

        if (!firebaseService.isInitialized()) {
            logger.warn("Firebase not initialized");
            return null;
        }

        return firebaseService.getUserByUsername(username);
    }

    /**
     * Validate JWT token and extract user
     * 
     * @param token Backend JWT token
     * @return User if token is valid
     * @throws Exception if token is invalid or user not found
     */
    public User validateToken(String token) throws Exception {
        try {
            Claims claims = Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
            
            String username = claims.getSubject();
            
            User user = getUserByUsername(username);
            if (user == null || !user.isActive()) {
                throw new Exception("User not found or inactive");
            }
            
            return user;
        } catch (Exception e) {
            logger.warn("Invalid JWT token: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Refresh access token using a refresh token
     * 
     * @param refreshToken Refresh token from login
     * @return New access token + new refresh token
     * @throws Exception if refresh token is invalid
     */
    public AuthToken refreshToken(String refreshToken) throws Exception {
        User user = validateToken(refreshToken);
        String newToken = generateJWT(user);
        String newRefreshToken = generateRefreshToken(user);
        
        logger.info("Token refreshed for user: {}", user.getUsername());
        
        return new AuthToken(newToken, newRefreshToken, user, TOKEN_EXPIRATION_HOURS * 3600);
    }

    /**
     * Check if user has admin role
     */
    public boolean isAdmin(User user) {
        return user != null && user.getRole() != null && "admin".equalsIgnoreCase(user.getRole());
    }

    // =========== PRIVATE JWT HELPER METHODS ===========

    /**
     * Generate backend JWT token (24 hour expiration)
     */
    private String generateJWT(User user) {
        return Jwts.builder()
            .subject(user.getUsername())
            .claim("userId", user.getId())
            .claim("email", user.getEmail())
            .claim("role", user.getRole() != null ? user.getRole() : "user")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + TOKEN_EXPIRATION_HOURS * 3600 * 1000))
            .signWith(getSigningKey())
            .compact();
    }

    /**
     * Generate refresh token (7 day expiration)
     */
    private String generateRefreshToken(User user) {
        return Jwts.builder()
            .subject(user.getUsername())
            .claim("type", "refresh")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION_DAYS * 24 * 3600 * 1000))
            .signWith(getSigningKey())
            .compact();
    }

    /**
     * Get signing key for JWT (must be 256 bits minimum for HS256)
     */
    private Key getSigningKey() {
        byte[] keyBytes = JWT_SECRET.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
