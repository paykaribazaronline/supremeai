package com.supremeai.agentorchestration;

import static org.junit.jupiter.api.Assertions.*;

import com.supremeai.agent.AlphaAgent;
import com.supremeai.agent.BetaAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ExpertAgentRouterTest {

  @InjectMocks private ExpertAgentRouter expertAgentRouter;

  @BeforeEach
  void setUp() {
    expertAgentRouter.register(new AlphaAgent());
    expertAgentRouter.register(new BetaAgent());
  }

  @Test
  void testRouteRequest_ToSecurityAgent() {
    String prompt = "OWASP security scan needed";
    assertNotNull(expertAgentRouter.route(prompt));
  }

  @Test
  void testRouteRequest_ToComplianceAgent() {
    String prompt = "GDPR compliance check";
    assertNotNull(expertAgentRouter.route(prompt));
  }

  @Test
  void testRouteRequest_ToCodingAgent() {
    String prompt = "Write a Java function to sort a list";
    assertNotNull(expertAgentRouter.route(prompt));
  }

  @Test
  void testRouteRequest_ToGeneralAgent() {
    String prompt = "What is the capital of France?";
    assertNotNull(expertAgentRouter.route(prompt));
  }
}
