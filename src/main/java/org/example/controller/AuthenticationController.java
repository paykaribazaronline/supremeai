package org.example.controller;

import org.example.model.User;
import org.example.model.AuthToken;
import org.example.service.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Authentication Controller
 * 
 * REST API endpoints for authentication:
 * - POST /api/auth/login - User login
 * - POST /api/auth/register - User registration (admin only)
 * - POST /api/auth/refresh - Refresh access token
 * - GET /api/auth/me - Get current user info
 * - POST /api/auth/change-password - Change password
 * - GET /api/auth/users - Get all users (admin only)
 * - POST /api/auth/users/{userId}/role - Update user role (admin only)
 * - POST /api/auth/users/{userId}/disable - Disable user (admin only)
 */
@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);
    
    @Autowired
    private AuthenticationService authService;
    
    /**
     * POST /api/auth/login
     * Login user and return JWT token
     * Supports both email and username as login identifier
     * 
     * Body: {
     *   "username": "admin",           // OR use "email"
     *   "password": "your_password"
     * }
     * 
     * Examples:
     * - Login with username: {"username": "supremeai", "password": "Admin@123456!"}
     * - Login with email: {"email": "admin@supremeai.com", "password": "Admin@123456!"}
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        try {
            String usernameOrEmail = request.get("username");
            if (usernameOrEmail == null) {
                usernameOrEmail = request.get("email");
            }
            String password = request.get("password");
            
            if (usernameOrEmail == null || password == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "Username or email and password required"));
            }
            
            AuthToken token = authService.login(usernameOrEmail, password);
            
            // Return user info without password
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("token", token.getToken());
            response.put("refreshToken", token.getRefreshToken());
            response.put("type", token.getType());
            response.put("expiresIn", token.getExpiresIn());
            response.put("user", new AuthToken.UserResponse(token.getUser()));
            
            logger.info("✅ User logged in: {}", usernameOrEmail);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
    
    /**
     * POST /api/auth/register
     * Register new admin user (requires existing admin authentication)
     * 
     * Body: {
     *   "username": "newadmin",
     *   "email": "admin@example.com",
     *   "password": "secure_password"
     * }
     * 
     * Headers: Authorization: Bearer <admin_token>
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Verify admin authentication
            User currentUser = extractUserFromToken(authHeader);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("status", "error", "message", "Admin access required"));
            }
            
            String username = request.get("username");
            String email = request.get("email");
            String password = request.get("password");
            
            if (username == null || email == null || password == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "All fields required"));
            }
            
            User user = authService.registerUser(username, email, password);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Admin user registered successfully");
            response.put("user", new AuthToken.UserResponse(user));
            
            logger.info("✅ New admin registered: {}", username);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Registration failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
    
    /**
     * POST /api/auth/refresh
     * Refresh access token
     * 
     * Body: {
     *   "refreshToken": "your_refresh_token"
     * }
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            if (refreshToken == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "Refresh token required"));
            }
            
            AuthToken token = authService.refreshToken(refreshToken);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("token", token.getToken());
            response.put("refreshToken", token.getRefreshToken());
            response.put("type", token.getType());
            response.put("expiresIn", token.getExpiresIn());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("status", "error", "message", "Invalid refresh token"));
        }
    }
    
    /**
     * GET /api/auth/me
     * Get current user info
     * 
     * Headers: Authorization: Bearer <token>
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = extractUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "Unauthorized"));
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("user", new AuthToken.UserResponse(user));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Failed to get current user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("status", "error", "message", "Invalid token"));
        }
    }
    
    /**
     * POST /api/auth/change-password
     * Change user password
     * 
     * Body: {
     *   "oldPassword": "current_password",
     *   "newPassword": "new_password"
     * }
     * 
     * Headers: Authorization: Bearer <token>
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = extractUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "Unauthorized"));
            }
            
            String oldPassword = request.get("oldPassword");
            String newPassword = request.get("newPassword");
            
            if (oldPassword == null || newPassword == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "All fields required"));
            }
            
            authService.changePassword(user.getId(), oldPassword, newPassword);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Password changed successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Password change failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
    
    /**
     * GET /api/auth/users
     * Get all users (requires admin authentication)
     * 
     * Headers: Authorization: Bearer <admin_token>
     */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User currentUser = extractUserFromToken(authHeader);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("status", "error", "message", "Admin access required"));
            }
            
            List<User> users = authService.getAllUsers();
            List<AuthToken.UserResponse> userResponses = new ArrayList<>();
            
            for (User user : users) {
                userResponses.add(new AuthToken.UserResponse(user));
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("users", userResponses);
            response.put("total", userResponses.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Failed to get users: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
    
    /**
     * POST /api/auth/users/{userId}/disable
     * Disable user account (requires existing admin authentication)
     * 
     * Headers: Authorization: Bearer <admin_token>
     */
    @PostMapping("/users/{userId}/disable")
    public ResponseEntity<?> disableUser(
            @PathVariable String userId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User currentUser = extractUserFromToken(authHeader);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("status", "error", "message", "Admin access required"));
            }
            
            authService.disableUser(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "User disabled");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Failed to disable user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
    
    /**
     * POST /api/auth/bootstrap
     * Register the FIRST admin user (one-time, with optional token verification)
     * If users already exist, requires admin authentication
     * 
     * Body: {
     *   "username": "admin",
     *   "email": "admin@example.com",
     *   "password": "secure_password"
     * }
     * 
     * Headers (optional):
     *   X-Bootstrap-Token: <token from BOOTSTRAP_TOKEN env var>
     */
    @PostMapping("/bootstrap")
    public ResponseEntity<?> bootstrap(
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Bootstrap-Token", required = false) String bootstrapToken) {
        try {
            String username = request.get("username");
            String email = request.get("email");
            String password = request.get("password");
            
            if (username == null || email == null || password == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "All fields required"));
            }
            
            // Check environment variable for bootstrap token requirement
            String expectedBootstrapToken = System.getenv("BOOTSTRAP_TOKEN");
            if (expectedBootstrapToken != null && !expectedBootstrapToken.isEmpty()) {
                // Token verification required
                if (bootstrapToken == null || !bootstrapToken.equals(expectedBootstrapToken)) {
                    logger.warn("❌ Bootstrap attempt failed: Invalid bootstrap token");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("status", "error", "message", "Invalid or missing bootstrap token"));
                }
            }
            
            // Check if any users exist
            List<User> existingUsers = authService.getAllUsers();
            if (existingUsers != null && !existingUsers.isEmpty()) {
                // If users exist, require admin authentication (can't re-bootstrap)
                User currentUser = extractUserFromToken(authHeader);
                if (currentUser == null) {
                    logger.warn("❌ Bootstrap attempt failed: Users already exist, admin auth required");
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("status", "error", "message", "Users already exist. Use /api/auth/register with admin token instead."));
                }
            }
            
            User user = authService.registerUser(username, email, password);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "First admin user registered successfully");
            response.put("user", new AuthToken.UserResponse(user));
            
            logger.info("✅ Bootstrap: First admin registered: {}", username);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Bootstrap registration failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * POST /api/auth/setup
     * ONE-TIME setup endpoint to create initial admin user
     * Requires SUPREMEAI_SETUP_TOKEN environment variable
     * 
     * ⚠️ SECURITY: Only callable once and requires secure token
     * 
     * Body: {
     *   "setupToken": "value_from_SUPREMEAI_SETUP_TOKEN_env",
     *   "username": "admin",
     *   "email": "admin@supremeai.com",
     *   "password": "secure_password"
     * }
     */
    @PostMapping("/setup")
    public ResponseEntity<?> setupInitialAdmin(@RequestBody Map<String, String> request) {
        try {
            // Check if users already exist (first-time-only protection)
            List<User> existingUsers = authService.getAllUsers();
            if (existingUsers != null && !existingUsers.isEmpty()) {
                logger.warn("❌ Setup attempted but users already exist. Blocked!");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                        "status", "error",
                        "message", "System already initialized. Cannot re-run setup.",
                        "existingUsers", existingUsers.size()
                    ));
            }
            
            // Verify setup token from environment variable
            String setupToken = request.get("setupToken");
            String expectedToken = System.getenv("SUPREMEAI_SETUP_TOKEN");
            
            if (expectedToken == null || expectedToken.isEmpty()) {
                logger.error("❌ SUPREMEAI_SETUP_TOKEN not configured in environment");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "status", "error",
                        "message", "Setup not configured. Contact administrator."
                    ));
            }
            
            if (setupToken == null || !setupToken.equals(expectedToken)) {
                logger.warn("❌ Invalid setup token provided");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                        "status", "error",
                        "message", "Invalid setup token"
                    ));
            }
            
            // Create initial admin user
            String username = request.get("username");
            String email = request.get("email");
            String password = request.get("password");
            
            if (username == null || email == null || password == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "Username, email, and password required"));
            }
            
            if (password.length() < 8) {
                return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "Password must be at least 8 characters"));
            }
            
            User adminUser = authService.registerUser(username, email, password);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "✅ Initial admin user created successfully");
            response.put("username", username);
            response.put("email", email);
            response.put("note", "⚠️ Change password on first login. Other users can now be added through API.");
            response.put("user", new AuthToken.UserResponse(adminUser));
            
            logger.info("✅ Initial admin user created via setup: {}", username);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Setup failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * POST /api/auth/hash-password
     * Generate BCrypt hash for a password (for Firebase manual user creation)
     * 
     * Body: {
     *   "password": "your_password"
     * }
     * 
     * Response: {
     *   "password": "your_password",
     *   "hash": "$2a$10$...",
     *   "note": "Use this hash as 'passwordHash' field in Firebase"
     * }
     */
    @PostMapping("/hash-password")
    public ResponseEntity<?> hashPassword(@RequestBody Map<String, String> request) {
        try {
            String password = request.get("password");
            if (password == null || password.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "Password required"));
            }
            
            if (password.length() < 6) {
                return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "Password must be at least 6 characters"));
            }
            
            String hash = authService.hashPasswordForExport(password);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("password", password);
            response.put("hash", hash);
            response.put("note", "Use this hash as 'passwordHash' field in Firebase users collection");
            response.put("firebase_template", Map.of(
                "username", "your_username",
                "email", "user@example.com",
                "passwordHash", hash,
                "active", true,
                "role", "admin",
                "permissions", List.of(),
                "createdAt", System.currentTimeMillis(),
                "lastLogin", 0L
            ));
            
            logger.info("✅ Password hash generated");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Hash generation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // ============ PRIVATE HELPER METHODS ============
    
    /**
     * Extract user from JWT token in Authorization header
     */
    private User extractUserFromToken(String authHeader) throws Exception {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        
        String token = authHeader.substring(7);
        return authService.validateToken(token);
    }
}
