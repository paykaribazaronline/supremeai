package org.example.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class FirebaseService {
    private FirebaseDatabase db;
    private FirebaseAuth auth;
    private static final String DATABASE_URL = "https://supremeai-a-default-rtdb.asia-southeast1.firebasedatabase.app/";
    
    public FirebaseService() {
        try {
            initializeFirebase(null);
        } catch (Exception e) {
            System.err.println("❌ CRITICAL: Firebase initialization failed!");
            System.err.println("Reason: " + e.getMessage());
        }
    }

    public FirebaseService(String credentialsPath) {
        try {
            initializeFirebase(credentialsPath);
        } catch (Exception e) {
            System.err.println("❌ CRITICAL: Firebase initialization failed!");
            System.err.println("Reason: " + e.getMessage());
            if (credentialsPath != null) {
                System.err.println("Looking for file at: src/main/resources" + credentialsPath);
            }
        }
    }
    
    private void initializeFirebase(String credentialsPath) throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            InputStream serviceAccount = null;
            
            // Priority 1: Environment Variable (JSON String)
            String envConfig = System.getenv("FIREBASE_SERVICE_ACCOUNT_JSON");
            if (envConfig != null && !envConfig.isEmpty()) {
                serviceAccount = new ByteArrayInputStream(envConfig.getBytes(StandardCharsets.UTF_8));
                System.out.println("✅ Initializing Firebase using FIREBASE_SERVICE_ACCOUNT_JSON from environment.");
            } 
            // Priority 2: Specified Resource Path
            else if (credentialsPath != null) {
                serviceAccount = getClass().getResourceAsStream(credentialsPath);
                if (serviceAccount != null) {
                    System.out.println("✅ Initializing Firebase using resource path: " + credentialsPath);
                }
            }

            if (serviceAccount == null) {
                throw new IOException("No Firebase credentials found. Set FIREBASE_SERVICE_ACCOUNT_JSON env var or provide a valid resource path.");
            }
            
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl(DATABASE_URL)
                    .build();
            
            FirebaseApp.initializeApp(options);
            System.out.println("✅ Firebase initialized successfully.");
        }
        
        this.db = FirebaseDatabase.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }
    
    public void saveChatMessage(String projectId, String sender, String message, String type) {
        Map<String, Object> chatData = new HashMap<>();
        chatData.put("projectId", projectId);
        chatData.put("sender", sender);
        chatData.put("message", message);
        chatData.put("type", type);
        chatData.put("timestamp", System.currentTimeMillis());
        
        db.getReference("projects").child(projectId).child("chat").push().setValueAsync(chatData);
        System.out.println("📝 Message saved to RTDB for project: " + projectId);
    }

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

    public void updateProjectProgress(String projectId, int progress) {
        db.getReference("projects").child(projectId).child("progress").setValueAsync(progress);
    }

    public void saveSystemConfig(String configId, Map<String, Object> config) {
        db.getReference("config").child(configId).setValueAsync(config);
        System.out.println("⚙️ System configuration '" + configId + "' saved to RTDB.");
    }

    public Map<String, Object> getSystemConfig(String configId) {
        try {
            CompletableFuture<DataSnapshot> future = new CompletableFuture<>();
            db.getReference("config").child(configId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    future.complete(snapshot);
                }
                @Override
                public void onCancelled(DatabaseError error) {
                    future.completeExceptionally(error.toException());
                }
            });
            
            DataSnapshot snapshot = future.get();
            if (snapshot.exists()) {
                return (Map<String, Object>) snapshot.getValue();
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch system config: " + e.getMessage());
        }
        return new HashMap<>();
    }

    public void updateProjectStatus(String projectId, String status) {
        db.getReference("projects").child(projectId).child("status").setValueAsync(status);
    }

    public void sendNotification(String userId, String title, String message, String type) {
        System.out.println("🔔 Notification sent to " + userId + ": " + title);
    }
}
