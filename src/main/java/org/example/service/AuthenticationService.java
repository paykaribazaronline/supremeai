package org.example.service;

import org.example.model.User;
import org.example.model.AuthToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.Claims;
import java.security.Key;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Authentication Service
 * 
 * Handles:
 * - User login/registration
 * - Password hashing (BCrypt)
 * - JWT token generation
 * - Token validation
 * - User permissions
 * 
 * Stores users in Firebase Firestore collection: "users"
 */
@Service
public class AuthenticationService {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    
    // JWT Configuration
    private static final String JWT_SECRET = System.getenv("JWT_SECRET") != null 
        ? System.getenv("JWT_SECRET") 
        : "supremeai-secret-key-change-in-production-32-char-minimum";
    
    private static final long TOKEN_EXPIRATION_HOURS = 24;
    private static final long REFRESH_TOKEN_EXPIRATION_DAYS = 7;
    
    @Autowired
    private FirebaseService firebaseService;
    
    /**
     * Register a new admin user
     */
    public User registerUser(String username, String email, String password) throws Exception {
        // Validate input
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        
        // Check if user exists
        User existingUser = getUserByUsername(username);
        if (existingUser != null) {
            throw new IllegalArgumentException("Username already exists");
        }
        
        // Create user
        String passwordHash = hashPassword(password);
        User user = new User(username, email, passwordHash);
        
        // Save to Firebase
        firebaseService.saveUser(user);
        
        logger.info("✅ Admin user registered: {}", username);
        return user;
    }
    
    /**
     * Login user and generate JWT token
     */
    public AuthToken login(String username, String password) throws Exception {
        // Get user
        User user = getUserByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("Invalid username or password");
        }
        
        // Verify password
        if (!verifyPassword(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }
        
        // Check if user is active
        if (!user.isActive()) {
            throw new IllegalArgumentException("User account is disabled");
        }
        
        // Update last login
        user.setLastLogin(System.currentTimeMillis());
        firebaseService.updateUser(user);
        
        // Generate tokens
        String token = generateJWT(user);
        String refreshToken = generateRefreshToken(user);
        
        logger.info("✅ User logged in: {}", username);
        
        // Return token (without password hash)
        AuthToken authToken = new AuthToken(token, refreshToken, user, TOKEN_EXPIRATION_HOURS * 3600);
        return authToken;
    }
    
    /**
     * Validate JWT token and extract user
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
            logger.warn("❌ Invalid token: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Refresh access token using refresh token
     */
    public AuthToken refreshToken(String refreshToken) throws Exception {
        User user = validateToken(refreshToken);
        String newToken = generateJWT(user);
        String newRefreshToken = generateRefreshToken(user);
        
        logger.info("✅ Token refreshed for user: {}", user.getUsername());
        
        return new AuthToken(newToken, newRefreshToken, user, TOKEN_EXPIRATION_HOURS * 3600);
    }
    
    /**
     * Get user by username
     */
    public User getUserByUsername(String username) throws Exception {
        return firebaseService.getUserByUsername(username);
    }
    
    /**
     * Get user by ID
     */
    public User getUserById(String userId) throws Exception {
        return firebaseService.getUserById(userId);
    }
    
    /**
     * Update user permissions
     */
    public void disableUser(String userId) throws Exception {
        User user = getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        
        user.setActive(false);
        firebaseService.updateUser(user);
        
        logger.info("✅ User disabled: {}", user.getUsername());
    }
    
    /**
     * Get all users (admin only)
     */
    public List<User> getAllUsers() throws Exception {
        return firebaseService.getAllUsers();
    }
    
    /**
     * Change user password
     */
    public void changePassword(String userId, String oldPassword, String newPassword) throws Exception {
        User user = getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        
        // Verify old password
        if (!verifyPassword(oldPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        
        // Validate new password
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters");
        }
        
        // Hash and save
        user.setPasswordHash(hashPassword(newPassword));
        firebaseService.updateUser(user);
        
        logger.info("✅ Password changed for user: {}", user.getUsername());
    }
    
    // ============ PRIVATE HELPER METHODS ============
    
    /**
     * Generate JWT token (24 hour expiration)
     */
    private String generateJWT(User user) {
        return Jwts.builder()
            .setSubject(user.getUsername())
            .claim("userId", user.getId())
            .claim("role", user.getRole())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(TOKEN_EXPIRATION_HOURS)))
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }
    
    /**
     * Generate refresh token (7 day expiration)
     */
    private String generateRefreshToken(User user) {
        return Jwts.builder()
            .setSubject(user.getUsername())
            .claim("type", "refresh")
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(REFRESH_TOKEN_EXPIRATION_DAYS)))
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }
    
    /**
     * Get signing key for JWT
     */
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(JWT_SECRET.getBytes());
    }
    
    /**
     * Hash password using BCrypt
     */
    private String hashPassword(String password) {
        // Using simple hash for demo - in production use Spring Security's BCryptPasswordEncoder
        return org.springframework.security.crypto.bcrypt.BCrypt.hashpw(password, 
            org.springframework.security.crypto.bcrypt.BCrypt.gensalt());
    }
    
    /**
     * Verify password against hash
     */
    private boolean verifyPassword(String password, String hash) {
        return org.springframework.security.crypto.bcrypt.BCrypt.checkpw(password, hash);
    }
}
