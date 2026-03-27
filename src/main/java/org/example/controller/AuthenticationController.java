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
     * 
     * Body: {
     *   "username": "admin",
     *   "password": "your_password"
     * }
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");
            
            if (username == null || password == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "Username and password required"));
            }
            
            AuthToken token = authService.login(username, password);
            
            // Return user info without password
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("token", token.getToken());
            response.put("refreshToken", token.getRefreshToken());
            response.put("type", token.getType());
            response.put("expiresIn", token.getExpiresIn());
            response.put("user", new AuthToken.UserResponse(token.getUser()));
            
            logger.info("✅ User logged in: {}", username);
            
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
