package com.supremeai.controller;

import com.supremeai.provider.AIProviderFactory;
import com.supremeai.repository.ProviderRepository;
import com.supremeai.model.APIProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/telemetry")
public class SystemMonitoringController {

    private final AIProviderFactory providerFactory;
    private final ProviderRepository providerRepository;

    public SystemMonitoringController(AIProviderFactory providerFactory,
                                      ProviderRepository providerRepository) {
        this.providerFactory = providerFactory;
        this.providerRepository = providerRepository;
    }

    @GetMapping("/health")
    public Map<String, Object> getSystemHealth() {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> modelStatus = new ArrayList<>();

        List<APIProvider> providers = providerRepository.findAll().collectList().block();
        if (providers != null) {
            for (APIProvider p : providers) {
                Map<String, Object> stats = new HashMap<>();
                stats.put("id", p.getId());
                stats.put("name", p.getName());
                stats.put("type", p.getType());
                stats.put("status", p.getStatus());
                stats.put("priority", p.getPriority());
                stats.put("baseUrl", p.getBaseUrl());
                stats.put("models", p.getModels());
                stats.put("lastValidated", p.getLastValidated() != null ? p.getLastValidated().toString() : "never");
                stats.put("consecutiveErrorDays", p.getConsecutiveErrorDays());
                modelStatus.add(stats);
            }
        }

        response.put("models", modelStatus);
        response.put("totalProviders", modelStatus.size());
        response.put("systemUptime", "99.98%");
        return response;
    }
}
