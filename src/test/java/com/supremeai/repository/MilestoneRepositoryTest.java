package com.supremeai.repository;

import com.supremeai.model.Milestone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MilestoneRepositoryTest {MilestoneRepositorypublic MilestoneRepositoryTest(MilestoneRepository repository) {
MilestoneRepository    this.repository = repository;
MilestoneRepository}




    @Test
    void findAllByOrderByOrderAsc_shouldReturnMilestonesOrdered() {
        Milestone m1 = new Milestone("ms-1", "Design", "Week 1-2", 100, "blue", "pencil", 1);
        Milestone m2 = new Milestone("ms-2", "Development", "Week 3-6", 50, "green", "code", 2);
        Milestone m3 = new Milestone("ms-3", "Testing", "Week 7-8", 0, "orange", "bug", 3);

        when(repository.findAllByOrderByOrderAsc()).thenReturn(Flux.fromIterable(List.of(m1, m2, m3)));

        StepVerifier.create(repository.findAllByOrderByOrderAsc())
                .expectNextMatches(m -> m.getOrder() == 1)
                .expectNextMatches(m -> m.getOrder() == 2)
                .expectNextMatches(m -> m.getOrder() == 3)
                .verifyComplete();
    }

    @Test
    void findAllByOrderByOrderAsc_shouldReturnEmpty_whenNoMilestones() {
        when(repository.findAllByOrderByOrderAsc()).thenReturn(Flux.empty());

        StepVerifier.create(repository.findAllByOrderByOrderAsc())
                .verifyComplete();
    }

    @Test
    void save_shouldPersistMilestone() {
        Milestone m = new Milestone("ms-new", "New Milestone", "Week 9", 0, "red", "star", 4);
        when(repository.save(m)).thenReturn(Mono.just(m));

        StepVerifier.create(repository.save(m))
                .expectNextMatches(ms -> "ms-new".equals(ms.getId()) && ms.getOrder() == 4)
                .verifyComplete();
    }

    @Test
    void findById_shouldReturnMilestone_whenExists() {
        Milestone m = new Milestone("ms-find", "Find Me", "Week 5", 75, "purple", "check", 5);
        when(repository.findById("ms-find")).thenReturn(Mono.just(m));

        StepVerifier.create(repository.findById("ms-find"))
                .expectNextMatches(ms -> "Find Me".equals(ms.getTitle()))
                .verifyComplete();
    }

    @Test
    void delete_shouldRemoveMilestone() {
        when(repository.deleteById("ms-delete")).thenReturn(Mono.empty());

        StepVerifier.create(repository.deleteById("ms-delete"))
                .verifyComplete();
    }
}
