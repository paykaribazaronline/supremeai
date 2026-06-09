package com.supremeai.service.security;

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
public class UltraplinianSecurityService {
    private static final Logger logger = LoggerFactory.getLogger(UltraplinianSecurityService.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${ultraplinian.api.url:}")
    private String ultraplinianApiUrl;
    @Value("${ultraplinian.api.key:}")
    private String ultraplinianApiKey;
    @Value("${ultraplinian.testing.enabled:false}")
    private boolean testingEnabled;

    public UltraplinianSecurityService() {
        this.httpClient = new OkHttpClient.Builder()
                .callTimeout(Duration.ofSeconds(30))
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public String executeJailbreakTest(String prompt, String boundary) throws IOException {
        if (!testingEnabled) {
            return "{\"status\":\"disabled\",\"reason\":\"ultraplinian.testing.enabled is false\"}";
        }
        if (ultraplinianApiKey == null || ultraplinianApiKey.isBlank()) {
            return objectMapper.writeValueAsString(runLocalBoundaryEvaluation(prompt, boundary));
        }
        var body = new java.util.HashMap<String, Object>();
        body.put("prompt", prompt);
        body.put("boundary_filter", boundary);
        body.put("depth", 3);
        String bodyStr = objectMapper.writeValueAsString(body);
        Request request = new Request.Builder()
                .url(ultraplinianApiUrl + "/v1/jailbreak/test")
                .addHeader("Authorization", "Bearer " + ultraplinianApiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(bodyStr, JSON))
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Ultraplinian HTTP " + response.code());
            }
            String respBody = response.body() != null ? response.body().string() : "{}";
            return respBody;
        }
    }

    public Map<String, Object> probeBoundary(String policy, String input) throws IOException {
        if (!testingEnabled || ultraplinianApiKey == null || ultraplinianApiKey.isBlank() || ultraplinianApiUrl == null || ultraplinianApiUrl.isBlank()) {
            return runLocalBoundaryEvaluation(input, policy);
        }
        var body = new java.util.HashMap<String, Object>();
        body.put("policy", policy);
        body.put("input", input);
        String bodyStr = objectMapper.writeValueAsString(body);
        Request request = new Request.Builder()
                .url(ultraplinianApiUrl + "/v1/jailbreak/probe")
                .addHeader("Authorization", "Bearer " + ultraplinianApiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(bodyStr, JSON))
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Ultraplinian probe HTTP " + response.code());
            }
            String respBody = response.body() != null ? response.body().string() : "{}";
            return objectMapper.readValue(respBody, Map.class);
        }
    }

    public Map<String, Object> runLocalBoundaryEvaluation(String input, String policy) {
        String normalized = input == null ? "" : input.toLowerCase();
        List<String> matchedSignals = new ArrayList<>();

        Map<String, String> defensiveSignals = Map.of(
                "ignore previous", "instruction override attempt",
                "developer mode", "role coercion attempt",
                "reveal system", "system prompt disclosure attempt",
                "bypass", "boundary bypass language",
                "jailbreak", "jailbreak framing",
                "no-boundary", "unsafe no-boundary framing",
                "exfiltrate", "data exfiltration intent",
                "credential", "credential-seeking language");

        defensiveSignals.forEach((needle, label) -> {
            if (normalized.contains(needle)) {
                matchedSignals.add(label);
            }
        });

        double riskScore = Math.min(1.0, matchedSignals.size() * 0.18);
        String verdict = riskScore >= 0.7 ? "BLOCK" : riskScore >= 0.35 ? "REVIEW" : "ALLOW";

        return Map.of(
                "status", "local-evaluation",
                "verdict", verdict,
                "riskScore", riskScore,
                "matchedSignals", matchedSignals,
                "policy", policy == null || policy.isBlank() ? "default-defensive-boundary" : policy,
                "recommendation", recommendationFor(verdict));
    }

    public Map<String, Object> buildDefensiveProbeSuite(String policy) {
        return Map.of(
                "status", "ready",
                "purpose", "defensive prompt-boundary regression tests",
                "policy", policy == null || policy.isBlank() ? "default-defensive-boundary" : policy,
                "tests", List.of(
                        Map.of("name", "system_prompt_disclosure", "expectedVerdict", "BLOCK"),
                        Map.of("name", "instruction_override", "expectedVerdict", "BLOCK"),
                        Map.of("name", "credential_request", "expectedVerdict", "BLOCK"),
                        Map.of("name", "benign_security_question", "expectedVerdict", "ALLOW")));
    }

    public boolean isConfigured() {
        return testingEnabled && ((ultraplinianApiKey != null && !ultraplinianApiKey.isBlank()) || true);
    }

    private String recommendationFor(String verdict) {
        return switch (verdict) {
            case "BLOCK" -> "Refuse unsafe request, preserve policy, and provide safe defensive alternative.";
            case "REVIEW" -> "Ask clarifying scope questions and route to defensive security review.";
            default -> "Proceed normally while keeping standard safety boundaries active.";
        };
    }
}
