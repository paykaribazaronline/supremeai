package com.supremeai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AutonomousQuestioningEngineTest {

  private AutonomousQuestioningEngine agent;
  private SupremeAIBrain mockBrain;
  private DynamicSignatureRegistry mockRegistry;

  @BeforeEach
  void setUp() throws Exception {
    agent = new AutonomousQuestioningEngine();

    // Set mocks via reflection
    mockBrain = org.mockito.Mockito.mock(SupremeAIBrain.class);
    mockRegistry = org.mockito.Mockito.mock(DynamicSignatureRegistry.class);

    setField(agent, "supremeAIBrain", mockBrain);
    setField(agent, "signatureRegistry", mockRegistry);

    org.mockito.Mockito.when(
            mockBrain.think(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(reactor.core.publisher.Mono.just("FACTUAL"));
    org.mockito.Mockito.when(
            mockRegistry.matchesAny(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(false);
  }

  private void setField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  @Test
  void testClassifyIntentFactual() {
    AutonomousQuestioningEngine.IntentType result = agent.classifyIntent("what is java");
    assertEquals(AutonomousQuestioningEngine.IntentType.FACTUAL, result);
  }

  @Test
  void testClassifyIntentGreeting() throws Exception {
    org.mockito.Mockito.when(
            mockRegistry.matchesAny(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(true);
    org.mockito.Mockito.when(
            mockBrain.think(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(reactor.core.publisher.Mono.just("GREETING"));

    AutonomousQuestioningEngine.IntentType result = agent.classifyIntent("hi there");
    assertEquals(AutonomousQuestioningEngine.IntentType.GREETING, result);
  }

  @Test
  void testClassifyIntentTask() throws Exception {
    org.mockito.Mockito.when(
            mockRegistry.matchesAny(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(true);
    org.mockito.Mockito.when(
            mockBrain.think(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(reactor.core.publisher.Mono.just("TASK"));

    AutonomousQuestioningEngine.IntentType result = agent.classifyIntent("build a microservice");
    assertEquals(AutonomousQuestioningEngine.IntentType.TASK, result);
  }
}
