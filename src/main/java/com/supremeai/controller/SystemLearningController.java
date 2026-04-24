package com.supremeai.controller;

import com.supremeai.model.SystemLearning;
import com.supremeai.repository.SystemLearningRepository;
import com.supremeai.util.IdUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * System learning endpoints - ADMIN only.
 * Stores AI system improvements and learnings.
 */
@RestController
@RequestMapping("/api/system-learning")
@PreAuthorize("hasRole('ADMIN')")
public class SystemLearningController {

    private final SystemLearningRepository repository;

    public SystemLearningController(SystemLearningRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public Flux<SystemLearning> getAllLearning() {
        return repository.findAll();
    }

    @GetMapping("/category/{category}")
    public Flux<SystemLearning> getByCategory(@PathVariable String category) {
        return repository.findByCategory(category);
    }

    @PostMapping
    public Mono<SystemLearning> addLearning(@RequestBody SystemLearning learning) {
        learning.setId(IdUtils.ensureId(learning.getId()));
        return repository.save(learning);
    }

    @DeleteMapping("/{id}")
    public Mono<Void> deleteLearning(@PathVariable String id) {
        return repository.deleteById(id);
    }
}
