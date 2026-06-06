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
class ZetaAgentTest {
  @Test
  void process_returnsAgentResponse() {
    ZetaAgent agent = new ZetaAgent();
    Map<String, Object> ctx = new HashMap<>();
    StepVerifier.create(agent.process("budget", ctx))
        .assertNext(r -> assertNotNull(r))
        .verifyComplete();
  }

  @Test
  void getAgentId_returnsZETA() {
    assertEquals("ZETA", new ZetaAgent().getAgentId());
  }
}
