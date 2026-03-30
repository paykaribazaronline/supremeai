package org.example.controller;

import org.example.model.Improvement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Improvements Controller
 * Manages AI-proposed improvements tracking
 */
@RestController
@RequestMapping("/api/improvements")
@CrossOrigin(origins = "*")
public class ImprovementsController {

    private static final List<Improvement> improvements = new ArrayList<>();

    static {
        // Initialize with sample improvements
        Improvement i1 = new Improvement("Optimize Cache Strategy", "Agent-1");
        i1.setStatus("completed");
        i1.setEstimatedImpact(25.5);
        i1.setCategory("performance");
        i1.setCompletedDate(LocalDateTime.now().minusDays(1));
        improvements.add(i1);

        Improvement i2 = new Improvement("Add Query Indexes", "Agent-2");
        i2.setStatus("in-progress");
        i2.setEstimatedImpact(15.3);
        i2.setCategory("database");
        improvements.add(i2);
    }

    @GetMapping("/list")
    public ResponseEntity<?> getImprovements() {
        try {
            return ResponseEntity.ok(improvements);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
