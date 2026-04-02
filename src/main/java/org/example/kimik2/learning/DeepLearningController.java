package org.example.kimik2.learning;

import org.example.service.AIAPIService;
import org.example.service.QuotaService;
import org.example.service.SystemLearningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST API for Levels 2, 3, 4 of SupremeAI's deep learning system.
 *
 * Endpoints:
 *   GET  /api/kimik2/learning/profiles          — Level 2: all agent profiles
 *   GET  /api/kimik2/learning/profiles/{agent}  — Level 2: single agent profile
 *   POST /api/kimik2/learning/profiles/rebuild  — Level 2: rebuild all profiles NOW
 *   GET  /api/kimik2/learning/chains/stats      — Level 3: chain store stats
 *   GET  /api/kimik2/learning/chains/{agent}    — Level 3: chains for one agent
 *   POST /api/kimik2/learning/chains/index      — Level 3: re-index all chains NOW
 *   POST /api/kimik2/learning/generate          — Level 4: generate reasoning for task
 *   GET  /api/kimik2/learning/generate/stats    — Level 4: generator stats
 *   GET  /api/kimik2/learning/status            — All 3 levels combined status
 */
@RestController
@RequestMapping("/api/kimik2/learning")
public class DeepLearningController {

    @Autowired
    private AgentPatternProfiler profiler;

    @Autowired
    private ReasoningChainCopier chainCopier;

    @Autowired
    private ReasoningGenerator generator;

    @Autowired
    private KnowledgeSeedService knowledgeSeedService;

    @Autowired
    private AIAPIService aiAPIService;

    @Autowired
    private QuotaService quotaService;

    @Autowired
    private SystemLearningService learningService;

    // ── Level 2: Agent Pattern Profiles ──────────────────────────────────────

