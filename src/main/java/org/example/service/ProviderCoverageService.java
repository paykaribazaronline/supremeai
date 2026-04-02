package org.example.service;

import org.example.kimik2.learning.AgentPatternProfiler;
import org.example.kimik2.learning.KnowledgeSeedService;
import org.example.kimik2.learning.ReasoningChainCopier;
import org.example.model.APIProvider;
import org.example.model.SystemLearning;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
public class ProviderCoverageService {

    @Autowired
    private AIAPIService aiApiService;

    @Autowired
    private SystemLearningService learningService;

    @Autowired
    private AgentPatternProfiler patternProfiler;

    @Autowired
    private ReasoningChainCopier chainCopier;

    @Autowired
    private KnowledgeSeedService knowledgeSeedService;

    @Autowired(required = false)
    private ProviderRegistryService providerRegistryService;

    public Map<String, Object> getCoverageSummary() {
        List<Map<String, Object>> providers = getProviderCoverage();

        int nativeReady = 0;
        int configured = 0;
        int level1Ready = 0;
        int level2Ready = 0;
        int level3Ready = 0;
        int level4Ready = 0;

        for (Map<String, Object> provider : providers) {
            if (Boolean.TRUE.equals(provider.get("nativeConnector"))) nativeReady++;
            if (Boolean.TRUE.equals(provider.get("configured"))) configured++;
            if (Boolean.TRUE.equals(provider.get("level1Ready"))) level1Ready++;
            if (Boolean.TRUE.equals(provider.get("level2Ready"))) level2Ready++;
            if (Boolean.TRUE.equals(provider.get("level3Ready"))) level3Ready++;
            if (Boolean.TRUE.equals(provider.get("level4Ready"))) level4Ready++;
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalProviders", providers.size());
        summary.put("nativeConnectors", nativeReady);
        summary.put("configuredProviders", configured);
        summary.put("level1Ready", level1Ready);
        summary.put("level2Ready", level2Ready);
        summary.put("level3Ready", level3Ready);
        summary.put("level4Ready", level4Ready);
        summary.put("providers", providers);
        summary.put("timestamp", System.currentTimeMillis());
        return summary;
    }

    public List<Map<String, Object>> getProviderCoverage() {
        List<Map<String, Object>> coverage = new ArrayList<>();
        for (String providerId : getProviderUniverse()) {
            coverage.add(buildCoverageRow(providerId));
        }
        return coverage;
    }

    private Map<String, Object> buildCoverageRow(String providerId) {
        Map<String, Object> row = new LinkedHashMap<>();
        Map<String, Object> connector = aiApiService.getProviderConnectorStatus(providerId);
        AgentPatternProfiler.AgentProfile profile = patternProfiler.getProfile(providerId);
        List<SystemLearning> modelMemories = learningService.getMemoriesByAIModel(providerId);
        Map<String, Object> providerKnowledge = knowledgeSeedService.getProviderKnowledge(providerId);
        int chainCount = chainCopier.getChainCountForAgent(providerId);
        APIProvider registryProvider = providerRegistryService == null ? null : providerRegistryService.getProvider(providerId);

        boolean level1Ready = providerKnowledge != null
            && !((Collection<?>) providerKnowledge.getOrDefault("best_tasks", List.of())).isEmpty();
        boolean level2Ready = profile != null;
        boolean level3Ready = chainCount > 0;
        boolean level4Ready = Boolean.TRUE.equals(connector.get("nativeConnector"));
        boolean registryConfigured = registryProvider != null
            && registryProvider.getApiKey() != null
            && !registryProvider.getApiKey().isBlank();

        row.put("providerId", providerId);
        row.put("displayName", aiApiService.getProviderDisplayName(providerId));
        row.put("canonicalModel", connector.get("canonicalModel"));
        row.put("nativeConnector", connector.get("nativeConnector"));
        row.put("configured", Boolean.TRUE.equals(connector.get("configured")) || registryConfigured);
        row.put("endpoint", connector.get("endpoint"));
        row.put("defaultModel", connector.get("defaultModel"));
        row.put("fallbackChain", connector.get("fallbackChain"));
        row.put("registryActive", registryProvider != null && (registryProvider.getStatus() == null || !registryProvider.getStatus().equalsIgnoreCase("inactive")));
        row.put("level1Ready", level1Ready);
        row.put("level2Ready", level2Ready);
        row.put("level3Ready", level3Ready);
        row.put("level4Ready", level4Ready);
        row.put("profilePatterns", profile == null ? 0 : profile.topReasoningPatterns.size());
        row.put("profileConfidence", profile == null ? 0.0 : profile.overallConfidence);
        row.put("profileDecisions", profile == null ? 0 : profile.totalDecisions);
        row.put("chainCount", chainCount);
        row.put("memoryCount", modelMemories.size());
        row.put("bestTasks", providerKnowledge.getOrDefault("best_tasks", List.of()));
        row.put("strengths", providerKnowledge.getOrDefault("strengths", List.of()));
        row.put("coverageScore", calculateCoverageScore(level1Ready, level2Ready, level3Ready, level4Ready));
        return row;
    }

    private int calculateCoverageScore(boolean level1Ready, boolean level2Ready, boolean level3Ready, boolean level4Ready) {
        int score = 0;
        if (level1Ready) score++;
        if (level2Ready) score++;
        if (level3Ready) score++;
        if (level4Ready) score++;
        return score;
    }

    private List<String> getProviderUniverse() {
        LinkedHashSet<String> providerIds = new LinkedHashSet<>(aiApiService.getCanonicalProviderIds());
        providerIds.addAll(knowledgeSeedService.getAllProviders());
        if (providerRegistryService != null) {
            for (APIProvider provider : providerRegistryService.getAllProviders()) {
                providerIds.add(provider.getId());
            }
        }
        return new ArrayList<>(providerIds);
    }
}