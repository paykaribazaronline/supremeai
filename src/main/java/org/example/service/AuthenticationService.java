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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;

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
    
    // Rate limiting configuration
    private final Map<String, List<Long>> loginAttempts = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 5;
    private static final long ATTEMPT_WINDOW_MS = 5 * 60 * 1000; // 5 minutes

    // MFA configuration
    private final Map<String, String[]> mfaCodes = new ConcurrentHashMap<>();
    private static final long MFA_EXPIRY_MS = 5 * 60 * 1000; // 5 minutes
    
    // In-memory user cache (fast local access, synced with Firebase async)
    private final Map<String, User> userCache = new ConcurrentHashMap<>();
    
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
        user.setId(username);
        
        // Save to in-memory cache IMMEDIATELY (Firebase is async)
        userCache.put(username, user);
        logger.debug("✅ User added to in-memory cache: {}", username);
        
        // Also save to Firebase (async, will eventually persist)
        firebaseService.saveUser(user);

        // Keep Firebase Authentication in sync so client SDK login works.
        syncAdminToFirebaseAuth(email, password, username);
        
        logger.info("✅ Admin user registered: {}", username);
        return user;
    }
    
    /**
     * Login user and generate JWT token
     * Supports both email and username as login identifier
     */
    public AuthToken login(String usernameOrEmail, String password) throws Exception {
        // Get user (by email or username)
        User user = getUserByEmailOrUsername(usernameOrEmail);
        if (user == null) {
            throw new IllegalArgumentException("Invalid email, username or password");
        }
        
        // Verify password
        if (!verifyPassword(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }
        
        // Check if user is active
        if (!user.isActive()) {
            throw new IllegalArgumentException("User account is disabled");
        }
        
        // Rate limiting
        long now = System.currentTimeMillis();
        String username = user.getUsername();
        loginAttempts.putIfAbsent(username, new ArrayList<>());
        List<Long> attempts = loginAttempts.get(username);
        attempts.removeIf(ts -> now - ts > ATTEMPT_WINDOW_MS);
        if (attempts.size() >= MAX_ATTEMPTS) {
            throw new IllegalArgumentException("Too many login attempts. Please try again later.");
        }
        attempts.add(now);

        // Repair Firebase Auth credentials before any MFA challenge so
        // email/password clients can recover on the next sign-in attempt.
        syncAdminToFirebaseAuth(user.getEmail(), password, user.getUsername());
        
        // MFA required for admin
        if (user.getRole() != null && user.getRole().equals("ADMIN")) {
            generateAndSendMfaCode(username, user.getEmail());
            throw new IllegalArgumentException("MFA required. Code sent to email.");
        }

        // Backward-compat migration: ensure existing users are present in
        // Firebase Authentication after a successful legacy password login.
        syncAdminToFirebaseAuth(user.getEmail(), password, user.getUsername());
        
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
     * Generate and send MFA code to user email
     */
    public void generateAndSendMfaCode(String username, String email) {
        String code = String.valueOf(100000 + new Random().nextInt(900000));
        long expiry = System.currentTimeMillis() + MFA_EXPIRY_MS;
        mfaCodes.put(username, new String[]{code, String.valueOf(expiry)});
        // TODO: Integrate with email service
        logger.info("[MFA] Code for {}: {} (expires in 5 min)", username, code);
        // In production, send code to email
    }

    /**
     * Verify MFA code for user
     */
    public boolean verifyMfaCode(String username, String code) {
        String[] entry = mfaCodes.get(username);
        if (entry == null) return false;
        long expiry = Long.parseLong(entry[1]);
        if (System.currentTimeMillis() > expiry) {
            mfaCodes.remove(username);
            return false;
        }
        boolean valid = entry[0].equals(code);
        if (valid) mfaCodes.remove(username);
        return valid;
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

    public boolean isAdmin(User user) {
        return user != null && user.getRole() != null && "admin".equalsIgnoreCase(user.getRole());
    }
    
    /**
     * Get user by username (checks cache first for speed)
     */
    public User getUserByUsername(String username) throws Exception {
        // Check in-memory cache first (fast)
        if (userCache.containsKey(username)) {
            logger.debug("✅ User found in cache: {}", username);
            return userCache.get(username);
        }
        
        // Fall back to Firebase
        User user = firebaseService.getUserByUsername(username);
        if (user != null) {
            userCache.put(username, user);
        }
        return user;
    }
    
    /**
     * Get user by email (checks Firebase)
     */
    public User getUserByEmail(String email) throws Exception {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }

        for (User cachedUser : userCache.values()) {
            if (cachedUser != null && email.equalsIgnoreCase(cachedUser.getEmail())) {
                return cachedUser;
            }
        }

        return firebaseService.getUserByEmail(email);
    }
    
    /**
     * Get user by email OR username (login support)
     * Checks both email and username
     */
    public User getUserByEmailOrUsername(String identifier) throws Exception {
        if (identifier == null || identifier.trim().isEmpty()) {
            return null;
        }
        
        // Check cache by username first (fast)
        if (userCache.containsKey(identifier)) {
            logger.debug("✅ User found in cache: {}", identifier);
            return userCache.get(identifier);
        }
        
        // Try Firebase by username
        User user = firebaseService.getUserByUsername(identifier);
        if (user != null) {
            userCache.put(identifier, user);
            logger.debug("✅ User found by username: {}", identifier);
            return user;
        }
        
        // Try Firebase by email
        user = firebaseService.getUserByEmail(identifier);
        if (user != null) {
            userCache.put(user.getUsername(), user);
            logger.debug("✅ User found by email: {}", identifier);
            return user;
        }

        for (User cachedUser : userCache.values()) {
            if (cachedUser != null && identifier.equalsIgnoreCase(cachedUser.getEmail())) {
                logger.debug("✅ User found in cache by email: {}", identifier);
                return cachedUser;
            }
        }
        
        logger.debug("❌ User not found (email or username): {}", identifier);
        return null;
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
            .subject(user.getUsername())
            .claim("userId", user.getId())
            .claim("role", user.getRole())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(TOKEN_EXPIRATION_HOURS)))
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
            .expiration(new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(REFRESH_TOKEN_EXPIRATION_DAYS)))
            .signWith(getSigningKey())
            .compact();
    }
    
    /**
     * Public method to hash password for Firebase export
     * Used by utility endpoints to generate hashes for manual user creation
     */
    public String hashPasswordForExport(String password) {
        return hashPassword(password);
    }
    
    // ============ PRIVATE HELPER METHODS ============
    
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

    // ============ FIREBASE AUTH SYNC ============

    /**
     * Authenticate using a Firebase ID token issued by the Firebase Auth SDK.
     * Verifies the token with the Firebase Admin SDK, then loads the matching
     * SupremeAI user by email and returns a backend session JWT.
     *
     * This lets all three clients (localhost, React, Flutter) sign in via
     * Firebase Auth and exchange the Firebase ID token for a backend JWT.
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
            logger.warn("❌ Firebase ID token verification failed: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid Firebase ID token");
        }

        String email = decoded.getEmail();
        String firebaseUid = decoded.getUid();
        String firebaseDisplayName = decoded.getName();

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Firebase token does not contain an email address");
        }

        // Find matching SupremeAI user
        User user = getUserByEmailOrUsername(email);
        if (user == null) {
            user = provisionFirebaseUser(firebaseUid, email, firebaseDisplayName);
            logger.info("✅ Auto-provisioned SupremeAI user from Firebase Auth: {}", email);
        }

        if (!user.isActive()) {
            throw new IllegalArgumentException("User account is disabled");
        }

        // Update last login
        user.setLastLogin(System.currentTimeMillis());
        firebaseService.updateUser(user);

        // Issue backend session JWT
        String token = generateJWT(user);
        String refreshToken = generateRefreshToken(user);

        logger.info("✅ Firebase Auth login: email={} uid={} role={}", email, firebaseUid, user.getRole());
        return new AuthToken(token, refreshToken, user, TOKEN_EXPIRATION_HOURS * 3600);
    }

    private User provisionFirebaseUser(String firebaseUid, String email, String displayName) {
        String username = displayName != null && !displayName.isBlank()
            ? displayName
            : email.split("@")[0];
        String sanitizedUsername = username
            .trim()
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9._-]", "_");

        User user = new User();
        user.setId(firebaseUid);
        user.setUsername(sanitizedUsername.isBlank() ? "firebase_user" : sanitizedUsername);
        user.setEmail(email);
        user.setPasswordHash("FIREBASE_AUTH_ONLY");
        user.setActive(true);
        user.setRole("admin");
        user.setCreatedAt(System.currentTimeMillis());
        user.setLastLogin(System.currentTimeMillis());
        user.setPermissions(new ArrayList<>());

        firebaseService.saveUser(user);
        userCache.put(user.getUsername(), user);
        return user;
    }

    /**
     * Ensure the given admin user exists in Firebase Authentication.
     * Called during system seeding so that all three clients can sign in
     * via Firebase Auth SDK using the same email/password credentials.
     *
     * Non-fatal: if Firebase Auth is unavailable, custom-JWT auth still works.
     */
    public void syncAdminToFirebaseAuth(String email, String plainPassword, String displayName) {
        try {
            com.google.firebase.auth.FirebaseAuth fbAuth =
                com.google.firebase.auth.FirebaseAuth.getInstance();

            try {
                com.google.firebase.auth.UserRecord existingUser = fbAuth.getUserByEmail(email);
                com.google.firebase.auth.UserRecord.UpdateRequest updateRequest =
                    new com.google.firebase.auth.UserRecord.UpdateRequest(existingUser.getUid())
                        .setPassword(plainPassword)
                        .setDisplayName(displayName)
                        .setEmailVerified(true);
                fbAuth.updateUser(updateRequest);
                logger.info("ℹ️ Firebase Auth user updated: {}", email);
            } catch (com.google.firebase.auth.FirebaseAuthException notFound) {
                com.google.firebase.auth.UserRecord.CreateRequest req =
                    new com.google.firebase.auth.UserRecord.CreateRequest()
                        .setEmail(email)
                        .setPassword(plainPassword)
                        .setDisplayName(displayName)
                        .setEmailVerified(true);
                fbAuth.createUser(req);
                logger.info("✅ Firebase Auth user created for admin: {}", email);
            }
        } catch (Exception e) {
            logger.warn("⚠️ Could not sync admin to Firebase Auth (non-fatal): {}", e.getMessage());
        }
    }
}
