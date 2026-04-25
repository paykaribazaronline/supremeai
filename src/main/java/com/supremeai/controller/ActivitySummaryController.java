package com.supremeai.controller;

import com.supremeai.model.ActivityLog;
import com.supremeai.repository.ActivityLogRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    public Mono<ResponseEntity<Map<String, Object>>> getActivitySummary(
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String severity
    ) {
        Flux<ActivityLog> recentActionsFlux;
        
        if (severity != null && !severity.isEmpty()) {
            recentActionsFlux = activityLogRepository.findBySeverityOrderByTimestampDesc(severity.toUpperCase());
        } else if (category != null && !category.isEmpty()) {
            recentActionsFlux = activityLogRepository.findByCategoryOrderByTimestampDesc(category.toUpperCase());
        } else {
            recentActionsFlux = activityLogRepository.findAll().take(100);
        }
        
        return Mono.zip(
            recentActionsFlux.collectList(),
            activityLogRepository.count()
        ).map(tuple -> {
            Map<String, Object> summary = new HashMap<>();
            summary.put("recentActions", tuple.getT1());
            summary.put("totalActions", tuple.getT2());
            return ResponseEntity.ok(summary);
        });
    }

    @PostMapping("/log")
    public Mono<ResponseEntity<ActivityLog>> logActivity(@RequestBody ActivityLog log) {
        return activityLogRepository.save(log)
                .map(ResponseEntity::ok);
    }
}