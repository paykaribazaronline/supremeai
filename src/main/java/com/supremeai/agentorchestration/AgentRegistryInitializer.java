package com.supremeai.agentorchestration;

import com.supremeai.agent.AgentCapability;
import com.supremeai.agent.AlphaAgent;
import com.supremeai.agent.BetaAgent;
import com.supremeai.agent.DeltaAgent;
import com.supremeai.agent.EpsilonAgent;
import com.supremeai.agent.EtaAgent;
import com.supremeai.agent.GammaAgent;
import com.supremeai.agent.IotaAgent;
import com.supremeai.agent.KappaAgent;
import com.supremeai.agent.ThetaAgent;
import com.supremeai.agent.ZetaAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AgentRegistryInitializer implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(AgentRegistryInitializer.class);
  private final ExpertAgentRouter router;

  public AgentRegistryInitializer(ExpertAgentRouter router) {
    this.router = router;
  }

  @Override
  public void run(ApplicationArguments args) {
    for (AgentCapability agent :
        new AgentCapability[] {
          new AlphaAgent(),
          new BetaAgent(),
          new GammaAgent(),
          new DeltaAgent(),
          new EpsilonAgent(),
          new ZetaAgent(),
          new EtaAgent(),
          new ThetaAgent(),
          new IotaAgent(),
          new KappaAgent()
        }) {
      router.register(agent);
    }
    log.info("All {} agents registered", router.getAllAgents().size());
  }
}
