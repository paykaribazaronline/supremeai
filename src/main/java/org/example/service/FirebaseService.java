package org.example.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.cloud.FirestoreClient;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class FirebaseService {
    private Firestore db;
    private FirebaseAuth auth;
    
    public FirebaseService(String credentialsPath) {
        try {
            initializeFirebase(credentialsPath);
        } catch (IOException e) {
            System.err.println("Failed to initialize Firebase: " + e.getMessage());
        }
    }
    
    private void initializeFirebase(String credentialsPath) throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            GoogleCredentials credentials;
            if (credentialsPath != null && !credentialsPath.isEmpty()) {
                credentials = GoogleCredentials.fromStream(
                    getClass().getResourceAsStream(credentialsPath)
                );
            } else {
                credentials = GoogleCredentials.getApplicationDefault();
            }
            
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .setDatabaseUrl("https://supremeai.firebaseio.com")
                    .build();
            FirebaseApp.initializeApp(options);
        }
        
        this.db = FirestoreClient.getFirestore();
        this.auth = FirebaseAuth.getInstance();
    }
    
    // ============ CHAT & MESSAGING ============
    
    public void saveChatMessage(String projectId, String sender, String message, String type) {
        try {
            Map<String, Object> chatData = new HashMap<>();
            chatData.put("projectId", projectId);
            chatData.put("sender", sender);
            chatData.put("message", message);
            chatData.put("type", type); // ai, admin, system, human_required
            chatData.put("timestamp", System.currentTimeMillis());
            chatData.put("read", false);
            
            if (type.equals("human_required")) {
                chatData.put("priority", "HIGH");
                chatData.put("alert", "🔔");
            }
            
            db.collection("projects").document(projectId)
                    .collection("chat")
                    .add(chatData);
        } catch (Exception e) {
            System.err.println("Failed to save chat message: " + e.getMessage());
        }
    }
    
    // ============ PROJECT MANAGEMENT ============
    
    public String createProject(String name, String description, String requirementSummary) {
        try {
            Map<String, Object> projectData = new HashMap<>();
            projectData.put("name", name);
            projectData.put("description", description);
            projectData.put("requirementSummary", requirementSummary);
            projectData.put("status", "planning");
            projectData.put("createdAt", System.currentTimeMillis());
            projectData.put("updatedAt", System.currentTimeMillis());
            projectData.put("progress", 0);
            
            DocumentReference docRef = db.collection("projects").add(projectData).get();
            return docRef.getId();
        } catch (ExecutionException | InterruptedException e) {
            System.err.println("Failed to create project: " + e.getMessage());
            return null;
        }
    }

    public void updateProjectProgress(String projectId, int percentage) {
        try {
            db.collection("projects")
                    .document(projectId)
                    .update("progress", percentage);
        } catch (Exception e) {
            System.err.println("Failed to update project progress: " + e.getMessage());
        }
    }

    public void updateProjectStatus(String projectId, String status) {
        try {
            db.collection("projects")
                    .document(projectId)
                    .update("status", status, "updatedAt", System.currentTimeMillis());
        } catch (Exception e) {
            System.err.println("Failed to update project status: " + e.getMessage());
        }
    }

    public Map<String, Object> getProject(String projectId) {
        try {
            DocumentSnapshot doc = db.collection("projects")
                    .document(projectId)
                    .get()
                    .get();
            return doc.getData();
        } catch (ExecutionException | InterruptedException e) {
            System.err.println("Failed to get project: " + e.getMessage());
            return null;
        }
    }
    
    // ============ REQUIREMENTS & APPROVALS ============
    
    public void sendNotification(String userId, String title, String message, String type) {
        try {
            Map<String, Object> notifData = new HashMap<>();
            notifData.put("userId", userId);
            notifData.put("title", title);
            notifData.put("message", message);
            notifData.put("type", type);
            notifData.put("timestamp", System.currentTimeMillis());
            notifData.put("read", false);
            
            if (type.equals("human_action")) {
                notifData.put("icon", "🚨");
            }
            
            db.collection("notifications").add(notifData);
        } catch (Exception e) {
            System.err.println("Failed to send notification: " + e.getMessage());
        }
    }

    public void saveSystemConfig(String configName, Map<String, Object> config) {
        try {
            db.collection("config").document(configName).set(config);
        } catch (Exception e) {
            System.err.println("Failed to save config: " + e.getMessage());
        }
    }
    
    public Map<String, Object> getSystemConfig(String configName) {
        try {
            DocumentSnapshot doc = db.collection("config")
                    .document(configName)
                    .get()
                    .get();
            return doc.getData();
        } catch (ExecutionException | InterruptedException e) {
            System.err.println("Failed to get config: " + e.getMessage());
            return new HashMap<>();
        }
    }
}
