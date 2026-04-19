package com.supremeai.controller;

import com.supremeai.model.SystemLearning;
import com.supremeai.repository.SystemLearningRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

@RestController
@RequestMapping("/api/system-learning")
public class SystemLearningController {

    @Autowired
    private SystemLearningRepository repository;

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
        if (learning.getId() == null) {
            learning.setId(UUID.randomUUID().toString());
        }
        return repository.save(learning);
    }

    @DeleteMapping("/{id}")
    public Mono<Void> deleteLearning(@PathVariable String id) {
        return repository.deleteById(id);
    }
}
