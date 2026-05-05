package com.supremeai.controller;

import com.supremeai.model.ModelEvolution;
import com.supremeai.repository.ModelEvolutionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/admin/evolution")
public class EvolutionController {

    @Autowired
    private ModelEvolutionRepository evolutionRepository;

    @GetMapping
    public Flux<ModelEvolution> getAllEvolutionStates() {
        return evolutionRepository.findAll();
    }

    @PostMapping
    public Mono<ModelEvolution> saveEvolution(@RequestBody ModelEvolution evolution) {
        return evolutionRepository.save(evolution);
    }

    @PutMapping("/{id}")
    public Mono<ModelEvolution> updateEvolution(@PathVariable String id, @RequestBody ModelEvolution evolution) {
        evolution.setId(id);
        return evolutionRepository.save(evolution);
    }
}
