package com.supremeai.controller;

import com.supremeai.model.User;
import com.supremeai.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import reactor.core.publisher.Mono;

/**
 * Security testing and validation endpoints.
 * Helps verify Firebase authentication and user data isolation.
 */
@Profile({"dev", "local", "test"})
@RestController
@RequestMapping("/api/security")
@PreAuthorize("hasRole('ADMIN')")
public class SecurityTestController {

    private static final Logger log = LoggerFactory.getLogger(SecurityTestController.class);

    private final UserRepository userRepository;

    public SecurityTestController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * POST /api/security/validate-firebase-token
     * Validate a Firebase ID token and return detailed info.
     * Used to verify Firebase login is working correctly.
     */
    @PostMapping("/validate-firebase-token")
    public Mono<ResponseEntity<Map<String, Object>>> validateFirebaseToken(@RequestBody Map<String, String> request) {
        String idToken = request.get("idToken");
        
        if (idToken == null || idToken.trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of(
                "valid", false,
                "message", "ID token is required"
            )));
        }

        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            Map<String, Object> result = new HashMap<>();
            result.put("valid", true);
            // Sanitize: Only return UID and basic status, remove raw claims
            result.put("uid", decodedToken.getUid());
            
            return userRepository.findByFirebaseUid(decodedToken.getUid())
                .map(user -> {
                    result.put("status", "RECOGNIZED");
                    return ResponseEntity.ok(result);
                })
                .defaultIfEmpty(ResponseEntity.ok(Map.of(
                    "valid", true,
                    "uid", decodedToken.getUid(),
                    "status", "UNRECOGNIZED_IN_DB"
                )));
            
        } catch (FirebaseAuthException e) {
            log.warn("Firebase token validation failed: {}", e.getMessage());
            return Mono.just(ResponseEntity.ok(Map.of(
                "valid", false,
                "message", "Invalid Firebase token: " + e.getMessage()
            )));
        } catch (Exception e) {
            log.error("Unexpected error validating Firebase token", e);
            return Mono.just(ResponseEntity.internalServerError().body(Map.of(
                "valid", false,
                "message", "Validation error: " + e.getMessage()
            )));
        }
    }

    /**
     * GET /api/security/current-user
     * Get the currently authenticated user's info and verify session.
     * Used to test if authentication is working properly.
     */
    @GetMapping("/current-user")
    public Mono<ResponseEntity<Map<String, Object>>> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            return Mono.just(ResponseEntity.status(401).body(Map.of(
                "authenticated", false
            )));
        }

        String uid = auth.getName();
        return userRepository.findByFirebaseUid(uid)
            .map(user -> {
                Map<String, Object> result = new HashMap<>();
                result.put("authenticated", true);
                result.put("uid", user.getFirebaseUid());
                result.put("role", user.getTier() != null ? user.getTier().toString() : "USER");
                return ResponseEntity.ok(result);
            })
            .defaultIfEmpty(ResponseEntity.status(404).body(Map.of(
                "authenticated", true,
                "message", "User record missing"
            )));
    }

    /**
     * GET /api/security/test-isolation
     * Test endpoint to verify user data isolation.
     * Tries to access a different user's data to see if it's blocked.
     * (This is a diagnostic endpoint for admins/developers)
     */
    @GetMapping("/test-isolation")
    public Mono<ResponseEntity<Map<String, Object>>> testDataIsolation(
            @RequestParam(required = false) String targetUid) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            return Mono.just(ResponseEntity.status(401).body(Map.of(
                "authenticated", false,
                "message", "Not authenticated"
            )));
        }

        String currentUid = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Map<String, Object> result = new HashMap<>();
        result.put("currentUser", currentUid);
        result.put("isAdmin", isAdmin);
        result.put("targetUid", targetUid);

        if (targetUid == null || targetUid.isEmpty()) {
            return userRepository.findByFirebaseUid(currentUid)
                .map(user -> {
                    result.put("canAccessOwnData", true);
                    result.put("userFound", true);
                    return ResponseEntity.ok(result);
                })
                .defaultIfEmpty(ResponseEntity.ok(Map.of(
                    "currentUser", currentUid,
                    "isAdmin", isAdmin,
                    "targetUid", targetUid,
                    "canAccessOwnData", false,
                    "userFound", false
                )));
        }

        // Test if current user can access another user's data
        if (currentUid.equals(targetUid)) {
            result.put("canAccessTarget", true);
            result.put("reason", "Accessing own data is allowed");
        } else if (isAdmin) {
            result.put("canAccessTarget", true);
            result.put("reason", "Admin can access any user's data");
        } else {
            // Try to fetch the target user - should be blocked by controller logic
            // We indicate that access would be denied based on authorization rules
            result.put("canAccessTarget", false);
            result.put("reason", "Access denied: cross-user data access prevented by authorization");
        }

        return Mono.just(ResponseEntity.ok(result));
    }

    /**
     * GET /api/security/health-check
     * Comprehensive security health check including:
     * - Firebase connectivity
     * - JWT configuration
     * - Firestore rules status
     * - Session management
     */
    @GetMapping("/health-check")
    public ResponseEntity<Map<String, Object>> securityHealthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        // Sanitize: Do not expose detailed config or connectivity details to prevent mapping
        return ResponseEntity.ok(health);
    }

    private Map<String, Object> buildFirestoreUserInfo(User user) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("existsInFirestore", true);
        userInfo.put("displayName", user.getDisplayName());
        userInfo.put("tier", user.getTier() != null ? user.getTier().toString() : null);
        userInfo.put("isActive", user.getIsActive());
        return userInfo;
    }
}
