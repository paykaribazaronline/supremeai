package com.supremeai.controller;

import com.supremeai.dto.ProjectCreateRequest;
import com.supremeai.model.ExistingProject;
import com.supremeai.repository.ProjectRepository;
import com.supremeai.response.ApiResponse;
import com.supremeai.util.IdUtils;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Project management endpoints. Users can only access their own projects. Admins can access all
 * projects.
 */
@RestController
@RequestMapping("/api/projects")
public class ProjectsController {

  private final ProjectRepository projectRepository;

  public ProjectsController(ProjectRepository projectRepository) {
    this.projectRepository = projectRepository;
  }

  /**
   * GET /api/projects - Get all projects. Regular users only see their own projects. Admins see all
   * projects.
   */
  @GetMapping
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  public Mono<ApiResponse<List<ExistingProject>>> getAllProjects(Authentication auth) {
    if (auth == null || auth.getName() == null) {
      return Mono.just(ApiResponse.error("Not authenticated"));
    }

    boolean isAdmin =
        auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

    if (isAdmin) {
      return projectRepository.findAll().collectList().map(ApiResponse::ok);
    } else {
      return projectRepository.findByOwnerId(auth.getName()).collectList().map(ApiResponse::ok);
    }
  }

  /**
   * GET /api/projects/owner/{ownerId} - Get projects by owner. Only the owner themselves or admins
   * can view projects for a specific owner.
   */
  @GetMapping("/owner/{ownerId}")
  @PreAuthorize("hasRole('ADMIN') or #ownerId == authentication.name")
  public Mono<ApiResponse<List<ExistingProject>>> getByOwner(@PathVariable String ownerId) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getName() == null) {
      return Mono.just(ApiResponse.error("Not authenticated"));
    }

    boolean isAdmin =
        auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

    if (!isAdmin && !ownerId.equals(auth.getName())) {
      return Mono.just(ApiResponse.error("Access denied: You can only view your own projects"));
    }

    return projectRepository.findByOwnerId(ownerId).collectList().map(ApiResponse::ok);
  }

  /**
   * POST /api/projects - Create a new project. The project owner is automatically set to the
   * current authenticated user.
   */
  @PostMapping
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  public Mono<ApiResponse<ExistingProject>> createProject(
      @Valid @RequestBody ProjectCreateRequest request, Authentication auth) {
    if (auth == null || auth.getName() == null) {
      return Mono.just(ApiResponse.error("Not authenticated"));
    }

    ExistingProject project = new ExistingProject();
    project.setName(request.getName());
    project.setDescription(request.getDescription());
    project.setOwnerId(auth.getName());
    project.setId(IdUtils.ensureId(UUID.randomUUID().toString()));
    return projectRepository.save(project).map(ApiResponse::ok);
  }

  /**
   * PUT /api/projects/{id}/status - Update project status. Only the project owner or admins can
   * update a project.
   */
  @PutMapping("/{id}/status")
  public Mono<ApiResponse<ExistingProject>> updateProjectStatus(
      @PathVariable String id, @RequestParam String status) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getName() == null) {
      return Mono.just(ApiResponse.error("Not authenticated"));
    }

    return projectRepository
        .findById(id)
        .flatMap(
            project -> {
              boolean isAdmin =
                  auth.getAuthorities().stream()
                      .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

              // Only owner or admin can update
              if (!isAdmin && !project.getOwnerId().equals(auth.getName())) {
                return Mono.error(
                    new IllegalStateException(
                        "Access denied: You can only modify your own projects"));
              }

              project.setStatus(status);
              return projectRepository.save(project);
            })
        .map(ApiResponse::ok)
        .onErrorResume(e -> Mono.just(ApiResponse.error(e.getMessage())));
  }

  /**
   * DELETE /api/projects/{id} - Delete a project. Only the project owner or admins can delete a
   * project.
   */
  @DeleteMapping("/{id}")
  public Mono<ApiResponse<String>> deleteProject(@PathVariable String id) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getName() == null) {
      return Mono.just(ApiResponse.error("Not authenticated"));
    }

    return projectRepository
        .findById(id)
        .flatMap(
            project -> {
              boolean isAdmin =
                  auth.getAuthorities().stream()
                      .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

              // Only owner or admin can delete
              if (!isAdmin && !project.getOwnerId().equals(auth.getName())) {
                return Mono.error(
                    new IllegalStateException(
                        "Access denied: You can only delete your own projects"));
              }

              return projectRepository.deleteById(id).thenReturn(ApiResponse.ok("Project deleted"));
            })
        .onErrorResume(e -> Mono.just(ApiResponse.error(e.getMessage())));
  }
}
