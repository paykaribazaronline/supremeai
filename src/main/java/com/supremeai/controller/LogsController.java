package com.supremeai.controller;

import com.supremeai.model.ActivityLog;
import com.supremeai.repository.ActivityLogRepository;
import com.supremeai.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/logs")
@org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
public class LogsController extends BaseAdminController<ActivityLog, String> {

    private final ActivityLogRepository activityLogRepository;

    public LogsController(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> getLogs(
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "50") int limit
    ) {
        if (severity != null && !severity.isEmpty()) {
            return wrapList(activityLogRepository.findBySeverityOrderByTimestampDesc(severity.toUpperCase()).take(limit), "logs");
        } else if (category != null && !category.isEmpty()) {
            return wrapList(activityLogRepository.findByCategoryOrderByTimestampDesc(category.toUpperCase()).take(limit), "logs");
        } else {
            return wrapList(activityLogRepository.findAll().take(limit), "logs");
        }
    }

    @DeleteMapping("/clear")
    public Mono<ResponseEntity<ApiResponse<String>>> clearLogs() {
        return activityLogRepository.deleteAll()
                .then(Mono.just(ResponseEntity.ok(ApiResponse.ok("Logs cleared successfully"))))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body(ApiResponse.error("Failed to clear logs: " + e.getMessage()))));
    }
}
