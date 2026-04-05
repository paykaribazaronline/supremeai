package org.example.controller;

import org.example.model.Quota;
import org.example.service.AlertingService;
import org.example.service.AIAPIService;
import org.example.service.QuotaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/ops")
public class AIOperationsController {

    @Autowired
    private AIAPIService aiApiService;

    @Autowired(required = false)
    private QuotaService quotaService;

    @Autowired(required = false)
    private AlertingService alertingService;

    @GetMapping("/metrics")
    public Map<String, Object> getAiOperationalMetrics() {
        return aiApiService.getOperationalMetrics();
    }

    @GetMapping("/summary")
    public Map<String, Object> getAiSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("ops", aiApiService.getOperationalMetrics());
        summary.put("quota", quotaService != null ? quotaService.getQuotaSummary() : Map.of("status", "UNAVAILABLE"));
        summary.put("alerts", alertingService != null ? alertingService.getAlertStats() : Map.of("status", "UNAVAILABLE"));
        summary.put("timestamp", System.currentTimeMillis());
        return summary;
    }

    @GetMapping("/cost-estimate")
    public Map<String, Object> getProviderCostEstimate(
        @RequestParam(defaultValue = "0.25") double defaultPricePer1kTokensUsd
    ) {
        Map<String, Object> response = new LinkedHashMap<>();

        if (quotaService == null) {
            response.put("status", "UNAVAILABLE");
            response.put("message", "QuotaService unavailable");
            return response;
        }

        Map<String, Quota> quotas = quotaService.getAllQuotas();
        Map<String, Double> providerPrice = Map.ofEntries(
            Map.entry("openai", 2.50),
            Map.entry("gpt", 2.50),
            Map.entry("claude", 1.80),
            Map.entry("anthropic", 1.80),
            Map.entry("gemini", 0.35),
            Map.entry("google", 0.35),
            Map.entry("groq", 0.15),
            Map.entry("deepseek", 0.14),
            Map.entry("mistral", 0.30),
            Map.entry("cohere", 0.50),
            Map.entry("huggingface", 0.25),
            Map.entry("xai", 1.20),
            Map.entry("grok", 1.20),
            Map.entry("llama", 0.40),
            Map.entry("perplexity", 0.60)
        );

        List<Map<String, Object>> byProvider = quotas.values().stream().map(quota -> {
            String providerName = quota.getProviderName() == null ? quota.getProviderId() : quota.getProviderName();
            String normalized = providerName == null ? "" : providerName.toLowerCase();
            double price = providerPrice.entrySet().stream()
                .filter(entry -> normalized.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(defaultPricePer1kTokensUsd);

            double estimatedUsd = round((quota.getTokensUsedThisMonth() / 1000.0) * price);
            return Map.<String, Object>of(
                "providerId", quota.getProviderId(),
                "providerName", providerName,
                "tokensUsedThisMonth", quota.getTokensUsedThisMonth(),
                "requestsUsedThisMonth", quota.getRequestsUsedThisMonth(),
                "pricePer1kTokensUsd", price,
                "estimatedMonthlyCostUsd", estimatedUsd,
                "quotaStatus", quota.getStatus(),
                "usagePercent", round(quota.getUsagePercentage())
            );
        }).toList();

        double totalCost = byProvider.stream()
            .mapToDouble(item -> ((Number) item.get("estimatedMonthlyCostUsd")).doubleValue())
            .sum();

        response.put("status", "OK");
        response.put("date", LocalDate.now().toString());
        response.put("defaultPricePer1kTokensUsd", defaultPricePer1kTokensUsd);
        response.put("estimatedTotalMonthlyCostUsd", round(totalCost));
        response.put("providers", byProvider);
        return response;
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }
}
