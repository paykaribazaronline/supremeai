package com.supremeai.repository;

import static org.mockito.Mockito.*;

import com.supremeai.model.ModelEvolution;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ModelEvolutionRepositoryTest {

  @Mock private ModelEvolutionRepository repository;

  @Test
  void save_shouldPersistEvolution() {
    ModelEvolution evo =
        new ModelEvolution("evo-1", "GPT-4", 5, 1000, List.of("contribution-1"), true);
    when(repository.save(evo)).thenReturn(Mono.just(evo));

    StepVerifier.create(repository.save(evo))
        .expectNextMatches(e -> "evo-1".equals(e.getId()) && "GPT-4".equals(e.getName()))
        .verifyComplete();
  }

  @Test
  void findById_shouldReturnEvolution_whenExists() {
    ModelEvolution evo = new ModelEvolution("evo-2", "Claude", 3, 500, List.of("c1", "c2"), false);
    when(repository.findById("evo-2")).thenReturn(Mono.just(evo));

    StepVerifier.create(repository.findById("evo-2"))
        .expectNextMatches(e -> e.getLevel() == 3 && e.getXp() == 500)
        .verifyComplete();
  }

  @Test
  void findAll_shouldReturnAllEvolutions() {
    ModelEvolution e1 = new ModelEvolution("evo-3", "Model A", 1, 100, List.of(), false);
    ModelEvolution e2 = new ModelEvolution("evo-4", "Model B", 2, 200, List.of(), true);

    when(repository.findAll()).thenReturn(Flux.fromIterable(List.of(e1, e2)));

    StepVerifier.create(repository.findAll()).expectNextCount(2).verifyComplete();
  }

  @Test
  void deleteById_shouldRemoveEvolution() {
    when(repository.deleteById("evo-delete")).thenReturn(Mono.empty());

    StepVerifier.create(repository.deleteById("evo-delete")).verifyComplete();
  }
}
