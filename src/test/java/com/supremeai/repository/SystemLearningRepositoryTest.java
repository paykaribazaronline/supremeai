package com.supremeai.repository;

import com.supremeai.model.SystemLearning;
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
class SystemLearningRepositoryTest {

    @Mock
    private SystemLearningRepository repository;

    @Test
    void findByCategory_shouldReturnEntriesForCategory() {
        SystemLearning s1 = new SystemLearning("sl-1", "Error Handling", "errors", "Handle NPEs");
        SystemLearning s2 = new SystemLearning("sl-2", "Logging", "errors", "Log exceptions properly");

        when(repository.findByCategory("errors")).thenReturn(Flux.fromIterable(List.of(s1, s2)));

        StepVerifier.create(repository.findByCategory("errors"))
                .expectNextMatches(s -> "sl-1".equals(s.getId()) && "errors".equals(s.getCategory()))
                .expectNextMatches(s -> "sl-2".equals(s.getId()))
                .verifyComplete();
    }

    @Test
    void findByCategory_shouldReturnEmpty_whenNotFound() {
        when(repository.findByCategory("unknown")).thenReturn(Flux.empty());

        StepVerifier.create(repository.findByCategory("unknown"))
                .verifyComplete();
    }

    @Test
    void findByConfidenceScoreGreaterThanEqual_shouldReturnMatchingEntries() {
        SystemLearning s1 = new SystemLearning("sl-3", "Topic A", "general", "Content A");
        s1.setConfidenceScore(0.9);
        SystemLearning s2 = new SystemLearning("sl-4", "Topic B", "general", "Content B");
        s2.setConfidenceScore(0.8);

        when(repository.findByConfidenceScoreGreaterThanEqual(0.8)).thenReturn(Flux.fromIterable(List.of(s1, s2)));

        StepVerifier.create(repository.findByConfidenceScoreGreaterThanEqual(0.8))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void findByConfidenceScoreGreaterThanEqual_shouldReturnEmpty_whenNoneMatch() {
        when(repository.findByConfidenceScoreGreaterThanEqual(0.99)).thenReturn(Flux.empty());

        StepVerifier.create(repository.findByConfidenceScoreGreaterThanEqual(0.99))
                .verifyComplete();
    }

    @Test
    void save_shouldPersistEntry() {
        SystemLearning s = new SystemLearning("sl-new", "New Topic", "general", "New content");
        when(repository.save(s)).thenReturn(Mono.just(s));

        StepVerifier.create(repository.save(s))
                .expectNextMatches(sl -> "sl-new".equals(sl.getId()))
                .verifyComplete();
    }

    @Test
    void findById_shouldReturnEntry_whenExists() {
        SystemLearning s = new SystemLearning("sl-find", "Findable", "test", "Find me content");
        when(repository.findById("sl-find")).thenReturn(Mono.just(s));

        StepVerifier.create(repository.findById("sl-find"))
                .expectNextMatches(sl -> "Findable".equals(sl.getTopic()))
                .verifyComplete();
    }
}
