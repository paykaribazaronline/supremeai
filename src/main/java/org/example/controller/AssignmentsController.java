package org.example.controller;

import org.example.model.TaskAssignment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Task Assignments Controller
 * Manages AI task assignments
 */
@RestController
@RequestMapping("/api/assignments")
@CrossOrigin(origins = "*")
public class AssignmentsController {

    private static final List<TaskAssignment> assignments = new ArrayList<>();

    static {
        // Initialize with sample assignments
        TaskAssignment a1 = new TaskAssignment("agent-1", "Data Analysis");
        a1.setStatus("in-progress");
        a1.setProgress(75.0);
        a1.setPriority("high");
        assignments.add(a1);
    }

    @GetMapping
    public ResponseEntity<?> getAssignments() {
        try {
            return ResponseEntity.ok(assignments);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> createAssignment(@RequestBody TaskAssignment assignment) {
        try {
            if (assignment.getId() == null) {
                assignment.setId(UUID.randomUUID().toString());
            }
            if (assignment.getCreatedAt() == null) {
                assignment.setCreatedAt(LocalDateTime.now());
            }
            assignments.add(assignment);
            return ResponseEntity.ok(Map.of("success", true, "id", assignment.getId()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