    @GetMapping("/profiles")
    public ResponseEntity<Map<String, Object>> getAllProfiles() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("level", 2);
        response.put("description", "Agent reasoning pattern profiles");
        response.put("profiles", profiler.getAllProfileSummaries());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profiles/{agentName}")
    public ResponseEntity<Map<String, Object>> getProfile(@PathVariable String agentName) {
        AgentPatternProfiler.AgentProfile profile = profiler.getProfile(agentName);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("level", 2);
        response.put("profile", profile.toMap());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/profiles/rebuild")
    public ResponseEntity<Map<String, Object>> rebuildProfiles() {
        Map<String, AgentPatternProfiler.AgentProfile> built = profiler.buildAllProfiles();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("level", 2);
        response.put("profiles_built", built.size());
        response.put("agents", new ArrayList<>(built.keySet()));
        return ResponseEntity.ok(response);
    }

    // ── Level 3: Reasoning Chains ─────────────────────────────────────────────

    @GetMapping("/chains/stats")
    public ResponseEntity<Map<String, Object>> chainStats() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("level", 3);
        response.put("description", "Reasoning chain store statistics");
        response.putAll(chainCopier.getStats());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/chains/{agentName}")
    public ResponseEntity<Map<String, Object>> getChainsForAgent(
            @PathVariable String agentName) {
        List<Map<String, Object>> chains = chainCopier.getChainsForAgent(agentName)
            .stream()
            .map(ReasoningChainCopier.ReasoningChain::toMap)
            .collect(java.util.stream.Collectors.toList());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("level", 3);
        response.put("agent", agentName);
        response.put("chain_count", chains.size());
        response.put("chains", chains);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/chains/index")
    public ResponseEntity<Map<String, Object>> reindexChains() {
        int count = chainCopier.indexAllChains();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("level", 3);
        response.put("chains_indexed", count);
        response.putAll(chainCopier.getStats());
        return ResponseEntity.ok(response);
    }

    // ── Level 4: Reasoning Generator ─────────────────────────────────────────

    /**
     * Generate reasoning for a new task.
     *
     * Request body:
     * {
     *   "agentName": "B-Fixer",
     *   "taskType":  "BUG_FIX",
     *   "context":   "AuthenticationFilterTest is failing with null pointer"
     * }
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generate(
            @RequestBody Map<String, String> body) {

        String agentName = body.get("agentName");
        String taskType  = body.get("taskType");
        String context   = body.get("context");

        if (agentName == null || taskType == null || context == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "agentName, taskType, context are required"));
        }

        ReasoningGenerator.GeneratedReasoning result =
            generator.generateReasoning(agentName, taskType, context);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("level", 4);
        response.put("description", "Few-shot reasoning generated from Levels 2+3");
        response.putAll(result.toMap());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/generate/stats")
    public ResponseEntity<Map<String, Object>> generatorStats() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("level", 4);
        response.putAll(generator.getStats());
        return ResponseEntity.ok(response);
    }

    // ── Combined status ───────────────────────────────────────────────────────

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("learning_levels_implemented", Arrays.asList(
            "Level 1: RLVR (who wins) — KimiK2 core package",
            "Level 2: Pattern Profiler (how they reason) — AgentPatternProfiler",
            "Level 3: Chain Copier (copy best reasoning) — ReasoningChainCopier",
            "Level 4: Reasoning Generator (generate new reasoning) — ReasoningGenerator"
        ));
        response.put("level2_profiles", profiler.getAllProfileSummaries().size());
        response.put("level3_chains", chainCopier.getStats());
        response.put("level4_cache", generator.getStats());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/providers/status")
    public ResponseEntity<Map<String, Object>> providerStatus() {
        List<Map<String, Object>> providers = new ArrayList<>();
        int allLevelsReady = 0;
        int nativeConfigured = 0;

        for (String provider : knowledgeSeedService.getAllProviders()) {
            Map<String, Object> connector = aiAPIService.getProviderConnectorStatus(provider);
            AgentPatternProfiler.AgentProfile profile = profiler.getProfile(provider);
            int chainCount = chainCopier.getChainCountForAgent(provider);
            int generatedCount = generator.getGeneratedReasoningCount(provider);
            int memoryCount = learningService.getMemoriesByAIModel(provider).size();
            boolean level1 = true;
            boolean level2 = profile != null && profile.agentName != null;
            boolean level3 = chainCount > 0;
            boolean level4 = Boolean.TRUE.equals(connector.get("nativeConnector"))
                && Boolean.TRUE.equals(connector.get("configured"));
            int readyLevels = (level1 ? 1 : 0) + (level2 ? 1 : 0) + (level3 ? 1 : 0) + (level4 ? 1 : 0);
            if (readyLevels == 4) {
                allLevelsReady++;
            }
            if (level4) {
                nativeConfigured++;
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("provider", provider);
            item.put("canonicalModel", connector.get("canonicalModel"));
            item.put("endpoint", connector.get("endpoint"));
            item.put("fallbackChain", connector.get("fallbackChain"));
            item.put("quotaHealthy", quotaService.canUseAI(provider));
            item.put("memories", memoryCount);
            item.put("profileDecisionCount", profile == null ? 0 : profile.totalDecisions);
            item.put("chainCount", chainCount);
            item.put("generatedReasonings", generatedCount);
            item.put("nativeConnector", connector.get("nativeConnector"));
            item.put("configured", connector.get("configured"));
            item.put("levels", Map.of(
                "level1", level1,
                "level2", level2,
                "level3", level3,
                "level4", level4
            ));
            item.put("coveragePercent", readyLevels * 25);
            providers.add(item);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("providers", providers);
        response.put("summary", Map.of(
            "totalProviders", providers.size(),
            "allLevelsReady", allLevelsReady,
            "nativeConfigured", nativeConfigured,
            "quotaHealthy", providers.stream().filter(p -> Boolean.TRUE.equals(p.get("quotaHealthy"))).count(),
            "generatedReasonings", providers.stream().mapToInt(p -> ((Number) p.get("generatedReasonings")).intValue()).sum()
        ));
        return ResponseEntity.ok(response);
    }
}
