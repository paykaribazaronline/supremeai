package com.supremeai.controller;

import com.supremeai.model.User;
import com.supremeai.model.UserTier;
import com.supremeai.service.UserAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST API for user account management.
 *
 * Endpoints:
 *   POST /api/accounts/create          - Create a single account
 *   POST /api/accounts/bulk-create      - Create accounts from pre-saved Firestore credentials
 *   GET  /api/accounts                  - List all users (admin only)
 *   GET  /api/accounts/{uid}            - Get user details
 *   PUT  /api/accounts/{uid}/tier       - Update user tier (admin only)
 *   PUT  /api/accounts/{uid}/deactivate - Deactivate a user (admin only)
 */
@RestController
@RequestMapping("/api/accounts")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserAccountController {

    @Autowired
    private UserAccountService userAccountService;

    /**
     * POST /api/accounts/create - Create a single user account.
     * Body: { "email": "...", "password": "...", "displayName": "...", "tier": "FREE" }
     */
    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createAccount(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");
        String displayName = body.get("displayName");
        String tierStr = body.getOrDefault("tier", "FREE");

        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "email and password are required"));
        }

        if (password.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 6 characters (Firebase requirement)"));
        }

        try {
            UserTier tier = UserTier.valueOf(tierStr.toUpperCase());
            User user = userAccountService.createAccount(email, password, displayName, tier);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("uid", user.getFirebaseUid());
            response.put("email", user.getEmail());
            response.put("displayName", user.getDisplayName());
            response.put("tier", user.getTier().toString());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid tier: " + tierStr));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/accounts/bulk-create - Create accounts from a Firestore collection
     * containing pre-saved email/password credentials.
     *
     * Body: { "collectionName": "pre_saved_credentials" }
     *
     * The collection should have documents with:
     *   - email (required)
     *   - password (required)
     *   - displayName (optional)
     *   - tier (optional, defaults to FREE)
     */
    @PostMapping("/bulk-create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> bulkCreate(@RequestBody Map<String, String> body) {
        String collectionName = body.get("collectionName");

        if (collectionName == null || collectionName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "collectionName is required"));
        }

        try {
            Map<String, Object> summary = userAccountService.createAccountsFromCollection(collectionName);
            return ResponseEntity.ok(summary);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(503).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/accounts - List all registered users (admin only).
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> listUsers() {
        List<User> users = userAccountService.listAllUsers();
        if (users == null) users = Collections.emptyList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (User u : users) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("uid", u.getFirebaseUid());
            map.put("email", u.getEmail());
            map.put("displayName", u.getDisplayName());
            map.put("tier", u.getTier().toString());
            map.put("isActive", u.getIsActive());
            map.put("currentUsage", u.getCurrentUsage());
            map.put("monthlyQuota", u.getMonthlyQuota());
            map.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : null);
            map.put("lastLoginAt", u.getLastLoginAt() != null ? u.getLastLoginAt().toString() : null);
            result.add(map);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/accounts/{uid} - Get details for a specific user.
     */
    @GetMapping("/{uid}")
    public ResponseEntity<Map<String, Object>> getUser(@PathVariable String uid) {
        User user = userAccountService.getUser(uid);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("uid", user.getFirebaseUid());
        response.put("email", user.getEmail());
        response.put("displayName", user.getDisplayName());
        response.put("tier", user.getTier().toString());
        response.put("isActive", user.getIsActive());
        response.put("currentUsage", user.getCurrentUsage());
        response.put("monthlyQuota", user.getMonthlyQuota());
        response.put("hasQuotaRemaining", user.hasQuotaRemaining());
        response.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
        response.put("lastLoginAt", user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : null);

        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/accounts/{uid}/tier - Update a user's tier (admin only).
     * Body: { "tier": "PRO" }
     */
    @PutMapping("/{uid}/tier")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateTier(
            @PathVariable String uid,
            @RequestBody Map<String, String> body) {
        String tierStr = body.get("tier");
        if (tierStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "tier is required"));
        }

        try {
            UserTier tier = UserTier.valueOf(tierStr.toUpperCase());
            User user = userAccountService.updateUserTier(uid, tier);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "uid", user.getFirebaseUid(),
                    "tier", user.getTier().toString(),
                    "monthlyQuota", user.getMonthlyQuota()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid tier: " + tierStr));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PUT /api/accounts/{uid}/deactivate - Deactivate a user account (admin only).
     */
    @PutMapping("/{uid}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deactivateUser(@PathVariable String uid) {
        try {
            User user = userAccountService.deactivateUser(uid);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "uid", user.getFirebaseUid(),
                    "isActive", user.getIsActive()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }
}
