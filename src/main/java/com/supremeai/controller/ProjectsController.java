package com.supremeai.controller;

import com.supremeai.model.ExistingProject;
import com.supremeai.repository.ProjectRepository;
import com.supremeai.util.IdUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Project management endpoints.
 * Users can only access their own projects. Admins can access all projects.
 */
@RestController
@RequestMapping("/api/projects")
public class ProjectsController {

    private final ProjectRepository projectRepository;

    public ProjectsController(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    /**
     * GET /api/projects - Get all projects.
     * Regular users only see their own projects. Admins see all projects.
     */
    @GetMapping
    public Flux<ExistingProject> getAllProjects() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("Not authenticated");
        }
        
        boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
        if (isAdmin) {
            return projectRepository.findAll();
        } else {
            // Regular user can only see their own projects
            return projectRepository.findByOwnerId(auth.getName());
        }
    }

    /**
     * GET /api/projects/owner/{ownerId} - Get projects by owner.
     * Only the owner themselves or admins can view projects for a specific owner.
     */
    @GetMapping("/owner/{ownerId}")
    public Flux<ExistingProject> getByOwner(@PathVariable String ownerId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("Not authenticated");
        }
        
        boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
        // Only allow access if requesting user is admin or the owner themselves
        if (!isAdmin && !auth.getName().equals(ownerId)) {
            throw new IllegalStateException("Access denied: You can only view your own projects");
        }
        
        return projectRepository.findByOwnerId(ownerId);
    }

    /**
     * POST /api/projects - Create a new project.
     * The project owner is automatically set to the current authenticated user.
     */
    @PostMapping
    public Mono<ExistingProject> createProject(@RequestBody ExistingProject project) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("Not authenticated");
        }
        
        // Set the owner to the current authenticated user
        project.setOwnerId(auth.getName());
        project.setId(IdUtils.ensureId(project.getId()));
        return projectRepository.save(project);
    }

    /**
     * PUT /api/projects/{id}/status - Update project status.
     * Only the project owner or admins can update a project.
     */
    @PutMapping("/{id}/status")
    public Mono<ExistingProject> updateProjectStatus(@PathVariable String id, @RequestParam String status) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("Not authenticated");
        }
        
        return projectRepository.findById(id)
                .flatMap(project -> {
                    boolean isAdmin = auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                    
                    // Only owner or admin can update
                    if (!isAdmin && !project.getOwnerId().equals(auth.getName())) {
                        throw new IllegalStateException("Access denied: You can only modify your own projects");
                    }
                    
                    project.setStatus(status);
                    return projectRepository.save(project);
                });
    }

    /**
     * DELETE /api/projects/{id} - Delete a project.
     * Only the project owner or admins can delete a project.
     */
    @DeleteMapping("/{id}")
    public Mono<Void> deleteProject(@PathVariable String id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("Not authenticated");
        }
        
        return projectRepository.findById(id)
                .flatMap(project -> {
                    boolean isAdmin = auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                    
                    // Only owner or admin can delete
                    if (!isAdmin && !project.getOwnerId().equals(auth.getName())) {
                        throw new IllegalStateException("Access denied: You can only delete your own projects");
                    }
                    
                    return projectRepository.deleteById(id);
                });
    }
}
