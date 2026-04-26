package com.supremeai.service;

import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.supremeai.model.User;
import com.supremeai.model.UserTier;
import com.supremeai.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CachePut;

/**
 * Service for creating and managing user accounts using Firebase Authentication.
 *
 * Features:
 * - Create Firebase Auth accounts from pre-saved email/password in Firestore
 * - Bulk account provisioning from stored credentials
 * - User profile management (tier, status, quota)
 * - Account listing and status tracking
 */
@Service
public class UserAccountService {

    private static final Logger log = LoggerFactory.getLogger(UserAccountService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired(required = false)
    private Firestore firestore;

    /**
     * Create a single Firebase Auth user account with email and password.
     * Also creates the corresponding Firestore user document.
     *
     * @param email    The email for the new account
     * @param password The password for the new account
     * @param displayName The display name (optional)
     * @param tier     The user tier (defaults to FREE)
     * @return The created User record
     */
    public User createAccount(String email, String password, String displayName, UserTier tier) {
        try {
            // Check if user already exists in Firebase Auth
            try {
                UserRecord existingUser = FirebaseAuth.getInstance().getUserByEmail(email);
                if (existingUser != null) {
                    log.warn("Firebase Auth user already exists for email: {}", email);
                    // Return the existing Firestore user or create one
                    User existing = userRepository.findByFirebaseUid(existingUser.getUid()).block();
                    if (existing != null) return existing;

                    // Firestore doc missing but Auth exists - create the doc
                    return createUserDocument(existingUser.getUid(), email,
                            displayName != null ? displayName : existingUser.getDisplayName(), tier);
                }
            } catch (FirebaseAuthException e) {
                // User doesn't exist - proceed to create
            }

            // Create Firebase Auth account
            UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                    .setEmail(email)
                    .setPassword(password)
                    .setDisplayName(displayName != null ? displayName : email.split("@")[0])
                    .setEmailVerified(false)
                    .setDisabled(false);

            UserRecord userRecord = FirebaseAuth.getInstance().createUser(request);

            // Set custom claims for tier if not FREE
            if (tier != null && tier != UserTier.FREE) {
                Map<String, Object> claims = new HashMap<>();
                claims.put("tier", tier.name());
                if (tier == UserTier.ADMIN) {
                    claims.put("admin", true);
                    claims.put("role", "ADMIN");
                }
                FirebaseAuth.getInstance().setCustomUserClaims(userRecord.getUid(), claims);
            }

            // Create Firestore user document
            User user = createUserDocument(userRecord.getUid(), email,
                    displayName != null ? displayName : userRecord.getDisplayName(), tier);

            log.info("Created Firebase Auth account for: {} (uid: {})", email, userRecord.getUid());
            return user;

        } catch (FirebaseAuthException e) {
            log.error("Failed to create Firebase Auth account for {}: {}", email, e.getMessage());
            throw new RuntimeException("Account creation failed: " + e.getMessage(), e);
        }
    }

     /**
      * Create user accounts in bulk from pre-saved credentials stored in a Firestore collection.
      *
      * The credentials collection should have documents with:
      *   - email: String (required)
      *   - password: String (required)
      *   - displayName: String (optional)
      *   - tier: String (optional, defaults to FREE)
      *   - status: String (will be updated to "created" or "error")
      *   - uid: String (will be populated after creation)
      *
      * @param collectionName The Firestore collection containing pre-saved credentials
      * @return CompletableFuture with summary of created accounts
      */
    @Async
    public CompletableFuture<Map<String, Object>> createAccountsFromCollection(String collectionName) {
        if (firestore == null) {
            throw new IllegalStateException("Firestore is not available. Ensure cloud profile is active.");
        }

        int created = 0;
        int skipped = 0;
        int errors = 0;
        List<String> createdEmails = new ArrayList<>();
        List<String> errorEmails = new ArrayList<>();

        try {
            var snapshots = firestore.collection(collectionName).get().get().getDocuments();

            for (var doc : snapshots) {
                String email = doc.getString("email");
                String password = doc.getString("password");
                String displayName = doc.getString("displayName");
                String tierStr = doc.getString("tier");

                // Skip if already processed
                String status = doc.getString("status");
                if ("created".equals(status)) {
                    skipped++;
                    continue;
                }

                if (email == null || password == null) {
                    errors++;
                    errorEmails.add(doc.getId() + ": missing email or password");
                    continue;
                }

                try {
                    UserTier tier = tierStr != null ? UserTier.valueOf(tierStr.toUpperCase()) : UserTier.FREE;
                    User user = createAccount(email, password, displayName, tier);

                    // Update the credential document to mark as created
                    firestore.collection(collectionName).document(doc.getId())
                            .update(Map.of(
                                    "status", "created",
                                    "uid", user.getFirebaseUid(),
                                    "createdAt", LocalDateTime.now().toString()
                            )).get();

                    created++;
                    createdEmails.add(email);
                } catch (Exception e) {
                    errors++;
                    errorEmails.add(email + ": " + e.getMessage());

                    // Update the credential document to mark as error
                    try {
                        firestore.collection(collectionName).document(doc.getId())
                                .update(Map.of("status", "error", "errorMessage", e.getMessage())).get();
                    } catch (Exception ignored) {}
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to read credentials collection: {}", e.getMessage());
            throw new RuntimeException("Failed to read credentials: " + e.getMessage(), e);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalProcessed", created + skipped + errors);
        summary.put("created", created);
        summary.put("skipped", skipped);
        summary.put("errors", errors);
        summary.put("createdEmails", createdEmails);
        summary.put("errorDetails", errorEmails);
        return CompletableFuture.completedFuture(summary);
    }

    /**
     * Create the Firestore user document.
     */
    private User createUserDocument(String uid, String email, String displayName, UserTier tier) {
        User user = new User(uid, email, displayName != null ? displayName : email.split("@")[0]);
        user.setTier(tier != null ? tier : UserTier.FREE);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now().toString());
        user.setUpdatedAt(LocalDateTime.now().toString());
        return userRepository.save(user).block();
    }

        /**
     * Update a user's tier.
     */
    @CachePut(value = "user_sessions", key = "#userId")
    public User updateUserTier(String userId, UserTier newTier) {
        User user = userRepository.findByFirebaseUid(userId).block();
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + userId);
        }

        user.setTier(newTier);
        user.setUpdatedAt(LocalDateTime.now().toString());
        user = userRepository.save(user).block();

        // Update Firebase custom claims
        try {
            Map<String, Object> claims = new HashMap<>();
            claims.put("tier", newTier.name());
            if (newTier == UserTier.ADMIN) {
                claims.put("admin", true);
                claims.put("role", "ADMIN");
            }
            FirebaseAuth.getInstance().setCustomUserClaims(userId, claims);
        } catch (FirebaseAuthException e) {
            log.error("Failed to update custom claims for {}: {}", userId, e.getMessage());
        }

        return user;
    }

    /**
     * List all registered users.
     */
    public List<User> listAllUsers() {
        return userRepository.findAll().collectList().block();
    }

        /**
     * Get a user by Firebase UID.
     */
    @Cacheable(value = "user_sessions", key = "#uid")
    public User getUser(String uid) {
        return userRepository.findByFirebaseUid(uid).block();
    }

        /**
     * Deactivate a user account (sets isActive = false).
     */
    @CachePut(value = "user_sessions", key = "#userId")
    public User deactivateUser(String userId) {
        User user = userRepository.findByFirebaseUid(userId).block();
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + userId);
        }
        user.setIsActive(false);
        user.setUpdatedAt(LocalDateTime.now().toString());

        // Disable in Firebase Auth
        try {
            UserRecord.UpdateRequest request = new UserRecord.UpdateRequest(userId).setDisabled(true);
            FirebaseAuth.getInstance().updateUser(request);
        } catch (FirebaseAuthException e) {
            log.error("Failed to disable Firebase Auth user {}: {}", userId, e.getMessage());
        }

        return userRepository.save(user).block();
    }
}
