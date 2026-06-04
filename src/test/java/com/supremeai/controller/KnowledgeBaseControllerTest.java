package com.supremeai.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.supremeai.learning.knowledge.GlobalKnowledgeBase;
import com.supremeai.learning.knowledge.SolutionMemory;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class KnowledgeBaseControllerTest {

  @Mock private GlobalKnowledgeBase globalKnowledgeBase;

  @InjectMocks private KnowledgeBaseController controller;

  private SolutionMemory sampleSolution;

  @BeforeEach
  void setUp() {
    sampleSolution = new SolutionMemory();
    sampleSolution.setId("solution-1");
    sampleSolution.setTriggerError("NullPointerException at Main.java:42");
    sampleSolution.setResolvedCode("if (obj != null) { obj.method(); }");
    sampleSolution.setSecurityScore(0.95);
  }

  @Test
  void getSolution_whenFound_shouldReturnFoundResponse() {
    when(globalKnowledgeBase.findKnownSolution("NullPointerException at Main.java:42"))
        .thenReturn(Mono.just("Add null check before obj.method()"));

    KnowledgeBaseController.SolutionResponse response =
        controller.getSolution("NullPointerException at Main.java:42").block();

    assertTrue(response.found);
    assertEquals("Add null check before obj.method()", response.solution);
    assertEquals("Solution found", response.message);
  }

  @Test
  void getSolution_whenNotFound_shouldReturnNotFoundResponse() {
    when(globalKnowledgeBase.findKnownSolution("UnknownError")).thenReturn(Mono.empty());

    KnowledgeBaseController.SolutionResponse response =
        controller.getSolution("UnknownError").block();

    assertFalse(response.found);
    assertNull(response.solution);
    assertEquals("No known solution for this error", response.message);
  }

  @Test
  void getAllSolutions_shouldReturnAllSolutionsForError() {
    List<SolutionMemory> solutions = Arrays.asList(sampleSolution);
    when(globalKnowledgeBase.getSolutions("NullPointerException"))
        .thenReturn(Flux.fromIterable(solutions));

    List<SolutionMemory> result =
        controller.getAllSolutions("NullPointerException").collectList().block();

    assertEquals(1, result.size());
    assertEquals(sampleSolution, result.get(0));
    verify(globalKnowledgeBase).getSolutions("NullPointerException");
  }

  @Test
  void getAllSolutions_whenNoSolutions_shouldReturnEmptyList() {
    when(globalKnowledgeBase.getSolutions("UnknownError")).thenReturn(Flux.empty());

    List<SolutionMemory> result = controller.getAllSolutions("UnknownError").collectList().block();

    assertTrue(result.isEmpty());
  }

  @Test
  void learnSolution_shouldRecordSolutionWithPermission() {
    KnowledgeBaseController.LearnSolutionRequest request =
        new KnowledgeBaseController.LearnSolutionRequest();
    request.setErrorSignature("CustomError");
    request.setResolvedCode("fixed code");
    request.setProvider("openai");
    request.setExecutionTimeMs(150L);
    request.setSecurityScore(0.95);

    when(globalKnowledgeBase.recordSuccessWithPermission(
            any(), any(), any(), anyLong(), anyDouble()))
        .thenReturn(Mono.empty());

    String result = controller.learnSolution(request).block();

    assertEquals("Solution recorded (subject to auto-pilot/approval rules)", result);
    verify(globalKnowledgeBase)
        .recordSuccessWithPermission("CustomError", "fixed code", "openai", 150L, 0.95);
  }

  @Test
  void recordFailure_shouldRecordFailure() {
    KnowledgeBaseController.RecordFailureRequest request =
        new KnowledgeBaseController.RecordFailureRequest();
    request.setErrorSignature("FailedError");
    request.setFailedCode("failed code attempt");

    when(globalKnowledgeBase.recordFailure(any(), any())).thenReturn(Mono.empty());

    String result = controller.recordFailure(request).block();

    assertEquals("Failure recorded", result);
    verify(globalKnowledgeBase).recordFailure("FailedError", "failed code attempt");
  }

  @Test
  void getStats_shouldReturnKnowledgeStats() {
    when(globalKnowledgeBase.countSolutions()).thenReturn(Mono.just(42L));
    KnowledgeBaseController.KnowledgeStats stats = controller.getStats().block();

    assertEquals("Total solutions: 42", stats.note);
  }
}
