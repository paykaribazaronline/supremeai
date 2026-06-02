package com.supremeai.controller;

import com.supremeai.model.ModelEvolution;
import com.supremeai.repository.ModelEvolutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvolutionControllerTest {ModelEvolutionRepositorypublic EvolutionControllerTest(ModelEvolutionRepository evolutionRepository, EvolutionController controller) {
ModelEvolutionRepository    this.evolutionRepository = evolutionRepository;
ModelEvolutionRepository    this.controller = controller;
ModelEvolutionRepository}




    @InjectMocks


    @Test
    void getAllEvolutionStates_shouldReturnAll() {
        ModelEvolution e1 = new ModelEvolution("evo-1", "GPT-4", 5, 1000, List.of("c1"), true);
        ModelEvolution e2 = new ModelEvolution("evo-2", "Claude", 3, 500, List.of("c2"), false);

        when(evolutionRepository.findAll()).thenReturn(Flux.just(e1, e2));

        StepVerifier.create(controller.getAllEvolutionStates())
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void getAllEvolutionStates_shouldReturnEmpty_whenNone() {
        when(evolutionRepository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(controller.getAllEvolutionStates())
                .verifyComplete();
    }

    @Test
    void saveEvolution_shouldPersistAndReturn() {
        ModelEvolution input = new ModelEvolution(null, "New Model", 1, 0, List.of(), false);
        ModelEvolution saved = new ModelEvolution("evo-new", "New Model", 1, 0, List.of(), false);

        when(evolutionRepository.save(input)).thenReturn(Mono.just(saved));

        StepVerifier.create(controller.saveEvolution(input))
                .expectNextMatches(e -> "evo-new".equals(e.getId()) && "New Model".equals(e.getName()))
                .verifyComplete();
    }

    @Test
    void updateEvolution_shouldSetIdAndSave() {
        ModelEvolution input = new ModelEvolution(null, "Updated Model", 2, 200, List.of("c3"), true);
        ModelEvolution saved = new ModelEvolution("evo-1", "Updated Model", 2, 200, List.of("c3"), true);

        when(evolutionRepository.save(any(ModelEvolution.class))).thenReturn(Mono.just(saved));

        StepVerifier.create(controller.updateEvolution("evo-1", input))
                .expectNextMatches(e -> "evo-1".equals(e.getId()) && "Updated Model".equals(e.getName()))
                .verifyComplete();

        verify(evolutionRepository).save(argThat(e -> "evo-1".equals(e.getId())));
    }
}
