package org.example.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Work History Controller
 * Tracks AI agent work history and decisions
 */
@RestController
@RequestMapping("/api/work-history")
@CrossOrigin(origins = "*")
public class WorkHistoryController {

    @GetMapping
    public ResponseEntity<?> getWorkHistory() {
        try {
            List<Map<String, Object>> history = new ArrayList<>();
            
            Map<String, Object> work1 = new HashMap<>();
            work1.put("id", UUID.randomUUID().toString());
            work1.put("agent", "Agent-1");
            work1.put("taskName", "Data Analysis");
            work1.put("status", "completed");
            work1.put("outcome", "success");
            work1.put("startTime", System.currentTimeMillis() - 3600000);
            work1.put("endTime", System.currentTimeMillis());
            work1.put("duration", 3600);
            history.add(work1);

            Map<String, Object> work2 = new HashMap<>();
            work2.put("id", UUID.randomUUID().toString());
            work2.put("agent", "Agent-2");
            work2.put("taskName", "Report Generation");
            work2.put("status", "completed");
            work2.put("outcome", "success");
            work2.put("startTime", System.currentTimeMillis() - 1800000);
            work2.put("endTime", System.currentTimeMillis());
            work2.put("duration", 1800);
            history.add(work2);

            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
