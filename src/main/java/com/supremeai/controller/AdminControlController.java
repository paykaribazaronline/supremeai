package com.supremeai.controller;

import com.supremeai.model.ActivityLog;
import com.supremeai.repository.ActivityLogRepository;
import com.supremeai.service.analysis.ProjectDNAHarvesterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/control")
public class AdminControlController {

    private final ActivityLogRepository activityLogRepository;
    private final ProjectDNAHarvesterService dnaHarvesterService;

    public AdminControlController(ActivityLogRepository activityLogRepository, ProjectDNAHarvesterService dnaHarvesterService) {
        this.activityLogRepository = activityLogRepository;
        this.dnaHarvesterService = dnaHarvesterService;
    }

    @PostMapping("/dna-harvest")
    public Mono<ResponseEntity<Map<String, String>>> triggerDNAHarvest() {
        return dnaHarvesterService.harvestDNA()
            .then(Mono.just(ResponseEntity.ok(Map.of("message", "Project DNA Harvesting triggered successfully"))));
    }

    @GetMapping("/history")
    public Mono<ResponseEntity<List<ActivityLog>>> getControlHistory() {
        return activityLogRepository.findAll()
            .take(100)
            .collectList()
            .map(ResponseEntity::ok);
    }
}
