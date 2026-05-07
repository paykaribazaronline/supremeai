package com.supremeai.controller;

import com.supremeai.model.Milestone;
import com.supremeai.repository.MilestoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MilestoneControllerTest {

    @Mock
    private MilestoneRepository milestoneRepository;

    @InjectMocks
    private MilestoneController controller;

    @Test
    void getAllMilestones_shouldReturnOrderedMilestones() {
        Milestone m1 = new Milestone("ms-1", "Design", "Week 1", 100, "blue", "pencil", 1);
        Milestone m2 = new Milestone("ms-2", "Dev", "Week 2", 50, "green", "code", 2);

        when(milestoneRepository.findAllByOrderByOrderAsc()).thenReturn(Flux.just(m1, m2));

        StepVerifier.create(controller.getAllMilestones())
                .expectNextMatches(m -> m.getOrder() == 1)
                .expectNextMatches(m -> m.getOrder() == 2)
                .verifyComplete();
    }

    @Test
    void getAllMilestones_shouldReturnEmpty_whenNone() {
        when(milestoneRepository.findAllByOrderByOrderAsc()).thenReturn(Flux.empty());

        StepVerifier.create(controller.getAllMilestones())
                .verifyComplete();
    }

    @Test
    void saveMilestone_shouldPersistAndReturn() {
        Milestone input = new Milestone(null, "New", "Week 3", 0, "red", "star", 3);
        Milestone saved = new Milestone("ms-new", "New", "Week 3", 0, "red", "star", 3);

        when(milestoneRepository.save(input)).thenReturn(Mono.just(saved));

        StepVerifier.create(controller.saveMilestone(input))
                .expectNextMatches(m -> "ms-new".equals(m.getId()) && "New".equals(m.getTitle()))
                .verifyComplete();
    }

    @Test
    void updateMilestone_shouldSetIdAndSave() {
        Milestone input = new Milestone(null, "Updated", "Week 4", 25, "yellow", "check", 4);
        Milestone saved = new Milestone("ms-1", "Updated", "Week 4", 25, "yellow", "check", 4);

        when(milestoneRepository.save(any(Milestone.class))).thenReturn(Mono.just(saved));

        StepVerifier.create(controller.updateMilestone("ms-1", input))
                .expectNextMatches(m -> "ms-1".equals(m.getId()) && "Updated".equals(m.getTitle()))
                .verifyComplete();

        verify(milestoneRepository).save(argThat(m -> "ms-1".equals(m.getId())));
    }

    @Test
    void deleteMilestone_shouldRemoveMilestone() {
        when(milestoneRepository.deleteById("ms-1")).thenReturn(Mono.empty());

        StepVerifier.create(controller.deleteMilestone("ms-1"))
                .verifyComplete();

        verify(milestoneRepository).deleteById("ms-1");
    }
}
