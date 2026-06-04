package com.supremeai.repository;

import static org.mockito.Mockito.*;

import com.supremeai.learning.knowledge.SolutionMemory;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class SolutionMemoryRepositoryTest {

  @Mock(answer = org.mockito.Answers.CALLS_REAL_METHODS)
  private SolutionMemoryRepository repository;

  @Test
  void findByTriggerError_shouldFilterByErrorSignature() {
    SolutionMemory s1 = new SolutionMemory("NPE at line 42", "null check", "OpenAI", 100, 0.9);
    s1.setId("sm-1");
    SolutionMemory s2 = new SolutionMemory("ClassCastException", "cast fix", "Anthropic", 200, 0.8);
    s2.setId("sm-2");
    SolutionMemory s3 =
        new SolutionMemory("NPE at line 99", "Objects.requireNonNull", "Groq", 50, 0.95);
    s3.setId("sm-3");

    when(repository.findAll()).thenReturn(Flux.fromIterable(List.of(s1, s2, s3)));

    StepVerifier.create(repository.findByTriggerError("NPE at line 42"))
        .expectNextMatches(s -> "sm-1".equals(s.getId()))
        .verifyComplete();
  }

  @Test
  void findByTriggerError_shouldReturnEmpty_whenNoMatch() {
    SolutionMemory s = new SolutionMemory("Some other error", "fix", "OpenAI", 100, 0.7);
    s.setId("sm-4");

    when(repository.findAll()).thenReturn(Flux.just(s));

    StepVerifier.create(repository.findByTriggerError("nonexistent error")).verifyComplete();
  }

  @Test
  void findTopSolutionsByError_shouldReturnTopNByScore() {
    SolutionMemory s1 = new SolutionMemory("error-X", "fix-a", "OpenAI", 100, 0.9);
    s1.setId("sm-5");
    s1.setSuccessCount(9);
    s1.setFailureCount(1);
    SolutionMemory s2 = new SolutionMemory("error-X", "fix-b", "Anthropic", 50, 0.8);
    s2.setId("sm-6");
    s2.setSuccessCount(5);
    s2.setFailureCount(5);
    SolutionMemory s3 = new SolutionMemory("error-X", "fix-c", "Groq", 200, 0.95);
    s3.setId("sm-7");
    s3.setSuccessCount(8);
    s3.setFailureCount(2);

    when(repository.findAll()).thenReturn(Flux.fromIterable(List.of(s1, s2, s3)));

    StepVerifier.create(repository.findTopSolutionsByError("error-X", 2))
        .expectNextCount(2)
        .verifyComplete();
  }

  @Test
  void findTopSolutionsByError_shouldReturnEmpty_whenNoMatch() {
    SolutionMemory s = new SolutionMemory("other", "fix", "OpenAI", 100, 0.7);
    s.setId("sm-8");

    when(repository.findAll()).thenReturn(Flux.just(s));

    StepVerifier.create(repository.findTopSolutionsByError("error-X", 2)).verifyComplete();
  }

  @Test
  void save_shouldPersistSolution() {
    SolutionMemory s = new SolutionMemory("new-error", "new-fix", "OpenAI", 75, 0.85);
    when(repository.save(s)).thenReturn(Mono.just(s));

    StepVerifier.create(repository.save(s))
        .expectNextMatches(sm -> "new-error".equals(sm.getTriggerError()))
        .verifyComplete();
  }

  @Test
  void findById_shouldReturnSolution_whenExists() {
    SolutionMemory s = new SolutionMemory("find-error", "find-fix", "OpenAI", 100, 0.8);
    s.setId("sm-find");
    when(repository.findById("sm-find")).thenReturn(Mono.just(s));

    StepVerifier.create(repository.findById("sm-find"))
        .expectNextMatches(sm -> "find-error".equals(sm.getTriggerError()))
        .verifyComplete();
  }

  @Test
  void delete_shouldRemoveSolution() {
    SolutionMemory s = new SolutionMemory("del-error", "del-fix", "OpenAI", 100, 0.8);
    when(repository.delete(s)).thenReturn(Mono.empty());

    StepVerifier.create(repository.delete(s)).verifyComplete();
  }
}
