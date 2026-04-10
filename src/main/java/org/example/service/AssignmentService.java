package org.example.service;

import org.example.model.TaskAssignment;
import com.google.firebase.database.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Assignment Service - Handles persistent storage of task assignments in Firebase
 * NEVER uses in-memory storage - ALL assignments are persisted
 */
@Service
public class AssignmentService {

    @Autowired
    private FirebaseService firebaseService;

    private static final String ASSIGNMENTS_PATH = "assignments";

    /**
     * Get all task assignments from Firebase
     */
    public List<TaskAssignment> getAllAssignments() {
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
            return new ArrayList<>();
        }
    }

    /**
     * Get assignment by ID
     */
    public TaskAssignment getAssignmentById(String id) {
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
     * Create a new task assignment - PERSISTED IN FIREBASE
     */
    public String createAssignment(TaskAssignment assignment) {
        try {
            if (assignment.getId() == null) {
                assignment.setId(UUID.randomUUID().toString());
            }
            
            // Persist to Firebase
            firebaseService.getDatabase()
                .getReference(ASSIGNMENTS_PATH)
                .child(assignment.getId())
                .setValueAsync(assignment);

            System.out.println("✅ Assignment created and persisted: " + assignment.getId());
            return assignment.getId();
        } catch (Exception e) {
            System.err.println("❌ Error creating assignment: " + e.getMessage());
            throw new RuntimeException("Failed to create assignment", e);
        }
    }

    /**
     * Update assignment status
     */
    public void updateAssignmentStatus(String id, String status) {
        try {
            firebaseService.getDatabase()
                .getReference(ASSIGNMENTS_PATH)
                .child(id)
                .child("status")
                .setValueAsync(status);

            System.out.println("✅ Assignment status updated: " + id + " -> " + status);
        } catch (Exception e) {
            System.err.println("❌ Error updating assignment status: " + e.getMessage());
        }
    }

    /**
     * Update assignment progress
     */
    public void updateAssignmentProgress(String id, double progress) {
        try {
            firebaseService.getDatabase()
                .getReference(ASSIGNMENTS_PATH)
                .child(id)
                .child("progress")
                .setValueAsync(progress);

            System.out.println("✅ Assignment progress updated: " + id + " -> " + progress + "%");
        } catch (Exception e) {
            System.err.println("❌ Error updating assignment progress: " + e.getMessage());
        }
    }

    /**
     * Delete assignment
     */
    public void deleteAssignment(String id) {
        try {
            firebaseService.getDatabase()
                .getReference(ASSIGNMENTS_PATH)
                .child(id)
                .removeValueAsync();

            System.out.println("✅ Assignment deleted: " + id);
        } catch (Exception e) {
            System.err.println("❌ Error deleting assignment: " + e.getMessage());
        }
    }

    /**
     * Get assignments by agent ID
     */
    public List<TaskAssignment> getAssignmentsByAgent(String agentId) {
        try {
            CompletableFuture<List<TaskAssignment>> future = new CompletableFuture<>();
            firebaseService.getDatabase()
                .getReference(ASSIGNMENTS_PATH)
                .orderByChild("agentId")
                .equalTo(agentId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        List<TaskAssignment> assignments = new ArrayList<>();
                        if (snapshot.exists()) {
                            for (DataSnapshot data : snapshot.getChildren()) {
                                TaskAssignment assignment = data.getValue(TaskAssignment.class);
                                if (assignment != null) {
                                    assignments.add(assignment);
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
            System.err.println("❌ Error fetching assignments by agent: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get assignments by status
     */
    public List<TaskAssignment> getAssignmentsByStatus(String status) {
        try {
            CompletableFuture<List<TaskAssignment>> future = new CompletableFuture<>();
            firebaseService.getDatabase()
                .getReference(ASSIGNMENTS_PATH)
                .orderByChild("status")
                .equalTo(status)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        List<TaskAssignment> assignments = new ArrayList<>();
                        if (snapshot.exists()) {
                            for (DataSnapshot data : snapshot.getChildren()) {
                                TaskAssignment assignment = data.getValue(TaskAssignment.class);
                                if (assignment != null) {
                                    assignments.add(assignment);
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
            System.err.println("❌ Error fetching assignments by status: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}
