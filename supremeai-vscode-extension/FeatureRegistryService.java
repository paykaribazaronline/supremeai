package com.supremeai.service;

import com.supremeai.model.FeatureDefinition;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FeatureRegistryService {
    private final List<FeatureDefinition> registry = new ArrayList<>();

    @PostConstruct
    public void initRegistry() {
        // Register existing features to prevent duplicates
        register(FeatureDefinition.builder()
                .id("tiny-ai-v1")
                .name("Tiny AI Model")
                .category("AI_MODEL")
                .provider("On-Device SuperFly")
                .status("ACTIVE")
                .classPath("com.supremeai.ai.TinyAiEngine")
                .build());

        register(FeatureDefinition.builder()
                .id("godmode-3-browser")
                .name("Godmode 3 Browser")
                .category("BROWSER_ENGINE")
                .provider("Stateful Playwright")
                .status("ACTIVE")
                .classPath("com.supremeai.browser.GodmodeEngine")
                .build());
    }

    public void register(FeatureDefinition feature) {
        // Enhanced logic to prevent actual duplicates in the inventory
        boolean isDuplicate = registry.stream()
                .anyMatch(f -> f.getClassPath().equals(feature.getClassPath()) || 
                               f.getId().equals(feature.getId()));
        
        if (!isDuplicate) {
            registry.add(feature);
        }
    }

    public List<FeatureDefinition> getAllFeatures() {
        return registry;
    }

    public List<FeatureDefinition> findByCategory(String category) {
        return registry.stream().filter(f -> f.getCategory().equalsIgnoreCase(category)).collect(Collectors.toList());
    }
}