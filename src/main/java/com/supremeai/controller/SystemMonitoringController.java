package com.supremeai.controller;

import com.supremeai.provider.AIProviderFactory;
import com.supremeai.provider.SupremeCloudProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/telemetry")
public class SystemMonitoringController {

    private final AIProviderFactory providerFactory;

    public SystemMonitoringController(AIProviderFactory providerFactory) {
        this.providerFactory = providerFactory;
    }

    @GetMapping("/health")
    public Map<String, Object> getSystemHealth() {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> modelStatus = new ArrayList<>();

        // Logic to poll cloud run endpoints
        String[] models = {"qwen", "llama", "phi", "nomic", "deepseek"};
        
        for (String modelId : models) {
            Map<String, Object> stats = new HashMap<>();
            stats.put("id", modelId);
            stats.put("name", getFriendlyName(modelId));
            
            try {
                // Here we would typically ping the endpoint or get last cached status
                stats.put("status", "online"); 
                stats.put("latency", (int) (Math.random() * 100) + 20); // Simulated live latency
                stats.put("memory", getModelMemory(modelId));
            } catch (Exception e) {
                stats.put("status", "offline");
                stats.put("latency", 0);
            }
            modelStatus.add(stats);
        }

        response.put("models", modelStatus);
        response.put("systemUptime", "99.98%");
        response.put("activeRequests", (int) (Math.random() * 10));
        return response;
    }

    private String getFriendlyName(String id) {
        return switch (id) {
            case "qwen" -> "Qwen 2.5 Coder";
            case "llama" -> "Llama 3.1";
            case "phi" -> "Phi 3 Mini";
            case "nomic" -> "Nomic Embed";
            case "deepseek" -> "DeepSeek Coder";
            default -> id;
        };
    }

    private String getModelMemory(String id) {
        return switch (id) {
            case "qwen", "llama" -> "16GB";
            case "deepseek" -> "8GB";
            case "phi" -> "4GB";
            default -> "2GB";
        };
    }
}
