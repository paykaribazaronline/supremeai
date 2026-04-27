package com.supremeai.controller;

import com.supremeai.model.ActivityLog;
import com.supremeai.repository.ActivityLogRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/admin/control")
public class AdminControlController {

    private final ActivityLogRepository activityLogRepository;

    public AdminControlController(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    @GetMapping("/history")
    public Mono<ResponseEntity<List<ActivityLog>>> getControlHistory() {
        return activityLogRepository.findAll()
            .take(100)
            .collectList()
            .map(ResponseEntity::ok);
    }
}
