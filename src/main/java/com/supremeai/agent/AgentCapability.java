package com.supremeai.agent;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import reactor.core.publisher.Mono;

public interface AgentCapability {
  String getAgentId();

  String getAgentName();

  List<String> getTriggerKeywords();

  default List<String> defaultKeywords() {
    return Collections.singletonList(getAgentId().toLowerCase());
  }

  Mono<String> process(String task, Map<String, Object> context);
}
