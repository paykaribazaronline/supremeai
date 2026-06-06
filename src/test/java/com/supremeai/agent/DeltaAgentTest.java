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
class DeltaAgentTest {
  @Test
  void process_returnsAgentResponse() {
    DeltaAgent agent = new DeltaAgent();
    Map<String, Object> ctx = new HashMap<>();
    StepVerifier.create(agent.process("cost tracking", ctx))
        .assertNext(r -> assertNotNull(r))
        .verifyComplete();
  }

  @Test
  void getAgentId_returnsDELTA() {
    assertEquals("DELTA", new DeltaAgent().getAgentId());
  }

  @Test
  void getAgentName_returnsCost() {
    assertEquals("Delta-Cost", new DeltaAgent().getAgentName());
  }
}
