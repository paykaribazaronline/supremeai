package com.supremeai.service.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ProximaAgentService {
    private static final Logger logger = LoggerFactory.getLogger(ProximaAgentService.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${proxima.api.url:}")
    private String proximaApiUrl;
    @Value("${proxima.api.key:}")
    private String proximaApiKey;

    public ProximaAgentService() {
        this.httpClient = new OkHttpClient.Builder()
                .callTimeout(Duration.ofSeconds(30))
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public String launchAgent(String task, String context) throws IOException {
        if (proximaApiKey == null || proximaApiKey.isBlank()) {
            return objectMapper.writeValueAsString(createLocalTaskPlan(task, context));
        }
        var body = new java.util.HashMap<String, Object>();
        body.put("task", task);
        body.put("context", context);
        body.put("mode", "autonomous");
        String bodyStr = objectMapper.writeValueAsString(body);
        Request request = new Request.Builder()
                .url(proximaApiUrl + "/v1/agent/launch")
                .addHeader("Authorization", "Bearer " + proximaApiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(bodyStr, JSON))
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Proxima HTTP " + response.code());
            }
            return response.body() != null ? response.body().string() : "{\"status\":\"ok\"}";
        }
    }

    public boolean isConfigured() {
        return (proximaApiKey != null && !proximaApiKey.isBlank())
                || (proximaApiUrl != null && !proximaApiUrl.isBlank());
    }

    public Map<String, Object> createLocalTaskPlan(String task, String context) {
        String normalized = task == null ? "" : task.toLowerCase();
        List<Map<String, Object>> steps = new ArrayList<>();

        steps.add(step("understand_scope", "SupremeAIBrain", "Clarify objective, constraints, and success criteria."));

        if (containsAny(normalized, "code", "implement", "build", "fix", "refactor")) {
            steps.add(step("decompose_code_work", "CodeGenerationAgent", "Split implementation into minimal ordered subtasks."));
            steps.add(step("validate_changes", "SimulatorAgent", "Run compile/tests/simulation for changed behavior."));
        }

        if (containsAny(normalized, "security", "audit", "jailbreak", "boundary", "pentest")) {
            steps.add(step("defensive_security_review", "SecurityAuditAgent", "Run non-destructive policy and vulnerability checks."));
        }

        if (containsAny(normalized, "research", "web", "latest", "docs")) {
            steps.add(step("research_context", "ResearchAgent", "Gather and summarize trusted external context."));
        }

        steps.add(step("consensus_decision", "MultiAIVotingService", "Ask eligible providers to vote on the best final path."));
        steps.add(step("finalize", "SupremeAIBrain", "Synthesize final response and next actions."));

        return Map.of(
                "status", "local-plan",
                "mode", "proxima-compatible",
                "task", task == null ? "" : task,
                "context", context == null ? "" : context,
                "steps", steps,
                "stepCount", steps.size());
    }

    public Map<String, Object> defineLogicChain(String task, List<String> agentNames) {
        List<Map<String, Object>> chain = new ArrayList<>();
        List<String> agents = agentNames == null || agentNames.isEmpty()
                ? List.of("SupremeAIBrain", "MultiAIVotingService", "CodeGenerationAgent", "SimulatorAgent")
                : agentNames;
        for (int i = 0; i < agents.size(); i++) {
            chain.add(Map.of(
                    "order", i + 1,
                    "agent", agents.get(i),
                    "handoff", i == agents.size() - 1 ? "complete" : agents.get(i + 1)));
        }
        return Map.of("task", task == null ? "" : task, "chain", chain);
    }

    private Map<String, Object> step(String id, String agent, String objective) {
        return Map.of("id", id, "agent", agent, "objective", objective, "status", "pending");
    }

    private boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
