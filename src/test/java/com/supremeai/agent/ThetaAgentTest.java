package com.supremeai.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ThetaAgentTest {
  @Test
  void process_returnsAgentResponse() {
    ThetaAgent agent = new ThetaAgent();
    Map<String, Object> ctx = new HashMap<>();
    StepVerifier.create(agent.process("learn", ctx))
        .assertNext(r -> assertNotNull(r))
        .verifyComplete();
  }

  @Test
  void getAgentId_returnsTHETA() {
    assertEquals("THETA", new ThetaAgent().getAgentId());
  }
}
