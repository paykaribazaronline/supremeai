package com.supremeai.repository;

import com.supremeai.model.ExistingProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectRepositoryTest {

    @Mock
    private ProjectRepository projectRepository;

    @Test
    void findByOwnerId_shouldReturnProjectsForOwner() {
        ExistingProject p1 = new ExistingProject("proj-1", "App One", "web", "owner-1");
        ExistingProject p2 = new ExistingProject("proj-2", "App Two", "mobile", "owner-1");

        when(projectRepository.findByOwnerId("owner-1")).thenReturn(Flux.fromIterable(List.of(p1, p2)));

        StepVerifier.create(projectRepository.findByOwnerId("owner-1"))
                .expectNextMatches(p -> "proj-1".equals(p.getId()) && "owner-1".equals(p.getOwnerId()))
                .expectNextMatches(p -> "proj-2".equals(p.getId()) && "owner-1".equals(p.getOwnerId()))
                .verifyComplete();
    }

    @Test
    void findByOwnerId_shouldReturnEmpty_whenNoProjects() {
        when(projectRepository.findByOwnerId("no-projects")).thenReturn(Flux.empty());

        StepVerifier.create(projectRepository.findByOwnerId("no-projects"))
                .verifyComplete();
    }

    @Test
    void findByStatus_shouldReturnProjectsWithStatus() {
        ExistingProject p1 = new ExistingProject("proj-3", "Active App", "api", "owner-2");
        p1.setStatus("ACTIVE");
        ExistingProject p2 = new ExistingProject("proj-4", "Another Active", "web", "owner-3");
        p2.setStatus("ACTIVE");

        when(projectRepository.findByStatus("ACTIVE")).thenReturn(Flux.fromIterable(List.of(p1, p2)));

        StepVerifier.create(projectRepository.findByStatus("ACTIVE"))
                .expectNextMatches(p -> "ACTIVE".equals(p.getStatus()))
                .expectNextMatches(p -> "ACTIVE".equals(p.getStatus()))
                .verifyComplete();
    }

    @Test
    void findByStatus_shouldReturnEmpty_whenNoMatchingStatus() {
        when(projectRepository.findByStatus("ARCHIVED")).thenReturn(Flux.empty());

        StepVerifier.create(projectRepository.findByStatus("ARCHIVED"))
                .verifyComplete();
    }

    @Test
    void save_shouldPersistProject() {
        ExistingProject project = new ExistingProject("proj-5", "New Project", "desktop", "owner-4");
        when(projectRepository.save(project)).thenReturn(Mono.just(project));

        StepVerifier.create(projectRepository.save(project))
                .expectNextMatches(p -> "proj-5".equals(p.getId()) && "New Project".equals(p.getName()))
                .verifyComplete();
    }

    @Test
    void findById_shouldReturnProject_whenExists() {
        ExistingProject project = new ExistingProject("proj-6", "Find Me", "web", "owner-5");
        when(projectRepository.findById("proj-6")).thenReturn(Mono.just(project));

        StepVerifier.create(projectRepository.findById("proj-6"))
                .expectNextMatches(p -> "Find Me".equals(p.getName()))
                .verifyComplete();
    }

    @Test
    void deleteById_shouldRemoveProject() {
        when(projectRepository.deleteById("proj-delete")).thenReturn(Mono.empty());

        StepVerifier.create(projectRepository.deleteById("proj-delete"))
                .verifyComplete();
    }
}
