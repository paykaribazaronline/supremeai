package com.supremeai.agent;

import com.supremeai.agentorchestration.*;
import com.supremeai.service.MultiAIVotingService;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class EtaAgent implements AgentCapability {
  private static final Logger log = LoggerFactory.getLogger(EtaAgent.class);

  private final Map<String, ConsensusResult> consensusHistory = new ConcurrentHashMap<>();

  @Autowired private AgentRuleService ruleService;

  @Autowired private MultiAIVotingService votingService;

  @Override
  public String getAgentId() {
    return "ETA";
  }

  @Override
  public String getAgentName() {
    return "Eta-Meta-Consensus";
  }

  @Override
  public List<String> getTriggerKeywords() {
    return Arrays.asList(
        "consensus", "meta", "agreement", "disagreement", "voting", "ensemble", "merge");
  }

  @Override
  public Mono<String> process(String task, Map<String, Object> context) {
    log.info("[EtaAgent] Performing meta-consensus analysis for task: {}", task);

    return Mono.fromCallable(
            () -> {
              @SuppressWarnings("unchecked")
              List<String> agentResponses =
                  (List<String>) context.getOrDefault("agentResponses", new ArrayList<>());
              @SuppressWarnings("unchecked")
              Map<String, Double> confidenceScores =
                  (Map<String, Double>) context.getOrDefault("confidenceScores", Map.of());
              String taskId = (String) context.getOrDefault("taskId", "unknown");

              if (agentResponses.isEmpty()) {
                return "[EtaAgent] No agent responses provided for consensus";
              }

              ConsensusResult result =
                  performMetaConsensus(taskId, agentResponses, confidenceScores);
              consensusHistory.put(taskId, result);

              return generateConsensusReport(task, result);
            })
        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
  }

  public ConsensusResult performMetaConsensus(
      String taskId, List<String> responses, Map<String, Double> scores) {
    Map<String, Integer> similarityMap = new HashMap<>();
    Map<String, Double> weightedScores = new HashMap<>();

    for (int i = 0; i < responses.size(); i++) {
      String response = responses.get(i);
      double weight = scores.getOrDefault("agent" + i, 0.5);

      String key = generateSentimentKey(response);
      similarityMap.merge(key, 1, Integer::sum);
      weightedScores.merge(key, weight, Double::sum);
    }

    String majority =
        similarityMap.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("neutral");

    double agreementScore =
        (double) similarityMap.values().stream().mapToInt(Integer::intValue).max().orElse(0)
            / responses.size();
    double avgConfidence =
        weightedScores.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.5);

    List<String> dissenting =
        responses.stream()
            .filter(r -> !generateSentimentKey(r).equals(majority))
            .collect(Collectors.toList());

    double disagreementPct = (double) dissenting.size() / responses.size();

    return new ConsensusResult(
        majority, agreementScore, avgConfidence, responses.size(), dissenting);
  }

  private String generateSentimentKey(String response) {
    String lower = response.toLowerCase();
    if (lower.contains("error") || lower.contains("fail") || lower.contains("cannot")) {
      return "negative";
    } else if (lower.contains("success") || lower.contains("complete") || lower.contains("valid")) {
      return "positive";
    }
    return "neutral";
  }

  private String generateConsensusReport(String task, ConsensusResult result) {
    StringBuilder report = new StringBuilder();
    report.append("[EtaAgent] Meta-Consensus Analysis:\n\n");
    report.append("Task: ").append(task).append("\n");
    report.append("Agents Responded: ").append(result.agentCount()).append("\n");
    report.append("Majority Position: ").append(result.majority()).append("\n");
    report
        .append("Agreement Score: ")
        .append(String.format("%.1f%%", result.agreementScore() * 100))
        .append("\n");
    report
        .append("Average Confidence: ")
        .append(String.format("%.2f", result.avgConfidence()))
        .append("\n\n");

    if (result.dissenting().size() > result.agentCount() * 0.3) {
      report
          .append("⚠️ Significant disagreement detected (")
          .append(result.dissenting().size())
          .append(" dissenting)\n");
      report.append("Recommendation: Consider additional review or human intervention\n\n");
    }

    report
        .append("Consensus Quality: ")
        .append(
            result.agreementScore() > 0.8
                ? "High"
                : result.agreementScore() > 0.5 ? "Medium" : "Low")
        .append("\n");

    return report.toString();
  }

  public ConsensusResult getPreviousConsensus(String taskId) {
    return consensusHistory.get(taskId);
  }

  public List<ConsensusResult> getRecentConsents(int limit) {
    return consensusHistory.values().stream().limit(limit).collect(Collectors.toList());
  }

  public record ConsensusResult(
      String majority,
      double agreementScore,
      double avgConfidence,
      int agentCount,
      List<String> dissenting) {}
}
