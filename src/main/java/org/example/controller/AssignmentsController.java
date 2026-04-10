package org.example.controller;

import org.example.model.TaskAssignment;
import org.example.service.AssignmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Task Assignments Controller
 * ✅ Manages AI task assignments with PERSISTENT Firebase storage
 * 🔒 All assignments survive application restarts
 */
@RestController
@RequestMapping("/api/assignments")
@CrossOrigin(origins = "*")
public class AssignmentsController {

    @Autowired
    private AssignmentService assignmentService;

    /**
     * Get all assignments from Firebase (PERSISTED)
     */
    @GetMapping
    public ResponseEntity<?> getAssignments() {
        try {
            List<TaskAssignment> assignments = assignmentService.getAllAssignments();
            return ResponseEntity.ok(assignments);
        } catch (Exception e) {
            System.err.println("❌ Error fetching assignments: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get assignment by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getAssignment(@PathVariable String id) {
        try {
            TaskAssignment assignment = assignmentService.getAssignmentById(id);
            if (assignment != null) {
                return ResponseEntity.ok(assignment);
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "Assignment not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Create new assignment in Firebase (PERSISTED)
     */
    @PostMapping("/create")
    public ResponseEntity<?> createAssignment(@RequestBody TaskAssignment assignment) {
        try {
            if (assignment.getCreatedAt() == null) {
                assignment.setCreatedAt(LocalDateTime.now());
            }
            
            // 🔒 Persists to Firebase - survives app restarts
            String id = assignmentService.createAssignment(assignment);
            
            return ResponseEntity.ok(Map.of(
                "success", true, 
                "id", id,
                "message", "✅ Assignment persisted in Firebase"
            ));
        } catch (Exception e) {
            System.err.println("❌ Error creating assignment: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update assignment status
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
        @PathVariable String id,
        @RequestParam String status) {
        try {
            assignmentService.updateAssignmentStatus(id, status);
            return ResponseEntity.ok(Map.of("success", true, "message", "Status updated"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update assignment progress
     */
    @PutMapping("/{id}/progress")
    public ResponseEntity<?> updateProgress(
        @PathVariable String id,
        @RequestParam double progress) {
        try {
            assignmentService.updateAssignmentProgress(id, progress);
            return ResponseEntity.ok(Map.of("success", true, "message", "Progress updated"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete assignment
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAssignment(@PathVariable String id) {
        try {
            assignmentService.deleteAssignment(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Assignment deleted"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get assignments by agent
     */
    @GetMapping("/agent/{agentId}")
    public ResponseEntity<?> getByAgent(@PathVariable String agentId) {
        try {
            List<TaskAssignment> assignments = assignmentService.getAssignmentsByAgent(agentId);
            return ResponseEntity.ok(assignments);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get assignments by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<?> getByStatus(@PathVariable String status) {
        try {
            List<TaskAssignment> assignments = assignmentService.getAssignmentsByStatus(status);
            return ResponseEntity.ok(assignments);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
