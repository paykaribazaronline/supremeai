package org.example.service;

import org.example.model.TaskAssignment;
import com.google.firebase.database.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Assignment Service - Handles persistent storage of task assignments in Firebase
 * FALLBACK to in-memory storage if Firebase is unavailable
 */
@Service
public class AssignmentService {

    @Autowired
    private FirebaseService firebaseService;

    private static final String ASSIGNMENTS_PATH = "assignments";
    // Fallback in-memory storage when Firebase is unavailable
    private static final Map<String, TaskAssignment> assignmentsCache = Collections.synchronizedMap(new LinkedHashMap<>());

    /**
     * Check if Firebase is available
     */
    private boolean isFirebaseAvailable() {
        try {
            return firebaseService != null && firebaseService.isInitialized();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get all task assignments from Firebase or fallback cache
     */
    public List<TaskAssignment> getAllAssignments() {
        if (!isFirebaseAvailable()) {
            // Use in-memory cache
            return new ArrayList<>(assignmentsCache.values());
        }

        try {
            CompletableFuture<List<TaskAssignment>> future = new CompletableFuture<>();
            firebaseService.getDatabase()
                .getReference(ASSIGNMENTS_PATH)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        List<TaskAssignment> assignments = new ArrayList<>();
                        if (snapshot.exists()) {
                            for (DataSnapshot data : snapshot.getChildren()) {
                                TaskAssignment assignment = data.getValue(TaskAssignment.class);
                                if (assignment != null) {
                                    assignments.add(assignment);
                                    assignmentsCache.put(assignment.getId(), assignment);
                                }
                            }
                        }
                        future.complete(assignments);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        future.completeExceptionally(error.toException());
                    }
                });
            return future.get();
        } catch (Exception e) {
            System.err.println("❌ Error fetching assignments: " + e.getMessage());
            // Return from cache if Firebase fails
            return new ArrayList<>(assignmentsCache.values());
        }
    }

    /**
     * Get assignment by ID - from cache or Firebase
     */
    public TaskAssignment getAssignmentById(String id) {
        // Check cache first
        if (assignmentsCache.containsKey(id)) {
            return assignmentsCache.get(id);
        }

        if (!isFirebaseAvailable()) {
            return null;
        }

        try {
            CompletableFuture<TaskAssignment> future = new CompletableFuture<>();
            firebaseService.getDatabase()
                .getReference(ASSIGNMENTS_PATH)
                .child(id)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            TaskAssignment assignment = snapshot.getValue(TaskAssignment.class);
                            if (assignment != null) {
                                assignmentsCache.put(id, assignment);
                            }
                            future.complete(assignment);
                        } else {
                            future.complete(null);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        future.completeExceptionally(error.toException());
                    }
                });
            return future.get();
        } catch (Exception e) {
            System.err.println("❌ Error fetching assignment: " + e.getMessage());
            return null;
        }
    }

    /**
     * Create a new task assignment - Firebase or fallback cache
     */
    public String createAssignment(TaskAssignment assignment) {
        if (assignment.getId() == null) {
            assignment.setId(UUID.randomUUID().toString());
        }

        // Try Firebase first
        if (isFirebaseAvailable()) {
            try {
                firebaseService.getDatabase()
                    .getReference(ASSIGNMENTS_PATH)
                    .child(assignment.getId())
                    .setValueAsync(assignment);
                System.out.println("✅ Assignment created and persisted in Firebase: " + assignment.getId());
            } catch (Exception e) {
                System.err.println("⚠️ Firebase write failed: " + e.getMessage() + " - using cache fallback");
            }
        }

        // Always save to in-memory cache as secondary storage
        assignmentsCache.put(assignment.getId(), assignment);
        System.out.println("✅ Assignment cached: " + assignment.getId());
        return assignment.getId();
    }

    /**
     * Update assignment status
     */
    public void updateAssignmentStatus(String id, String status) {
        // Update in cache
        if (assignmentsCache.containsKey(id)) {
            assignmentsCache.get(id).setStatus(status);
        }

        // Try Firebase
        if (isFirebaseAvailable()) {
            try {
                firebaseService.getDatabase()
                    .getReference(ASSIGNMENTS_PATH)
                    .child(id)
                    .child("status")
                    .setValueAsync(status);
            } catch (Exception e) {
                System.err.println("⚠️ Firebase update failed: " + e.getMessage());
            }
        }

        System.out.println("✅ Assignment status updated: " + id + " -> " + status);
    }

    /**
     * Update assignment progress
     */
    public void updateAssignmentProgress(String id, double progress) {
        // Update in cache
        if (assignmentsCache.containsKey(id)) {
            assignmentsCache.get(id).setProgress(progress);
        }

        // Try Firebase
        if (isFirebaseAvailable()) {
            try {
                firebaseService.getDatabase()
                    .getReference(ASSIGNMENTS_PATH)
                    .child(id)
                    .child("progress")
                    .setValueAsync(progress);
            } catch (Exception e) {
                System.err.println("⚠️ Firebase update failed: " + e.getMessage());
            }
        }

        System.out.println("✅ Assignment progress updated: " + id + " -> " + progress + "%");
    }

    /**
     * Delete assignment
     */
    public void deleteAssignment(String id) {
        // Delete from cache
        assignmentsCache.remove(id);

        // Try Firebase
        if (isFirebaseAvailable()) {
            try {
                firebaseService.getDatabase()
                    .getReference(ASSIGNMENTS_PATH)
                    .child(id)
                    .removeValueAsync();
            } catch (Exception e) {
                System.err.println("⚠️ Firebase delete failed: " + e.getMessage());
            }
        }

        System.out.println("✅ Assignment deleted: " + id);
    }

    /**
     * Get assignments by agent ID - from cache or Firebase
     */
    public List<TaskAssignment> getAssignmentsByAgent(String agentId) {
        List<TaskAssignment> result = new ArrayList<>();
        
        // Filter from cache
        for (TaskAssignment assignment : assignmentsCache.values()) {
            if (agentId.equals(assignment.getAgentId())) {
                result.add(assignment);
            }
        }
        
        // If Firebase available, try to sync
        if (isFirebaseAvailable()) {
            try {
                CompletableFuture<List<TaskAssignment>> future = new CompletableFuture<>();
                firebaseService.getDatabase()
                    .getReference(ASSIGNMENTS_PATH)
                    .orderByChild("agentId")
                    .equalTo(agentId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                for (DataSnapshot data : snapshot.getChildren()) {
                                    TaskAssignment assignment = data.getValue(TaskAssignment.class);
                                    if (assignment != null) {
                                        assignmentsCache.put(assignment.getId(), assignment);
                                    }
                                }
                            }
                            future.complete(null);
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            future.completeExceptionally(error.toException());
                        }
                    });
                future.get();
            } catch (Exception e) {
                System.err.println("⚠️ Firebase sync failed: " + e.getMessage());
            }
        }
        
        return result;
    }

    /**
     * Get assignments by status - from cache or Firebase
     */
    public List<TaskAssignment> getAssignmentsByStatus(String status) {
        List<TaskAssignment> result = new ArrayList<>();
        
        // Filter from cache
        for (TaskAssignment assignment : assignmentsCache.values()) {
            if (status.equals(assignment.getStatus())) {
                result.add(assignment);
            }
        }
        
        // If Firebase available, try to sync
        if (isFirebaseAvailable()) {
            try {
                CompletableFuture<List<TaskAssignment>> future = new CompletableFuture<>();
                firebaseService.getDatabase()
                    .getReference(ASSIGNMENTS_PATH)
                    .orderByChild("status")
                    .equalTo(status)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                for (DataSnapshot data : snapshot.getChildren()) {
                                    TaskAssignment assignment = data.getValue(TaskAssignment.class);
                                    if (assignment != null) {
                                        assignmentsCache.put(assignment.getId(), assignment);
                                    }
                                }
                            }
                            future.complete(null);
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            future.completeExceptionally(error.toException());
                        }
                    });
                future.get();
            } catch (Exception e) {
                System.err.println("⚠️ Firebase sync failed: " + e.getMessage());
            }
        }
        
        return result;
    }
}
