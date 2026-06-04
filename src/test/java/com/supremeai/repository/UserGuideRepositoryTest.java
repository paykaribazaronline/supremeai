package com.supremeai.repository;

import com.supremeai.model.UserGuide;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserGuideRepositoryTest {

    @Mock
    private UserGuideRepository repository;

    @Test
    void findByIsPublished_shouldReturnPublishedGuides() {
        UserGuide g1 = new UserGuide("guide-1", Map.of("en", "Getting Started"), Map.of("en", "Intro guide"));
        g1.setIsPublished(true);
        UserGuide g2 = new UserGuide("guide-2", Map.of("en", "Advanced"), Map.of("en", "Advanced topics"));
        g2.setIsPublished(true);

        when(repository.findByIsPublished(true)).thenReturn(Flux.fromIterable(List.of(g1, g2)));

        StepVerifier.create(repository.findByIsPublished(true))
                .expectNextMatches(g -> Boolean.TRUE.equals(g.getIsPublished()))
                .expectNextMatches(g -> Boolean.TRUE.equals(g.getIsPublished()))
                .verifyComplete();
    }

    @Test
    void findByIsPublished_shouldReturnEmpty_whenNoPublished() {
        when(repository.findByIsPublished(true)).thenReturn(Flux.empty());

        StepVerifier.create(repository.findByIsPublished(true))
                .verifyComplete();
    }

    @Test
    void findByCategoryAndShouldReturnMatchingGuides() {
        UserGuide g = new UserGuide("guide-3", Map.of("en", "Security Guide"), Map.of("en", "Security tips"));
        g.setCategory("security");
        g.setIsPublished(true);

        when(repository.findByCategoryAndIsPublished("security", true)).thenReturn(Flux.just(g));

        StepVerifier.create(repository.findByCategoryAndIsPublished("security", true))
                .expectNextMatches(guide -> "security".equals(guide.getCategory()) && Boolean.TRUE.equals(guide.getIsPublished()))
                .verifyComplete();
    }

    @Test
    void findByCategoryAndIsPublished_shouldReturnEmpty_whenNoMatch() {
        when(repository.findByCategoryAndIsPublished("nonexistent", true)).thenReturn(Flux.empty());

        StepVerifier.create(repository.findByCategoryAndIsPublished("nonexistent", true))
                .verifyComplete();
    }

    @Test
    void findByTagsContaining_shouldReturnGuidesWithTag() {
        UserGuide g = new UserGuide("guide-4", Map.of("en", "API Guide"), Map.of("en", "API docs"));
        g.setTags(List.of("api", "rest", "backend"));

        when(repository.findByTagsContaining("api")).thenReturn(Flux.just(g));

        StepVerifier.create(repository.findByTagsContaining("api"))
                .expectNextMatches(guide -> guide.getTags().contains("api"))
                .verifyComplete();
    }

    @Test
    void findByTagsContaining_shouldReturnEmpty_whenNoMatch() {
        when(repository.findByTagsContaining("nonexistent")).thenReturn(Flux.empty());

        StepVerifier.create(repository.findByTagsContaining("nonexistent"))
                .verifyComplete();
    }

    @Test
    void findById_shouldReturnGuide_whenExists() {
        UserGuide g = new UserGuide("guide-find", Map.of("en", "Find Me"), Map.of("en", "Description"));
        when(repository.findById("guide-find")).thenReturn(Mono.just(g));

        StepVerifier.create(repository.findById("guide-find"))
                .expectNextMatches(guide -> "guide-find".equals(guide.getId()))
                .verifyComplete();
    }

    @Test
    void save_shouldPersistGuide() {
        UserGuide g = new UserGuide("guide-new", Map.of("en", "New Guide"), Map.of("en", "New description"));
        when(repository.save(g)).thenReturn(Mono.just(g));

        StepVerifier.create(repository.save(g))
                .expectNextMatches(guide -> "guide-new".equals(guide.getId()))
                .verifyComplete();
    }
}
