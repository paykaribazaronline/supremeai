package com.supremeai.controller;

import com.supremeai.service.SelfImprovementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/improvements")
@CrossOrigin(origins = "*")
public class ImprovementsController {

    private final SelfImprovementService selfImprovementService;

    public ImprovementsController(SelfImprovementService selfImprovementService) {
        this.selfImprovementService = selfImprovementService;
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        SelfImprovementService.SystemStats stats = selfImprovementService.getStats();
        return ResponseEntity.ok(Map.of(
                "totalLearningEntries", stats.totalLearningEntries(),
                "lastImprovement", stats.lastImprovement().toString(),
                "secondsSinceLastImprovement", stats.secondsSinceLastImprovement()
        ));
    }

    @PostMapping("/trigger")
    public ResponseEntity<Map<String, String>> triggerImprovement() {
        selfImprovementService.hourlyImprovementLoop();
        return ResponseEntity.ok(Map.of("status", "triggered", "message", "Hourly improvement loop started"));
    }
}
