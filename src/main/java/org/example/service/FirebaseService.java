package org.example.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import org.example.model.Requirement;
import org.example.model.User;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class FirebaseService {
    private FirebaseDatabase db;
    private FirebaseAuth auth;
    private static final String DATABASE_URL = "https://supremeai-a-default-rtdb.asia-southeast1.firebasedatabase.app/";
    
    public FirebaseService() {
        try { initializeFirebase(null); } catch (Exception e) { e.printStackTrace(); }
    }

    public FirebaseService(String credentialsPath) {
        try { initializeFirebase(credentialsPath); } catch (Exception e) { e.printStackTrace(); }
    }
    
    private void initializeFirebase(String credentialsPath) throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            InputStream serviceAccount = null;
            String envConfig = System.getenv("FIREBASE_SERVICE_ACCOUNT_JSON");
            if (envConfig != null && !envConfig.isEmpty()) {
                serviceAccount = new ByteArrayInputStream(envConfig.getBytes(StandardCharsets.UTF_8));
            } else if (credentialsPath != null) {
                serviceAccount = getClass().getResourceAsStream(credentialsPath);
            }

            if (serviceAccount == null) throw new IOException("No Firebase credentials found.");
            
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl(DATABASE_URL)
                    .build();
            
            FirebaseApp.initializeApp(options);
        }
        this.db = FirebaseDatabase.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    /**
     * 🚀 FIX: Multi-API Key Support
     * Updates specific keys without overwriting the entire node.
     */
    public void updateAPIKey(String modelName, String apiKey) {
        // Path: config/api_keys/GEMINI_1, config/api_keys/GEMINI_2, etc.
        db.getReference("config").child("api_keys").child(modelName).setValueAsync(apiKey);
        System.out.println("✅ API Key updated in Firebase for: " + modelName);
    }

    public void saveSystemConfig(String configId, Map<String, Object> config) {
        db.getReference("config").child(configId).updateChildrenAsync(config);
        System.out.println("⚙️ System configuration '" + configId + "' updated.");
    }

    // ... (rest of existing methods: saveUser, getAllRequirements, etc.)
    public String createProject(String name, String description, String summary) {
        DatabaseReference projectsRef = db.getReference("projects").push();
        String projectId = projectsRef.getKey();
        Map<String, Object> projectData = new HashMap<>();
        projectData.put("name", name);
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

    // ==================== User Management Methods ====================

    /**
     * Save user to Firebase
     */
    public void saveUser(User user) {
        if (user != null && user.getId() != null) {
            db.getReference("users").child(user.getId()).setValueAsync(user);
        }
    }

    /**
     * Update user in Firebase
     */
    public void updateUser(User user) {
        if (user != null && user.getId() != null) {
            db.getReference("users").child(user.getId()).setValueAsync(user);
        }
    }

    /**
     * Get user by username
     */
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
                    public void onCancelled(DatabaseError error) {
                        future.completeExceptionally(error.toException());
                    }
                });
            return future.get();
        } catch (Exception e) {
            System.err.println("Error getting user by username: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get user by ID
     */
    public User getUserById(String userId) {
        try {
            CompletableFuture<User> future = new CompletableFuture<>();
            db.getReference("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    future.complete(user);
                }
                @Override
                public void onCancelled(DatabaseError error) {
                    future.completeExceptionally(error.toException());
                }
            });
            return future.get();
        } catch (Exception e) {
            System.err.println("Error getting user by ID: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get all users
     */
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
                            if (user != null) {
                                users.add(user);
                            }
                        }
                    }
                    future.complete(users);
                }
                @Override
                public void onCancelled(DatabaseError error) {
                    future.completeExceptionally(error.toException());
                }
            });
            return future.get();
        } catch (Exception e) {
            System.err.println("Error getting all users: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Send notification
     */
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
}
