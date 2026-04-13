package org.example.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import org.example.model.Requirement;
import org.example.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

// Not annotated with @Service — FirebaseService (the primary bean) is used by all injection points.
// FirebaseServiceFixed is kept as a reference/legacy class only.
public class FirebaseServiceFixed {
    private static final Logger logger = LoggerFactory.getLogger(FirebaseServiceFixed.class);
    
    private FirebaseDatabase db;
    private boolean isInitialized = false;
    private static final String DATABASE_URL = "https://supremeai-a-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private static final String DEFAULT_PROJECT_ID = "supremeai-a";
    
    public FirebaseServiceFixed() {
        // Default constructor for Spring
    }

    @PostConstruct
    public void init() {
        try {
            System.setProperty("com.google.cloud.compute.metadata.timeout", "3000");
            initializeFirebase(null);
            this.isInitialized = true;
            logger.info("✅ Firebase initialized successfully");
        } catch (Exception e) {
            logger.error("⚠️ Firebase initialization failed: {}", e.getMessage());
            logger.error("☁️ Cloud Firebase unavailable - local cache mode active until credentials restored");
            this.isInitialized = false;
        }
    }
    
    public boolean isInitialized() {
        return isInitialized;
    }
    
    public FirebaseDatabase getDatabase() {
        if (!isInitialized) {
            throw new IllegalStateException("Firebase not initialized - credentials missing or invalid");
        }
        return db;
    }

    private synchronized void initializeFirebase(String credentialsPath) throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            InputStream serviceAccount = null;
            String envConfig = System.getenv("FIREBASE_SERVICE_ACCOUNT_JSON");
            String projectId = System.getenv("FIREBASE_PROJECT_ID");
            if (projectId == null || projectId.isBlank()) {
                projectId = System.getenv("GOOGLE_CLOUD_PROJECT");
            }
            if (projectId == null || projectId.isBlank()) {
                projectId = DEFAULT_PROJECT_ID;
            }
            
            if (envConfig != null && !envConfig.isEmpty()) {
                serviceAccount = new ByteArrayInputStream(envConfig.getBytes(StandardCharsets.UTF_8));
            } else if (credentialsPath != null) {
                serviceAccount = getClass().getResourceAsStream(credentialsPath);
            }

            FirebaseOptions.Builder builder = FirebaseOptions.builder()
                    .setDatabaseUrl(DATABASE_URL)
                    .setProjectId(projectId);

            if (serviceAccount != null) {
                builder.setCredentials(GoogleCredentials.fromStream(serviceAccount));
            } else {
                String googleAppCreds = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
                if (googleAppCreds != null && !googleAppCreds.isBlank()) {
                    builder.setCredentials(GoogleCredentials.getApplicationDefault());
                } else {
                    throw new IOException("No Firebase credentials found — set FIREBASE_SERVICE_ACCOUNT_JSON or GOOGLE_APPLICATION_CREDENTIALS");
                }
            }
            
