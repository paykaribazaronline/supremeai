package com.supremeai.agentorchestration;

import com.supremeai.agent.AgentCapability;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ExpertAgentRouter {

  private static final Logger logger = LoggerFactory.getLogger(ExpertAgentRouter.class);
  private final Map<String, AgentCapability> agentsById = new LinkedHashMap<>();
  private final Map<String, List<AgentCapability>> agentsByKeyword = new LinkedHashMap<>();

  public void register(AgentCapability agent) {
    agentsById.put(agent.getAgentId(), agent);
    for (String keyword : agent.getTriggerKeywords()) {
      agentsByKeyword.computeIfAbsent(keyword.toLowerCase(), k -> new ArrayList<>()).add(agent);
    }
    logger.info("Registered agent: {} ({})", agent.getAgentName(), agent.getAgentId());
  }

  public AgentCapability route(String prompt) {
    if (prompt == null || prompt.isBlank()) {
      return agentsById.get("GENERAL");
    }
    String[] tokens = prompt.toLowerCase().split("[^a-z0-9]+");
    for (String token : tokens) {
      List<AgentCapability> hits = agentsByKeyword.get(token);
      if (hits != null && !hits.isEmpty()) {
        return hits.get(0);
      }
    }
    return agentsById.get("GENERAL");
  }

  public Map<String, AgentCapability> getAllAgents() {
    return new LinkedHashMap<>(agentsById);
  }
}
