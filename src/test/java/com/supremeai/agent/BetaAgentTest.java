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
class BetaAgentTest {
  @Test
  void process_returnsAgentResponse() {
    BetaAgent agent = new BetaAgent();
    Map<String, Object> ctx = new HashMap<>();
    StepVerifier.create(agent.process("gdpr check", ctx))
        .assertNext(r -> assertNotNull(r))
        .verifyComplete();
  }

  @Test
  void getAgentId_returnsBETA() {
    assertEquals("BETA", new BetaAgent().getAgentId());
  }

  @Test
  void getAgentName_returnsCompliance() {
    assertEquals("Beta-Compliance", new BetaAgent().getAgentName());
  }
}
