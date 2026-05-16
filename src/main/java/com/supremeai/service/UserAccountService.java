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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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
     */
    public Mono<User> createAccount(String email, String password, String displayName, UserTier tier) {
        return Mono.fromCallable(() -> {
            try {
                return FirebaseAuth.getInstance().getUserByEmail(email);
            } catch (FirebaseAuthException e) {
                return null;
            }
        }).subscribeOn(Schedulers.boundedElastic())
        .flatMap(existingUser -> {
            if (existingUser != null) {
                log.warn("Firebase Auth user already exists for email: {}", email);
                return userRepository.findByFirebaseUid(existingUser.getUid())
                        .switchIfEmpty(createUserDocument(existingUser.getUid(), email,
                                displayName != null ? displayName : existingUser.getDisplayName(), tier));
            }

            // Create Firebase Auth account
            return Mono.fromCallable(() -> {
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
                return userRecord;
            }).subscribeOn(Schedulers.boundedElastic())
            .flatMap(userRecord -> createUserDocument(userRecord.getUid(), email,
                    displayName != null ? displayName : userRecord.getDisplayName(), tier))
            .doOnNext(user -> log.info("Created Firebase Auth account for: {} (uid: {})", email, user.getFirebaseUid()));
        });
    }

    /**
     * Create user accounts in bulk from pre-saved credentials stored in a Firestore collection.
     */
    public Mono<Map<String, Object>> createAccountsFromCollection(String collectionName) {
        if (firestore == null) {
            return Mono.error(new IllegalStateException("Firestore is not available. Ensure cloud profile is active."));
        }

        return Mono.fromCallable(() -> firestore.collection(collectionName).get().get())
                .subscribeOn(Schedulers.boundedElastic())
            .flatMapMany(snapshots -> Flux.fromIterable(snapshots.getDocuments()))
            .flatMap(doc -> {
                String email = doc.getString("email");
                String password = doc.getString("password");
                String displayName = doc.getString("displayName");
                String tierStr = doc.getString("tier");
                String status = doc.getString("status");

                if ("created".equals(status)) {
                    return Mono.just(new BulkResult(BulkStatus.SKIPPED, email, null));
                }

                if (email == null || password == null) {
                    return Mono.just(new BulkResult(BulkStatus.ERROR, doc.getId(), "missing email or password"));
                }

                UserTier tier = tierStr != null ? UserTier.valueOf(tierStr.toUpperCase()) : UserTier.FREE;
                return createAccount(email, password, displayName, tier)
                    .flatMap(user -> Mono.fromCallable(() -> firestore.collection(collectionName).document(doc.getId())
                        .update(Map.of(
                                "status", "created",
                                "uid", user.getFirebaseUid(),
                                "createdAt", LocalDateTime.now().toString()
                        )).get()).subscribeOn(Schedulers.boundedElastic()))
                    .map(update -> new BulkResult(BulkStatus.CREATED, email, null))
                    .onErrorResume(e -> {
                        log.error("Failed to create account for {}: {}", email, e.getMessage());
                        return Mono.fromCallable(() -> firestore.collection(collectionName).document(doc.getId())
                            .update(Map.of("status", "error", "errorMessage", e.getMessage())).get())
                            .subscribeOn(Schedulers.boundedElastic())
                            .thenReturn(new BulkResult(BulkStatus.ERROR, email, e.getMessage()));
                    });
            }, 5) // Process 5 at a time
            .collectList()
            .map(results -> {
                Map<String, Object> summary = new LinkedHashMap<>();
                long created = results.stream().filter(r -> r.status == BulkStatus.CREATED).count();
                long skipped = results.stream().filter(r -> r.status == BulkStatus.SKIPPED).count();
                long errors = results.stream().filter(r -> r.status == BulkStatus.ERROR).count();

                summary.put("totalProcessed", results.size());
                summary.put("created", created);
                summary.put("skipped", skipped);
                summary.put("errors", errors);
                summary.put("createdEmails", results.stream().filter(r -> r.status == BulkStatus.CREATED).map(r -> r.identifier).toList());
                summary.put("errorDetails", results.stream().filter(r -> r.status == BulkStatus.ERROR).map(r -> r.identifier + ": " + r.message).toList());
                return summary;
            });
    }

    private enum BulkStatus { CREATED, SKIPPED, ERROR }
    private record BulkResult(BulkStatus status, String identifier, String message) {}

    /**
     * Create the Firestore user document.
     */
    private Mono<User> createUserDocument(String uid, String email, String displayName, UserTier tier) {
        User user = new User(uid, email, displayName != null ? displayName : email.split("@")[0]);
        user.setTier(tier != null ? tier : UserTier.FREE);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now().toString());
        user.setUpdatedAt(LocalDateTime.now().toString());
        return userRepository.save(user);
    }

    /**
     * Update a user's tier.
     */
    @CachePut(value = "user_sessions", key = "#userId")
    public Mono<User> updateUserTier(String userId, UserTier newTier) {
        return userRepository.findByFirebaseUid(userId)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("User not found: " + userId)))
            .flatMap(user -> {
                user.setTier(newTier);
                user.setUpdatedAt(LocalDateTime.now().toString());
                return userRepository.save(user);
            })
            .flatMap(user -> Mono.fromCallable(() -> {
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
            }).subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * List all registered users.
     */
    public Flux<User> listAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Get a user by Firebase UID.
     */
    @Cacheable(value = "user_sessions", key = "#uid")
    public Mono<User> getUser(String uid) {
        return userRepository.findByFirebaseUid(uid);
    }

    /**
     * Deactivate a user account (sets isActive = false).
     */
    @CachePut(value = "user_sessions", key = "#userId")
    public Mono<User> deactivateUser(String userId) {
        return userRepository.findByFirebaseUid(userId)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("User not found: " + userId)))
            .flatMap(user -> {
                user.setIsActive(false);
                user.setUpdatedAt(LocalDateTime.now().toString());
                return userRepository.save(user);
            })
            .flatMap(user -> Mono.fromCallable(() -> {
                try {
                    UserRecord.UpdateRequest request = new UserRecord.UpdateRequest(userId).setDisabled(true);
                    FirebaseAuth.getInstance().updateUser(request);
                } catch (FirebaseAuthException e) {
                    log.error("Failed to disable Firebase Auth user {}: {}", userId, e.getMessage());
                }
                return user;
            }).subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * Reactivate a deactivated user account (sets isActive = true).
     */
    @CachePut(value = "user_sessions", key = "#userId")
    public Mono<User> reactivateUser(String userId) {
        return userRepository.findByFirebaseUid(userId)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("User not found: " + userId)))
            .flatMap(user -> {
                user.setIsActive(true);
                user.setUpdatedAt(LocalDateTime.now().toString());
                return userRepository.save(user);
            })
            .flatMap(user -> Mono.fromCallable(() -> {
                try {
                    UserRecord.UpdateRequest request = new UserRecord.UpdateRequest(userId).setDisabled(false);
                    FirebaseAuth.getInstance().updateUser(request);
                } catch (FirebaseAuthException e) {
                    log.error("Failed to re-enable Firebase Auth user {}: {}", userId, e.getMessage());
                }
                return user;
            }).subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * Permanently delete a user from both Firebase Auth and Firestore.
     */
    @CachePut(value = "user_sessions", key = "#userId")
    public Mono<Void> deleteUser(String userId) {
        return userRepository.findByFirebaseUid(userId)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("User not found: " + userId)))
            .flatMap(user -> Mono.fromCallable(() -> {
                try {
                    FirebaseAuth.getInstance().deleteUser(userId);
                    log.info("Deleted Firebase Auth account for: {}", userId);
                } catch (FirebaseAuthException e) {
                    log.error("Failed to delete Firebase Auth user {}: {}", userId, e.getMessage());
                }
                return user;
            }).subscribeOn(Schedulers.boundedElastic()))
            .flatMap(user -> userRepository.deleteByFirebaseUid(userId));
    }
}