            FirebaseApp.initializeApp(builder.build());
        }
        this.db = FirebaseDatabase.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    /**
     * 🚀 Multi-API Key Support
     * ✅ NOW WITH ERROR CALLBACKS
     */
    public void updateAPIKey(String modelName, String apiKey, BiConsumer<Boolean, String> callback) {
        if (!isInitialized) {
            callback.accept(false, "Firebase not initialized");
            return;
        }
        
        db.getReference("config").child("api_keys").child(modelName)
            .setValue(apiKey, (error, ref) -> {
                if (error != null) {
                    logger.error("❌ Failed to update API key for {}: {}", modelName, error.getMessage());
                    callback.accept(false, "API key update failed: " + error.getMessage());
                } else {
                    logger.info("✅ API Key updated in Firebase for: {}", modelName);
                    callback.accept(true, "API key updated for: " + modelName);
                }
            });
    }

    /**
     * Save system configuration with error handling
     * ✅ NOW WITH COMPLETION CALLBACK
     */
    public void saveSystemConfig(String configId, Map<String, Object> config, 
                                 BiConsumer<Boolean, String> callback) {
        if (!isInitialized) {
            callback.accept(false, "Firebase not initialized");
            return;
        }
        
        db.getReference("config").child(configId)
            .updateChildren(config, (error, ref) -> {
                if (error != null) {
                    logger.error("❌ Failed to save config {}: {}", configId, error.getMessage());
                    callback.accept(false, "Config save failed: " + error.getMessage());
                } else {
                    logger.info("✅ System config {} saved successfully", configId);
                    callback.accept(true, "Config saved: " + configId);
                }
            });
    }

    /**
     * Backward compatibility: Fire-and-forget version (deprecated)
     */
    public void saveSystemConfigAsync(String configId, Map<String, Object> config) {
        if (isInitialized) {
            saveSystemConfig(configId, config, (success, message) -> {
                if (!success) logger.warn("Async config save failed: {}", message);
            });
        }
    }

    /**
     * Create project with error callback
     */
    public void createProject(String name, String description, String summary,
                             BiConsumer<String, String> callback) {
        if (!isInitialized) {
            callback.accept(null, "Firebase not initialized");
            return;
        }
        
        DatabaseReference projectsRef = db.getReference("projects").push();
        String projectId = projectsRef.getKey();
        Map<String, Object> projectData = new HashMap<>();
        projectData.put("name", name);
        projectData.put("description", description);
        projectData.put("summary", summary);
        projectData.put("status", "planning");
        projectData.put("createdAt", System.currentTimeMillis());
        
        projectsRef.setValue(projectData, (error, ref) -> {
            if (error != null) {
                logger.error("❌ Failed to create project: {}", error.getMessage());
                callback.accept(null, "Project creation failed: " + error.getMessage());
            } else {
                logger.info("✅ Project created: {}", projectId);
                callback.accept(projectId, null);
            }
        });
    }

    public Map<String, Object> getSystemConfig(String configId) {
        try {
            CompletableFuture<DataSnapshot> future = new CompletableFuture<>();
            db.getReference("config").child(configId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override public void onDataChange(DataSnapshot snapshot) { future.complete(snapshot); }
                @Override public void onCancelled(DatabaseError error) { future.completeExceptionally(error.toException()); }
            });
            DataSnapshot snapshot = future.get();
            return snapshot.exists() ? (Map<String, Object>) snapshot.getValue() : new HashMap<>();
        } catch (Exception e) { 
            logger.error("Error getting system config: {}", e.getMessage());
            return new HashMap<>(); 
        }
    }

    public List<Requirement> getAllRequirements() throws Exception {
        CompletableFuture<List<Requirement>> future = new CompletableFuture<>();
        db.getReference("requirements").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<Requirement> reqs = new ArrayList<>();
                if (snapshot.exists()) {
                    for (DataSnapshot data : snapshot.getChildren()) {
                        Requirement r = data.getValue(Requirement.class);
                        if (r != null) reqs.add(r);
                    }
                }
                future.complete(reqs);
            }
            @Override public void onCancelled(DatabaseError error) { future.completeExceptionally(error.toException()); }
        });
        return future.get();
    }

    /**
     * Update requirement status with error callback
     * ✅ NOW WITH ERROR HANDLING
     */
    public void updateRequirementStatus(String id, Requirement.Status status,
                                       BiConsumer<Boolean, String> callback) {
        if (!isInitialized) {
            callback.accept(false, "Firebase not initialized");
            return;
        }
        
        db.getReference("requirements").child(id).child("status")
            .setValue(status, (error, ref) -> {
                if (error != null) {
                    logger.error("❌ Failed to update requirement status {}: {}", id, error.getMessage());
                    callback.accept(false, "Status update failed: " + error.getMessage());
                } else {
                    logger.info("✅ Requirement status updated: {}", id);
                    callback.accept(true, "Status updated: " + id);
                }
            });
    }

    /**
     * Save chat message with error callback
     * ✅ NOW WITH ERROR HANDLING - CRITICAL FOR MESSAGE HISTORY
     */
    public void saveChatMessage(String projectId, String sender, String message, String type,
                               BiConsumer<String, String> callback) {
        if (!isInitialized) {
            callback.accept(null, "Firebase not initialized");
            return;
        }
        
        Map<String, Object> chatData = new HashMap<>();
        chatData.put("projectId", projectId);
        chatData.put("sender", sender);
        chatData.put("message", message);
        chatData.put("type", type);
        chatData.put("timestamp", System.currentTimeMillis());
        
        DatabaseReference chatRef = db.getReference("projects").child(projectId).child("chat").push();
        String chatId = chatRef.getKey();
        
        chatRef.setValue(chatData, (error, ref) -> {
            if (error != null) {
                logger.error("❌ Failed to save chat message for project {}: {}", projectId, error.getMessage());
                callback.accept(null, "Chat save failed: " + error.getMessage());
            } else {
                logger.info("✅ Chat message saved: {} in project {}", chatId, projectId);
                callback.accept(chatId, null);
            }
        });
    }

    /**
     * Save user with error callback
     * ✅ NOW WITH ERROR HANDLING - CRITICAL FOR USER DATA
     */
    public void saveUser(User user, BiConsumer<Boolean, String> callback) {
        if (user == null) {
            callback.accept(false, "User is null");
            return;
        }
        
        if (!isInitialized) {
            callback.accept(false, "Firebase not initialized");
            return;
        }
        
        String userKey = resolveUserKey(user);
        if (userKey != null) {
            db.getReference("users").child(userKey).setValue(user, (error, ref) -> {
                if (error != null) {
                    logger.error("❌ Failed to save user {}: {}", userKey, error.getMessage());
                    callback.accept(false, "User save failed: " + error.getMessage());
                } else {
                    logger.info("✅ User saved: {}", userKey);
                    callback.accept(true, "User saved: " + userKey);
                }
            });
        } else {
            callback.accept(false, "Could not resolve user key");
        }
    }

    public User getUserByUsername(String username) {
        try {
            CompletableFuture<User> future = new CompletableFuture<>();
            db.getReference("users").orderByChild("username").equalTo(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot data : snapshot.getChildren()) {
                                User user = data.getValue(User.class);
                                future.complete(user);
                                return;
                            }
                        }
                        future.complete(null);
                    }
                    @Override
                    public void onCancelled(DatabaseError error) { future.completeExceptionally(error.toException()); }
                });
            return future.get();
        } catch (Exception e) { 
            logger.error("Error getting user by username: {}", e.getMessage());
            return null; 
        }
    }

    public User getUserByEmail(String email) {
        try {
            CompletableFuture<User> future = new CompletableFuture<>();
            db.getReference("users").orderByChild("email").equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot data : snapshot.getChildren()) {
                                User user = data.getValue(User.class);
                                future.complete(user);
                                return;
                            }
                        }
                        future.complete(null);
                    }
                    @Override
                    public void onCancelled(DatabaseError error) { future.completeExceptionally(error.toException()); }
                });
            return future.get();
        } catch (Exception e) { 
            logger.error("Error getting user by email: {}", e.getMessage());
            return null; 
        }
    }

    public List<User> getAllUsers() {
        try {
            CompletableFuture<List<User>> future = new CompletableFuture<>();
            db.getReference("users").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    List<User> users = new ArrayList<>();
                    if (snapshot.exists()) {
                        for (DataSnapshot data : snapshot.getChildren()) {
                            User u = data.getValue(User.class);
                            if (u != null) users.add(u);
                        }
                    }
                    future.complete(users);
                }
                @Override public void onCancelled(DatabaseError error) { future.completeExceptionally(error.toException()); }
            });
            return future.get();
        } catch (Exception e) { 
            logger.error("Error getting all users: {}", e.getMessage());
            return new ArrayList<>(); 
        }
    }

    /**
     * Send notification with error callback
     * ✅ NOW WITH ERROR HANDLING
     */
    public void sendNotification(String recipient, String title, String message, String type,
                                BiConsumer<Boolean, String> callback) {
        if (!isInitialized) {
            callback.accept(false, "Firebase not initialized");
            return;
        }
        
        Map<String, Object> notification = new HashMap<>();
        notification.put("recipient", recipient);
        notification.put("title", title);
        notification.put("message", message);
        notification.put("type", type);
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("read", false);
        
        db.getReference("notifications").push().setValue(notification, (error, ref) -> {
            if (error != null) {
                logger.error("❌ Failed to send notification to {}: {}", recipient, error.getMessage());
                callback.accept(false, "Notification send failed: " + error.getMessage());
            } else {
                logger.info("✅ Notification sent to: {}", recipient);
                callback.accept(true, "Notification sent");
            }
        });
    }

    /**
     * Update user with error callback
     * ✅ NOW WITH ERROR HANDLING
     */
    public void updateUser(User user, BiConsumer<Boolean, String> callback) {
        if (user == null) {
            callback.accept(false, "User is null");
            return;
        }
        
        if (!isInitialized) {
            callback.accept(false, "Firebase not initialized");
            return;
        }
        
        String userKey = resolveUserKey(user);
        if (userKey != null) {
            db.getReference("users").child(userKey).setValue(user, (error, ref) -> {
                if (error != null) {
                    logger.error("❌ Failed to update user {}: {}", userKey, error.getMessage());
                    callback.accept(false, "User update failed: " + error.getMessage());
                } else {
                    logger.info("✅ User updated: {}", userKey);
                    callback.accept(true, "User updated: " + userKey);
                }
            });
        } else {
            callback.accept(false, "Could not resolve user key");
        }
    }

    private String resolveUserKey(User user) {
        if (user == null) {
            return null;
        }

        if (user.getId() != null && !user.getId().isBlank()) {
            return user.getId();
        }

        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            user.setId(user.getUsername());
            return user.getUsername();
        }

        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            String derivedId = user.getEmail().replace("@", "_at_").replace(".", "_");
            user.setId(derivedId);
            return derivedId;
        }

        return null;
    }

    /**
     * Save security audit report with error callback
     */
    public void saveSecurityAuditReport(Map<String, Object> report, BiConsumer<Boolean, String> callback) {
        if (!isInitialized) {
            callback.accept(false, "Firebase not initialized");
            return;
        }
        
        db.getReference("security").child("audits").push().setValue(report, (error, ref) -> {
            if (error != null) {
                logger.error("❌ Failed to save security audit: {}", error.getMessage());
                callback.accept(false, "Audit save failed");
            } else {
                logger.info("✅ Security audit saved");
                callback.accept(true, "Audit saved");
            }
        });
    }

    /**
     * Save system configuration for consensus engine with error callback
     */
    public void updateMainConfig(Map<String, Object> newConfig, BiConsumer<Boolean, String> callback) {
        if (!isInitialized) {
            callback.accept(false, "Firebase not initialized");
            return;
        }
        
        db.getReference("config").child("main_config").updateChildren(newConfig, (error, ref) -> {
            if (error != null) {
                logger.error("❌ Failed to update main config: {}", error.getMessage());
                callback.accept(false, "Config update failed");
            } else {
                logger.info("✅ Main config updated");
                callback.accept(true, "Config updated");
            }
        });
    }

    public User getUserById(String userId) {
        try {
            CompletableFuture<User> future = new CompletableFuture<>();
            db.getReference("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        User user = snapshot.getValue(User.class);
                        future.complete(user);
                    } else {
                        future.complete(null);
                    }
                }
                @Override
                public void onCancelled(DatabaseError error) { future.completeExceptionally(error.toException()); }
            });
            return future.get();
        } catch (Exception e) { 
            logger.error("Error getting user by ID: {}", e.getMessage());
            return null; 
        }
    }
}
