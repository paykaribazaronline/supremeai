package com.supremeai.repository;

import static org.mockito.Mockito.*;

import com.supremeai.model.ChatPlan;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ChatPlanRepositoryTest {

  @Mock private ChatPlanRepository repository;

  @Test
  void findByActive_shouldReturnActivePlans() {
    ChatPlan p1 = new ChatPlan("chat-1", "Deploy to prod", 0.95, "ai");
    p1.setId("plan-1");
    ChatPlan p2 = new ChatPlan("chat-2", "Run tests", 0.88, "ai");
    p2.setId("plan-2");

    when(repository.findByActive(true)).thenReturn(Flux.fromIterable(List.of(p1, p2)));

    StepVerifier.create(repository.findByActive(true)).expectNextCount(2).verifyComplete();
  }

  @Test
  void findByActive_shouldReturnEmpty_whenNoneActive() {
    when(repository.findByActive(true)).thenReturn(Flux.empty());

    StepVerifier.create(repository.findByActive(true)).verifyComplete();
  }

  @Test
  void findByIdAndActive_shouldReturnPlan_whenActive() {
    ChatPlan p = new ChatPlan("chat-3", "Plan content", 0.75, "human");
    p.setId("plan-3");
    p.setActive(true);

    when(repository.findByIdAndActive("plan-3", true)).thenReturn(Mono.just(p));

    StepVerifier.create(repository.findByIdAndActive("plan-3", true))
        .expectNextMatches(plan -> "plan-3".equals(plan.getId()) && plan.isActive())
        .verifyComplete();
  }

  @Test
  void findByIdAndActive_shouldReturnEmpty_whenNotActive() {
    when(repository.findByIdAndActive("plan-4", true)).thenReturn(Mono.empty());

    StepVerifier.create(repository.findByIdAndActive("plan-4", true)).verifyComplete();
  }

  @Test
  void save_shouldPersistPlan() {
    ChatPlan p = new ChatPlan("chat-new", "New plan content", 0.8, "ai");
    when(repository.save(p)).thenReturn(Mono.just(p));

    StepVerifier.create(repository.save(p))
        .expectNextMatches(plan -> "New plan content".equals(plan.getContent()))
        .verifyComplete();
  }
}
