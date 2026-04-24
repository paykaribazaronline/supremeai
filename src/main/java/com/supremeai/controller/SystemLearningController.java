package com.supremeai.controller;

import com.supremeai.model.SystemLearning;
import com.supremeai.service.SystemLearningService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * System learning endpoints - ADMIN only.
 * Stores AI system improvements and learnings.
 */
@RestController
@RequestMapping("/api/system-learning")
@PreAuthorize("hasRole('ADMIN')")
public class SystemLearningController {

    private final SystemLearningService service;

    public SystemLearningController(SystemLearningService service) {
        this.service = service;
    }

    @GetMapping
    public List<SystemLearning> getAllLearning() {
        return service.getAllLearningSync();
    }

    @GetMapping("/category/{category}")
    public List<SystemLearning> getByCategory(@PathVariable String category) {
        return service.getByCategorySync(category);
    }

    @PostMapping
    public Mono<SystemLearning> addLearning(@RequestBody SystemLearning learning) {
        return service.addLearning(learning);
    }

    @DeleteMapping("/{id}")
    public Mono<Void> deleteLearning(@PathVariable String id) {
        return service.deleteLearning(id);
    }

    /**
     * Get system 'wisdom' - highly confident autonomous learnings.
     */
    @GetMapping("/wisdom")
    public Mono<List<SystemLearning>> getSystemWisdom() {
        return service.getAllLearning()
            .filter(l -> l.getConfidenceScore() != null && l.getConfidenceScore() >= 0.85)
            .sort((a, b) -> b.getLearnedAt().compareTo(a.getLearnedAt()))
            .take(20)
            .collectList();
    }
}
