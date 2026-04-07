package org.example.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import org.example.model.Requirement;
import org.example.model.User;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class FirebaseService {
    private FirebaseDatabase db;
    private FirebaseAuth auth;
    private boolean isInitialized = false;
    private static final String DATABASE_URL = "https://supremeai-a-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private static final String DEFAULT_PROJECT_ID = "supremeai-a";
    
    public FirebaseService() {
        // Default constructor for Spring
    }

    @PostConstruct
    public void init() {
        try {
            // Prevent hanging on metadata server when not on GCP
            System.setProperty("com.google.cloud.compute.metadata.timeout", "3000");
            initializeFirebase(null);
            this.isInitialized = true;
        } catch (Exception e) {
            System.err.println("⚠️ Firebase initialization failed: " + e.getMessage());
            System.err.println("🔄 Continuing without Firebase - using fallback mode");
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
                // No explicit credentials — skip default lookup (hangs on non-GCP machines)
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
     */
    public void updateAPIKey(String modelName, String apiKey) {
        db.getReference("config").child("api_keys").child(modelName).setValueAsync(apiKey);
        System.out.println("✅ API Key updated in Firebase for: " + modelName);
    }

    public void saveSystemConfig(String configId, Map<String, Object> config) {
        db.getReference("config").child(configId).updateChildrenAsync(config);
    }

    public String createProject(String name, String description, String summary) {
        DatabaseReference projectsRef = db.getReference("projects").push();
        String projectId = projectsRef.getKey();
        Map<String, Object> projectData = new HashMap<>();
        projectData.put("name", name);
        projectData.put("description", description);
        projectData.put("summary", summary);
        projectData.put("status", "planning");
        projectData.put("createdAt", System.currentTimeMillis());
        projectsRef.setValueAsync(projectData);
        return projectId;
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
        } catch (Exception e) { return new HashMap<>(); }
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

    public void updateRequirementStatus(String id, Requirement.Status status) {
        db.getReference("requirements").child(id).child("status").setValueAsync(status);
    }

    public void saveChatMessage(String projectId, String sender, String message, String type) {
        Map<String, Object> chatData = new HashMap<>();
        chatData.put("projectId", projectId);
        chatData.put("sender", sender);
        chatData.put("message", message);
        chatData.put("type", type);
        chatData.put("timestamp", System.currentTimeMillis());
        db.getReference("projects").child(projectId).child("chat").push().setValueAsync(chatData);
    }

    public void saveUser(User user) {
        if (user != null) {
            String userKey = resolveUserKey(user);
            if (userKey != null) {
                db.getReference("users").child(userKey).setValueAsync(user);
            }
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
        } catch (Exception e) { return null; }
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
        } catch (Exception e) { return null; }
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
                            User user = data.getValue(User.class);
                            if (user != null) users.add(user);
                        }
                    }
                    future.complete(users);
                }
                @Override public void onCancelled(DatabaseError error) { future.completeExceptionally(error.toException()); }
            });
            return future.get();
        } catch (Exception e) { return new ArrayList<>(); }
    }

    public void sendNotification(String recipient, String title, String message, String type) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("recipient", recipient);
        notification.put("title", title);
        notification.put("message", message);
        notification.put("type", type);
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("read", false);
        db.getReference("notifications").push().setValueAsync(notification);
    }

    /**
     * Update user in Firebase
     */
    public void updateUser(User user) {
        if (user != null) {
            String userKey = resolveUserKey(user);
            if (userKey != null) {
                db.getReference("users").child(userKey).setValueAsync(user);
            }
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
     * Get user by ID from Firebase
     */
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
            return null;
        }
    }

    // ==================== PHASE 8: SECURITY AUDIT PERSISTENCE ====================

    /**
     * Save security audit report to Firebase
     */
    public void saveSecurityAudit(Map<String, Object> report) {
        db.getReference("security").child("audits").push().setValueAsync(report);
    }

    // ==================== PHASE 9: COST INTELLIGENCE PERSISTENCE ====================

    /**
     * Save cost report to Firebase
     */
    public void saveCostReport(Map<String, Object> report) {
        db.getReference("intelligence").child("costs").push().setValueAsync(report);
    }

    /**
     * Save optimization recommendations to Firebase
     */
    public void saveOptimizationRecommendations(Map<String, Object> recommendations) {
        db.getReference("intelligence").child("optimizations").push().setValueAsync(recommendations);
    }

    /**
     * Save budget plan to Firebase
     */
    public void saveBudgetPlan(Map<String, Object> budgetPlan) {
        db.getReference("intelligence").child("budgets").child("active_plan").setValueAsync(budgetPlan);
    }

    /**
     * Check if a budget limit is exceeded
     */
    public boolean isBudgetExceeded(double currentSpend) {
        try {
            CompletableFuture<Double> future = new CompletableFuture<>();
            db.getReference("intelligence").child("budgets").child("active_plan").child("annual_total_limit")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            future.complete(snapshot.getValue(Double.class));
                        } else {
                            future.complete(100000.0); // Default high limit
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError error) { future.complete(100000.0); }
                });
            return currentSpend > future.get();
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== PHASE 10: SELF-IMPROVEMENT PERSISTENCE ====================

    /**
     * Save evolution generation report
     */
    public void saveEvolutionReport(Map<String, Object> report) {
        db.getReference("evolution").child("generations").push().setValueAsync(report);
    }

    /**
     * Save learned patterns (Knowledge Base)
     */
    public void saveLearnedPattern(Map<String, Object> pattern) {
        db.getReference("evolution").child("patterns").push().setValueAsync(pattern);
    }

    /**
     * Update active system configuration based on consensus
     */
    public void updateActiveSystemConfig(Map<String, Object> newConfig) {
        db.getReference("config").child("main_config").updateChildrenAsync(newConfig);
        db.getReference("evolution").child("logs").push().setValueAsync(Collections.singletonMap("event", "SYSTEM_CONFIG_UPDATED_BY_CONSENSUS"));
    }

    /**
     * Persist a dead-letter queue entry (AI slow-queue request that failed after all retries).
     */
    public void saveDeadLetterItem(Map<String, Object> item) {
        if (!isInitialized) {
            return;
        }
        db.getReference("ai_dead_letter_queue").push().setValueAsync(item);
    }
}
