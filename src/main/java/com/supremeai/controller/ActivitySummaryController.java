package com.supremeai.controller;

import com.supremeai.model.ActivityLog;
import com.supremeai.repository.ActivityLogRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/activity")
public class ActivitySummaryController {

    private final ActivityLogRepository activityLogRepository;

    public ActivitySummaryController(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getActivitySummary(
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String severity
    ) {
        List<ActivityLog> recentActions;
        
        if (severity != null && !severity.isEmpty()) {
            recentActions = activityLogRepository.findBySeverityOrderByTimestampDesc(severity.toUpperCase()).collectList().block();
        } else if (category != null && !category.isEmpty()) {
            recentActions = activityLogRepository.findByCategoryOrderByTimestampDesc(category.toUpperCase()).collectList().block();
        } else {
            recentActions = activityLogRepository.findAll().take(100).collectList().block();
        }
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("recentActions", recentActions);
        summary.put("totalActions", activityLogRepository.count().block());
        
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/log")
    public ResponseEntity<ActivityLog> logActivity(@RequestBody ActivityLog log) {
        return ResponseEntity.ok(activityLogRepository.save(log).block());
    }
}