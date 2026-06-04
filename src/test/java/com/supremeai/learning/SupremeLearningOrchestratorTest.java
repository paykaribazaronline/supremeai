package com.supremeai.learning;

import com.supremeai.model.SystemLearning;
import com.supremeai.service.AdminDashboardFacadeService;
import com.supremeai.service.SystemLearningService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupremeLearningOrchestratorTest {

    @Mock
    private AdminDashboardFacadeService adminDashboardFacadeService;

    @Mock
    private SystemLearningService systemLearningService;

    private SupremeLearningOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new SupremeLearningOrchestrator(adminDashboardFacadeService, systemLearningService);
    }

    @Test
    void findCoreKnowledgeSolution_FallsBackToSystemLearningWhenCoreKnowledgeMisses() {
        SystemLearning systemLearning = new SystemLearning();
        systemLearning.setTopic("Supreme AI");
        systemLearning.setCategory("ai_platform");
        systemLearning.setContent("Supreme AI is an autonomous intelligence framework for adaptive workflows.");
        systemLearning.setConfidenceScore(0.85);

        when(systemLearningService.getAllLearning()).thenReturn(Flux.fromIterable(List.of(systemLearning)));

        orchestrator.loadKnowledgeBase();

        String answer = orchestrator.findCoreKnowledgeSolution("What is Nova Framework?");

        assertEquals("Supreme AI is an autonomous intelligence framework for adaptive workflows.", answer);
    }
}
