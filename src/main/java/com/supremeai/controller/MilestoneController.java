package com.supremeai.controller;

import com.supremeai.model.Milestone;
import com.supremeai.repository.MilestoneRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/admin/milestones")
public class MilestoneController {

    @Autowired
    private MilestoneRepository milestoneRepository;

    @GetMapping
    public Flux<Milestone> getAllMilestones() {
        return milestoneRepository.findAllByOrderByOrderAsc();
    }

    @PostMapping
    public Mono<Milestone> saveMilestone(@RequestBody Milestone milestone) {
        return milestoneRepository.save(milestone);
    }

    @PutMapping("/{id}")
    public Mono<Milestone> updateMilestone(@PathVariable String id, @RequestBody Milestone milestone) {
        milestone.setId(id);
        return milestoneRepository.save(milestone);
    }

    @DeleteMapping("/{id}")
    public Mono<Void> deleteMilestone(@PathVariable String id) {
        return milestoneRepository.deleteById(id);
    }
}
