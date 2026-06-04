package com.supremeai.repository;

import static org.mockito.Mockito.*;

import com.supremeai.model.AIBehaviorProfile;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AIBehaviorProfileRepositoryTest {

  @Mock private AIBehaviorProfileRepository repository;

  @Test
  void findByProjectId_shouldReturnProfilesForProject() {
    AIBehaviorProfile p1 =
        new AIBehaviorProfile(
            "abp-1",
            "proj-1",
            "1.0",
            AIBehaviorProfile.SecurityStrictness.HIGH,
            AIBehaviorProfile.PerformanceTradeoff.BALANCED,
            null);
    AIBehaviorProfile p2 =
        new AIBehaviorProfile(
            "abp-2",
            "proj-1",
            "1.1",
            AIBehaviorProfile.SecurityStrictness.MEDIUM,
            AIBehaviorProfile.PerformanceTradeoff.SPEED_OPTIMIZED,
            null);

    when(repository.findByProjectId("proj-1")).thenReturn(Flux.fromIterable(List.of(p1, p2)));

    StepVerifier.create(repository.findByProjectId("proj-1"))
        .expectNextMatches(p -> "abp-1".equals(p.getId()) && "proj-1".equals(p.getProjectId()))
        .expectNextMatches(p -> "abp-2".equals(p.getId()))
        .verifyComplete();
  }

  @Test
  void findByProjectId_shouldReturnEmpty_whenNotFound() {
    when(repository.findByProjectId("no-project")).thenReturn(Flux.empty());

    StepVerifier.create(repository.findByProjectId("no-project")).verifyComplete();
  }

  @Test
  void findFirstByProjectId_shouldReturnFirstProfile() {
    AIBehaviorProfile profile =
        new AIBehaviorProfile(
            "abp-3",
            "proj-2",
            "2.0",
            AIBehaviorProfile.SecurityStrictness.LOW,
            AIBehaviorProfile.PerformanceTradeoff.QUALITY_OPTIMIZED,
            null);

    when(repository.findFirstByProjectId("proj-2")).thenReturn(Mono.just(profile));

    StepVerifier.create(repository.findFirstByProjectId("proj-2"))
        .expectNextMatches(p -> "abp-3".equals(p.getId()))
        .verifyComplete();
  }

  @Test
  void findFirstByProjectId_shouldReturnEmpty_whenNotFound() {
    when(repository.findFirstByProjectId("missing")).thenReturn(Mono.empty());

    StepVerifier.create(repository.findFirstByProjectId("missing")).verifyComplete();
  }

  @Test
  void save_shouldPersistProfile() {
    AIBehaviorProfile profile =
        new AIBehaviorProfile(
            "abp-new",
            "proj-new",
            "1.0",
            AIBehaviorProfile.SecurityStrictness.HIGH,
            AIBehaviorProfile.PerformanceTradeoff.BALANCED,
            null);
    when(repository.save(profile)).thenReturn(Mono.just(profile));

    StepVerifier.create(repository.save(profile))
        .expectNextMatches(p -> "abp-new".equals(p.getId()))
        .verifyComplete();
  }
}
