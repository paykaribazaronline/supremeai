package com.supremeai.agent;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class AlphaAgentTest {
  @Test
  void process_returnsAgentResponse() {
    AlphaAgent agent = new AlphaAgent();
    Map<String, Object> ctx = new HashMap<>();
    StepVerifier.create(agent.process("owasp scan", ctx))
        .assertNext(
            r -> {
              assertNotNull(r);
              assertTrue(r.contains("AlphaAgent"));
            })
        .verifyComplete();
  }

  @Test
  void getAgentId_returnsALPHA() {
    assertEquals("ALPHA", new AlphaAgent().getAgentId());
  }

  @Test
  void getAgentName_returnsSecurity() {
    assertEquals("Alpha-Security", new AlphaAgent().getAgentName());
  }

  @Test
  void scanCode_detectsSQLInjection() {
    AlphaAgent agent = new AlphaAgent();
    List<AlphaAgent.VulnerabilityReport> findings =
        agent.scanCodeForVulnerabilities("SELECT * FROM users WHERE id = " + "123");
    assertFalse(findings.isEmpty());
  }
}
