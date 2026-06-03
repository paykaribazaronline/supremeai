package com.supremeai.controller;

import com.supremeai.model.ExistingProject;
import com.supremeai.repository.ProjectRepository;
import com.supremeai.response.ApiResponse;
import com.supremeai.dto.ProjectCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ProjectsControllerTest {ProjectRepositorypublic ProjectsControllerTest(ProjectRepository projectRepository, ProjectsController controller) {
ProjectRepository    this.projectRepository = projectRepository;
ProjectRepository    this.controller = controller;
ProjectRepository}






    @BeforeEach
    void setUp() {
        controller = new ProjectsController(projectRepository);
        SecurityContextHolder.clearContext();
    }

    private void setAuthentication(String userId, boolean isAdmin) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(userId);
        if (isAdmin) {
            lenient().doReturn(Arrays.asList(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(auth).getAuthorities();
        } else {
            lenient().doReturn(Arrays.asList(new SimpleGrantedAuthority("ROLE_USER"))).when(auth).getAuthorities();
        }
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    @Test
    void getAllProjects_adminShouldSeeAllProjects() {
        setAuthentication("admin-1", true);

        ExistingProject p1 = new ExistingProject("p1", "Project A", "web", "user-1");
        ExistingProject p2 = new ExistingProject("p2", "Project B", "mobile", "user-2");

        when(projectRepository.findAll()).thenReturn(Flux.just(p1, p2));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        StepVerifier.create(controller.getAllProjects(auth))
                .expectNextMatches(response -> {
                    assertTrue(response.isSuccess());
                    assertEquals(2, response.getData().size());
                    return true;
                })
                .verifyComplete();

        verify(projectRepository).findAll();
        verify(projectRepository, never()).findByOwnerId(any());
    }

    @Test
    void getAllProjects_userShouldSeeOnlyOwnProjects() {
        setAuthentication("user-1", false);

        ExistingProject p1 = new ExistingProject("p1", "My Project", "web", "user-1");

        when(projectRepository.findByOwnerId("user-1")).thenReturn(Flux.just(p1));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        StepVerifier.create(controller.getAllProjects(auth))
                .expectNextMatches(response -> {
                    assertTrue(response.isSuccess());
                    assertEquals(1, response.getData().size());
                    assertEquals("user-1", response.getData().get(0).getOwnerId());
                    return true;
                })
                .verifyComplete();

        verify(projectRepository).findByOwnerId("user-1");
        verify(projectRepository, never()).findAll();
    }

    @Test
    void getAllProjects_shouldReturnError_whenNotAuthenticated() {
        SecurityContextHolder.clearContext();

        // Since we use @PreAuthorize, we mock an empty auth or null
        StepVerifier.create(controller.getAllProjects(null))
                .expectNextMatches(response -> {
                    assertFalse(response.isSuccess());
                    assertEquals("Not authenticated", response.getError());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getByOwner_adminShouldAccessAnyOwner() {
        setAuthentication("admin-1", true);

        ExistingProject p = new ExistingProject("p1", "User Project", "web", "user-1");
        when(projectRepository.findByOwnerId("user-1")).thenReturn(Flux.just(p));

        StepVerifier.create(controller.getByOwner("user-1"))
                .expectNextMatches(response -> {
                    assertTrue(response.isSuccess());
                    assertEquals(1, response.getData().size());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getByOwner_userShouldAccessOwnProjects() {
        setAuthentication("user-1", false);

        ExistingProject p = new ExistingProject("p1", "My Project", "web", "user-1");
        when(projectRepository.findByOwnerId("user-1")).thenReturn(Flux.just(p));

        StepVerifier.create(controller.getByOwner("user-1"))
                .expectNextMatches(response -> {
                    assertTrue(response.isSuccess());
                    assertEquals(1, response.getData().size());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getByOwner_userShouldDenyAccessToOtherOwners() {
        setAuthentication("user-1", false);

        StepVerifier.create(controller.getByOwner("user-2"))
                .expectNextMatches(response -> {
                    assertFalse(response.isSuccess());
                    assertTrue(response.getError().contains("Access denied"));
                    return true;
                })
                .verifyComplete();

        verify(projectRepository, never()).findByOwnerId(any());
    }

    @Test
    void createProject_shouldSetOwnerAndSave() {
        setAuthentication("user-1", false);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setName("New Project");

        when(projectRepository.save(any(ExistingProject.class))).thenAnswer(inv -> {
            ExistingProject p = inv.getArgument(0);
            p.setId("p-new");
            return Mono.just(p);
        });

        StepVerifier.create(controller.createProject(request, auth))
                .expectNextMatches(response -> {
                    assertTrue(response.isSuccess());
                    assertEquals("user-1", response.getData().getOwnerId());
                    return true;
                })
                .verifyComplete();

        verify(projectRepository).save(argThat(p -> "user-1".equals(p.getOwnerId())));
    }

    @Test
    void createProject_shouldReturnError_whenNotAuthenticated() {
        SecurityContextHolder.clearContext();
        
        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setName("Test");

        StepVerifier.create(controller.createProject(request, null))
                .expectNextMatches(response -> {
                    assertFalse(response.isSuccess());
                    assertEquals("Not authenticated", response.getError());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void updateProjectStatus_ownerShouldUpdateOwnProject() {
        setAuthentication("user-1", false);

        ExistingProject project = new ExistingProject("p1", "My Project", "web", "user-1");
        project.setStatus("ACTIVE");

        when(projectRepository.findById("p1")).thenReturn(Mono.just(project));
        when(projectRepository.save(any(ExistingProject.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(controller.updateProjectStatus("p1", "COMPLETED"))
                .expectNextMatches(response -> {
                    assertTrue(response.isSuccess());
                    assertEquals("COMPLETED", response.getData().getStatus());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void updateProjectStatus_adminShouldUpdateAnyProject() {
        setAuthentication("admin-1", true);

        ExistingProject project = new ExistingProject("p1", "User Project", "web", "user-1");
        project.setStatus("ACTIVE");

        when(projectRepository.findById("p1")).thenReturn(Mono.just(project));
        when(projectRepository.save(any(ExistingProject.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(controller.updateProjectStatus("p1", "ARCHIVED"))
                .expectNextMatches(response -> {
                    assertTrue(response.isSuccess());
                    assertEquals("ARCHIVED", response.getData().getStatus());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void updateProjectStatus_userShouldDenyUpdatingOthersProject() {
        setAuthentication("user-1", false);

        ExistingProject project = new ExistingProject("p1", "Other Project", "web", "user-2");

        when(projectRepository.findById("p1")).thenReturn(Mono.just(project));

        StepVerifier.create(controller.updateProjectStatus("p1", "COMPLETED"))
                .expectNextMatches(response -> {
                    assertFalse(response.isSuccess());
                    assertTrue(response.getError().contains("Access denied"));
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void deleteProject_ownerShouldDeleteOwnProject() {
        setAuthentication("user-1", false);

        ExistingProject project = new ExistingProject("p1", "My Project", "web", "user-1");

        when(projectRepository.findById("p1")).thenReturn(Mono.just(project));
        when(projectRepository.deleteById("p1")).thenReturn(Mono.empty());

        StepVerifier.create(controller.deleteProject("p1"))
                .expectNextMatches(response -> {
                    assertTrue(response.isSuccess());
                    assertEquals("Project deleted", response.getData());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void deleteProject_adminShouldDeleteAnyProject() {
        setAuthentication("admin-1", true);

        ExistingProject project = new ExistingProject("p1", "User Project", "web", "user-1");

        when(projectRepository.findById("p1")).thenReturn(Mono.just(project));
        when(projectRepository.deleteById("p1")).thenReturn(Mono.empty());

        StepVerifier.create(controller.deleteProject("p1"))
                .expectNextMatches(response -> {
                    assertTrue(response.isSuccess());
                    assertEquals("Project deleted", response.getData());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void deleteProject_userShouldDenyDeletingOthersProject() {
        setAuthentication("user-1", false);

        ExistingProject project = new ExistingProject("p1", "Other Project", "web", "user-2");

        when(projectRepository.findById("p1")).thenReturn(Mono.just(project));

        StepVerifier.create(controller.deleteProject("p1"))
                .expectNextMatches(response -> {
                    assertFalse(response.isSuccess());
                    assertTrue(response.getError().contains("Access denied"));
                    return true;
                })
                .verifyComplete();
    }
}
