package com.supremeai.agentorchestration;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * AI-05: Cross-Agent Vector Memory Shared context store between Code Agent, Security Agent, and
 * Deploy Agent. Eliminates redundant context rebuilding across multi-agent workflows.
 */
@Service
public class CrossAgentVectorMemory {

  // Simple in-memory representation of vector store
  // Map of SessionId -> List of MemoryEntries
  private final Map<String, List<MemoryEntry>> memoryStore = new ConcurrentHashMap<>();

  public void storeContext(
      String sessionId, String sourceAgent, String contextData, String taskType) {
    memoryStore
        .computeIfAbsent(sessionId, k -> Collections.synchronizedList(new ArrayList<>()))
        .add(new MemoryEntry(sourceAgent, contextData, taskType, System.currentTimeMillis()));
  }

  public String retrieveRelevantContext(String sessionId, String targetAgent, String currentTask) {
    List<MemoryEntry> entries = memoryStore.getOrDefault(sessionId, Collections.emptyList());
    if (entries.isEmpty()) {
      return "";
    }

    StringBuilder sharedContext = new StringBuilder();
    sharedContext.append("--- Shared Agent Memory ---\n");

    for (MemoryEntry entry : entries) {
      // In a real vector DB, we would do a cosine similarity search here.
      // For now, we return recent context from other agents to avoid re-prompting.
      if (!entry.sourceAgent.equals(targetAgent)) {
        sharedContext
            .append("From ")
            .append(entry.sourceAgent)
            .append(" (Task: ")
            .append(entry.taskType)
            .append("):\n")
            .append(entry.contextData)
            .append("\n\n");
      }
    }

    return sharedContext.toString();
  }

  public void clearMemory(String sessionId) {
    memoryStore.remove(sessionId);
  }

  private static class MemoryEntry {
    String sourceAgent;
    String contextData;
    String taskType;
    long timestamp;

    public MemoryEntry(String sourceAgent, String contextData, String taskType, long timestamp) {
      this.sourceAgent = sourceAgent;
      this.contextData = contextData;
      this.taskType = taskType;
      this.timestamp = timestamp;
    }
  }
}
