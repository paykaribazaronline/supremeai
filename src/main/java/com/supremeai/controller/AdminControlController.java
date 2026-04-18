package com.supremeai.controller;

import com.supremeai.model.ActivityLog;
import com.supremeai.repository.ActivityLogRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/control")
public class AdminControlController {

    private final ActivityLogRepository activityLogRepository;

    public AdminControlController(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    @GetMapping("/history")
    public ResponseEntity<List<ActivityLog>> getControlHistory() {
        // In a real scenario, this might filter for specific administrative actions
        List<ActivityLog> history = activityLogRepository.findTop100ByOrderByTimestampDesc();
        return ResponseEntity.ok(history);
    }
}
