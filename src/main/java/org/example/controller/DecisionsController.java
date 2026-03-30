package org.example.controller;

import org.example.model.Decision;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Decisions Controller
 * Manages multi-agent voting and decisions
 */
@RestController
@RequestMapping("/api/decisions")
@CrossOrigin(origins = "*")
public class DecisionsController {

    private static final List<Decision> decisions = new ArrayList<>();

    static {
        // Initialize with sample decision
        Decision d1 = new Decision("Optimize Database Queries", "Agent-1");
        d1.setStatus("pending");
        d1.setApprovalRate(66.7);
        decisions.add(d1);
    }

    @GetMapping("/list")
    public ResponseEntity<?> listDecisions() {
        try {
            return ResponseEntity.ok(decisions);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> createDecision(@RequestBody Decision decision) {
        try {
            if (decision.getId() == null) {
                decision.setId(UUID.randomUUID().toString());
            }
            if (decision.getCreatedAt() == null) {
                decision.setCreatedAt(LocalDateTime.now());
            }
            decisions.add(decision);
            return ResponseEntity.ok(Map.of("success", true, "id", decision.getId()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
