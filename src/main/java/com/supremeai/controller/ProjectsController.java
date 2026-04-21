package com.supremeai.controller;

import com.supremeai.model.ExistingProject;
import com.supremeai.repository.ProjectRepository;
import com.supremeai.util.IdUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
public class ProjectsController {

    private final ProjectRepository projectRepository;

    public ProjectsController(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @GetMapping
    public Flux<ExistingProject> getAllProjects() {
        return projectRepository.findAll();
    }

    @GetMapping("/owner/{ownerId}")
    public Flux<ExistingProject> getByOwner(@PathVariable String ownerId) {
        return projectRepository.findByOwnerId(ownerId);
    }

    @PostMapping
    public Mono<ExistingProject> createProject(@RequestBody ExistingProject project) {
        project.setId(IdUtils.ensureId(project.getId()));
        return projectRepository.save(project);
    }

    @PutMapping("/{id}/status")
    public Mono<ExistingProject> updateProjectStatus(@PathVariable String id, @RequestParam String status) {
        return projectRepository.findById(id)
                .flatMap(project -> {
                    project.setStatus(status);
                    return projectRepository.save(project);
                });
    }

    @DeleteMapping("/{id}")
    public Mono<Void> deleteProject(@PathVariable String id) {
        return projectRepository.deleteById(id);
    }
}
