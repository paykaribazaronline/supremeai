package org.example.agentorchestration.learning;

import org.example.agentorchestration.ExpertAgentRouter;
import org.example.model.APIProvider;
import org.example.service.ProviderRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.util.*;

/**
 * Knowledge Seed Service
 *
 * Answers the question: "is this only for internal orchestrator agents?"
 * → NO. Level 1-4 learning covers ALL AI models:
 *   • 20 internal MoE agents (SupremeAI agents from phases 1-10)
 *   • 0..n external AI providers chosen by the admin
 *
 * On startup this service seeds BASE KNOWLEDGE for every agent/provider
 * directly into Firebase — NOT in GitHub.
 *
 * What is seeded:
 *   learning/knowledge_seed/ai_providers/{provider}  — capabilities, best tasks
 *   learning/knowledge_seed/moe_agents/{agent}        — domain expertise, phase
 *   learning/knowledge_seed/metadata                  — seed version + timestamp
 *   learning/routing_weights/{agent}/{task}           — initial MoE priors
 *
 * Idempotent: safe to restart — Firebase updateChildren only overwrites
 * keys that exist in the seed map, preserving learned weights.
 */
@Service
public class KnowledgeSeedService {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeSeedService.class);
    private static final String SEED_VERSION = "1.1";

    @Autowired
    private LearningFirebaseRepository firebaseRepo;

    @Autowired(required = false)
    private ProviderRegistryService providerRegistryService;

    // ── External AI Providers ───────────────────────────────────────────────

    // Base knowledge for each provider (seeded once; RLVR updates learn from there)
    private static Map<String, Object> providerKnowledge(String provider) {
        Map<String, Object> k = new LinkedHashMap<>();
        switch (provider) {
            case "openai-gpt4":
                k.put("strengths",        Arrays.asList("code_generation", "reasoning", "instruction_following"));
                k.put("weaknesses",       Arrays.asList("high_cost", "rate_limits"));
                k.put("best_tasks",       Arrays.asList("CODE_GENERATION", "BUG_FIX", "CODE_REVIEW"));
                k.put("base_confidence",  0.88);
                break;
            case "anthropic-claude":
                k.put("strengths",        Arrays.asList("long_context", "safety", "nuanced_reasoning"));
                k.put("weaknesses",       Arrays.asList("slower_speed", "conservative"));
                k.put("best_tasks",       Arrays.asList("ARCHITECTURE_DESIGN", "CODE_REVIEW", "SECURITY_AUDIT"));
                k.put("base_confidence",  0.87);
                break;
            case "google-gemini":
                k.put("strengths",        Arrays.asList("multimodal", "fast", "large_context"));
                k.put("weaknesses",       Arrays.asList("consistency_variance"));
                k.put("best_tasks",       Arrays.asList("CODE_GENERATION", "TESTING", "DEPLOYMENT"));
                k.put("base_confidence",  0.84);
                break;
            case "meta-llama":
                k.put("strengths",        Arrays.asList("open_source", "customizable", "cost_efficient"));
                k.put("weaknesses",       Arrays.asList("less_instruction_following"));
                k.put("best_tasks",       Arrays.asList("LEARNING", "META_IMPROVEMENT", "CODE_GENERATION"));
                k.put("base_confidence",  0.78);
                break;
            case "mistral":
                k.put("strengths",        Arrays.asList("efficient", "european_compliance", "fast"));
                k.put("weaknesses",       Arrays.asList("smaller_knowledge_base"));
                k.put("best_tasks",       Arrays.asList("CODE_GENERATION", "COST_OPTIMIZATION"));
                k.put("base_confidence",  0.80);
                break;
            case "cohere":
                k.put("strengths",        Arrays.asList("embedding", "retrieval_augmented", "reranking"));
                k.put("weaknesses",       Arrays.asList("less_creative_generation"));
                k.put("best_tasks",       Arrays.asList("LEARNING", "ARCHITECTURE_DESIGN"));
                k.put("base_confidence",  0.76);
                break;
            case "huggingface":
                k.put("strengths",        Arrays.asList("open_models", "research_grade", "flexible"));
                k.put("weaknesses",       Arrays.asList("variable_quality", "self_hosted_needed"));
                k.put("best_tasks",       Arrays.asList("LEARNING", "TESTING", "META_IMPROVEMENT"));
                k.put("base_confidence",  0.70);
                break;
            case "xai-grok":
                k.put("strengths",        Arrays.asList("real_time_data", "deep_reasoning", "web_access"));
                k.put("weaknesses",       Arrays.asList("newer_model", "evolving"));
                k.put("best_tasks",       Arrays.asList("META_IMPROVEMENT", "SECURITY_AUDIT", "LEARNING"));
                k.put("base_confidence",  0.82);
                break;
            case "deepseek":
                k.put("strengths",        Arrays.asList("code_specialized", "cost_efficient", "strong_math"));
                k.put("weaknesses",       Arrays.asList("primarily_english", "newer_in_west"));
                k.put("best_tasks",       Arrays.asList("CODE_GENERATION", "BUG_FIX", "TESTING"));
                k.put("base_confidence",  0.85);
                break;
            case "perplexity":
                k.put("strengths",        Arrays.asList("search_augmented", "current_info", "citations"));
                k.put("weaknesses",       Arrays.asList("less_creative", "search_dependent"));
                k.put("best_tasks",       Arrays.asList("LEARNING", "SECURITY_AUDIT", "ARCHITECTURE_DESIGN"));
                k.put("base_confidence",  0.79);
                break;
            default:
                k.put("strengths",        Collections.emptyList());
                k.put("best_tasks",       Collections.emptyList());
                k.put("base_confidence",  0.70);
        }
        k.put("provider_type", "external_ai");
        k.put("seeded_at",     System.currentTimeMillis());
        return k;
    }

    // ── 20 Internal MoE Agent knowledge ───────────────────────────────────────

    private static Map<String, Object> agentKnowledge(String agent) {
        Map<String, Object> k = new LinkedHashMap<>();
        k.put("provider_type", "internal_moe_agent");
        k.put("seeded_at", System.currentTimeMillis());
        switch (agent) {
            case "Architect":
                k.put("phase", "core"); k.put("domain", "system_design");
                k.put("best_tasks", Arrays.asList("ARCHITECTURE_DESIGN", "CODE_REVIEW", "DEPLOYMENT"));
                k.put("base_confidence", 0.90); break;
            case "Builder":
                k.put("phase", "core"); k.put("domain", "code_generation");
                k.put("best_tasks", Arrays.asList("CODE_GENERATION", "DEPLOYMENT", "TESTING"));
                k.put("base_confidence", 0.88); break;
            case "Reviewer":
                k.put("phase", "core"); k.put("domain", "code_review");
                k.put("best_tasks", Arrays.asList("CODE_REVIEW", "SECURITY_AUDIT", "TESTING"));
                k.put("base_confidence", 0.87); break;
            case "A-Visual":
                k.put("phase", "6"); k.put("domain", "visualization");
                k.put("best_tasks", Arrays.asList("CODE_GENERATION", "META_IMPROVEMENT"));
                k.put("base_confidence", 0.82); break;
            case "B-Fixer":
                k.put("phase", "6"); k.put("domain", "bug_fixing");
                k.put("best_tasks", Arrays.asList("BUG_FIX", "TESTING", "CODE_REVIEW"));
                k.put("base_confidence", 0.91); break;
            case "C-Tester":
                k.put("phase", "6"); k.put("domain", "testing");
                k.put("best_tasks", Arrays.asList("TESTING", "BUG_FIX", "CODE_REVIEW"));
                k.put("base_confidence", 0.88); break;
            case "D-iOS":
                k.put("phase", "7"); k.put("domain", "mobile_ios");
                k.put("best_tasks", Arrays.asList("CODE_GENERATION", "DEPLOYMENT"));
                k.put("base_confidence", 0.84); break;
            case "E-Web":
                k.put("phase", "7"); k.put("domain", "web_frontend");
                k.put("best_tasks", Arrays.asList("CODE_GENERATION", "TESTING", "DEPLOYMENT"));
                k.put("base_confidence", 0.85); break;
            case "F-Desktop":
                k.put("phase", "7"); k.put("domain", "desktop_apps");
                k.put("best_tasks", Arrays.asList("CODE_GENERATION", "DEPLOYMENT"));
                k.put("base_confidence", 0.80); break;
            case "G-Publish":
                k.put("phase", "7"); k.put("domain", "publish_deploy");
                k.put("best_tasks", Arrays.asList("DEPLOYMENT", "TESTING"));
                k.put("base_confidence", 0.83); break;
            case "Alpha-Security":
                k.put("phase", "8"); k.put("domain", "security");
                k.put("best_tasks", Arrays.asList("SECURITY_AUDIT", "CODE_REVIEW", "TESTING"));
                k.put("base_confidence", 0.92); break;
            case "Beta-Compliance":
                k.put("phase", "8"); k.put("domain", "compliance");
                k.put("best_tasks", Arrays.asList("SECURITY_AUDIT", "ARCHITECTURE_DESIGN"));
                k.put("base_confidence", 0.89); break;
            case "Gamma-Privacy":
                k.put("phase", "8"); k.put("domain", "privacy");
                k.put("best_tasks", Arrays.asList("SECURITY_AUDIT", "CODE_REVIEW"));
                k.put("base_confidence", 0.87); break;
            case "Delta-Cost":
                k.put("phase", "9"); k.put("domain", "cost_analysis");
                k.put("best_tasks", Arrays.asList("COST_OPTIMIZATION", "ARCHITECTURE_DESIGN"));
                k.put("base_confidence", 0.86); break;
            case "Epsilon-Optimizer":
                k.put("phase", "9"); k.put("domain", "optimization");
                k.put("best_tasks", Arrays.asList("COST_OPTIMIZATION", "CODE_GENERATION", "DEPLOYMENT"));
                k.put("base_confidence", 0.85); break;
            case "Zeta-Finance":
                k.put("phase", "9"); k.put("domain", "financial_modeling");
                k.put("best_tasks", Arrays.asList("COST_OPTIMIZATION", "ARCHITECTURE_DESIGN"));
                k.put("base_confidence", 0.83); break;
            case "Eta-Meta":
                k.put("phase", "10"); k.put("domain", "meta_cognition");
                k.put("best_tasks", Arrays.asList("META_IMPROVEMENT", "LEARNING", "ARCHITECTURE_DESIGN"));
                k.put("base_confidence", 0.88); break;
            case "Theta-Learning":
                k.put("phase", "10"); k.put("domain", "learning_systems");
                k.put("best_tasks", Arrays.asList("LEARNING", "META_IMPROVEMENT", "CODE_REVIEW"));
                k.put("base_confidence", 0.87); break;
            case "Iota-Knowledge":
                k.put("phase", "10"); k.put("domain", "knowledge_management");
                k.put("best_tasks", Arrays.asList("LEARNING", "ARCHITECTURE_DESIGN", "META_IMPROVEMENT"));
                k.put("base_confidence", 0.85); break;
            case "Kappa-Evolution":
                k.put("phase", "10"); k.put("domain", "self_evolution");
                k.put("best_tasks", Arrays.asList("META_IMPROVEMENT", "LEARNING", "CODE_GENERATION"));
                k.put("base_confidence", 0.89); break;
            default:
                k.put("phase", "unknown"); k.put("domain", "general");
                k.put("best_tasks", Collections.emptyList());
                k.put("base_confidence", 0.75);
        }
        return k;
    }

    // ── Startup seed ───────────────────────────────────────────────────────────

    @PostConstruct
    public void seedAll() {
        if (!firebaseRepo.isAvailable()) {
            logger.warn("⚠️  Firebase not available — knowledge seed skipped (in-memory only)");
            return;
        }
        logger.info("🌱 KnowledgeSeedService: seeding ALL AI model knowledge into Firebase...");
        List<String> providers = getAllProviders();
        seedExternalAIProviders();
        seedInternalMoEAgents();
        seedInitialRoutingWeights();
        seedMetadata();
        logger.info("✅ Knowledge seed complete — {} providers + {} MoE agents",
            providers.size(), ExpertAgentRouter.ALL_AGENTS.size());
    }

    /**
     * Seed base knowledge for all configured external AI providers.
     * Path: learning/knowledge_seed/ai_providers/{provider}
     *
    * This is NOT just the internal orchestrator — it covers every provider
     * that MultiAIConsensusService uses.
     */
    private void seedExternalAIProviders() {
        for (String provider : getAllProviders()) {
            Map<String, Object> knowledge = providerKnowledge(provider);
            firebaseRepo.seed(
                LearningFirebaseRepository.BASE + "/knowledge_seed/ai_providers/"
                    + LearningFirebaseRepository.key(provider),
                knowledge
            );
            logger.debug("  🤖 Seeded external AI: {}", provider);
        }
    }

    /**
     * Seed base knowledge for all 20 internal MoE agents.
     * Path: learning/knowledge_seed/moe_agents/{agent}
     */
    private void seedInternalMoEAgents() {
        for (String agent : ExpertAgentRouter.ALL_AGENTS) {
            Map<String, Object> knowledge = agentKnowledge(agent);
            firebaseRepo.seed(
                LearningFirebaseRepository.BASE + "/knowledge_seed/moe_agents/"
                    + LearningFirebaseRepository.key(agent),
                knowledge
            );
            logger.debug("  🧠 Seeded internal agent: {}", agent);
        }
    }

    /**
     * Seed initial routing weights for all agent×taskType pairs.
     * Uses base_confidence from agent knowledge as the initial weight.
     * Path: learning/routing_weights/{agent}/{taskType}
     *
     * RLVR will update these over time — this just gives a warm start.
     */
    private void seedInitialRoutingWeights() {
        // Seed for internal MoE agents
        for (String agent : ExpertAgentRouter.ALL_AGENTS) {
            Map<String, Object> agentK = agentKnowledge(agent);
            double baseConf = (double) agentK.getOrDefault("base_confidence", 0.75);
            @SuppressWarnings("unchecked")
            List<String> bestTasks = (List<String>) agentK.getOrDefault("best_tasks", Collections.emptyList());

            for (ExpertAgentRouter.TaskType tt : ExpertAgentRouter.TaskType.values()) {
                double weight = bestTasks.contains(tt.name()) ? baseConf : baseConf * 0.5;
                firebaseRepo.saveRoutingWeight(agent, tt.name(), weight);
            }
        }
        // Seed for external AI providers as "virtual agents"
        for (String provider : getAllProviders()) {
            Map<String, Object> provK = providerKnowledge(provider);
            double baseConf = (double) provK.getOrDefault("base_confidence", 0.75);
            @SuppressWarnings("unchecked")
            List<String> bestTasks = (List<String>) provK.getOrDefault("best_tasks", Collections.emptyList());

            for (ExpertAgentRouter.TaskType tt : ExpertAgentRouter.TaskType.values()) {
                double weight = bestTasks.contains(tt.name()) ? baseConf : baseConf * 0.4;
                firebaseRepo.saveRoutingWeight(provider, tt.name(), weight);
            }
        }
        logger.info("  ⚖️  Seeded initial routing weights for {} agents + {} providers",
            ExpertAgentRouter.ALL_AGENTS.size(), getAllProviders().size());
    }

    /**
     * Seed metadata record so we can check seed version in Firebase console.
     * Path: learning/knowledge_seed/metadata
     */
    private void seedMetadata() {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("seed_version",        SEED_VERSION);
        meta.put("seeded_at",           System.currentTimeMillis());
        meta.put("total_moe_agents",    ExpertAgentRouter.ALL_AGENTS.size());
        meta.put("total_ai_providers",  getAllProviders().size());
        meta.put("learning_levels",     Arrays.asList(
            "L1: RLVR routing weights (who wins)",
            "L2: Agent pattern profiles (how they reason)",
            "L3: Reasoning chain store (copy best chains)",
            "L4: AI-generated new reasoning (few-shot)"
        ));
        meta.put("coverage",            "ALL AI models — internal MoE + external providers");
        firebaseRepo.seed(LearningFirebaseRepository.BASE + "/knowledge_seed/metadata", meta);
    }

    /** Public accessor so other services can check all providers. */
    public List<String> getAllProviders() {
        if (providerRegistryService == null) {
            return getDefaultProviders();
        }

        List<String> providerKeys = new ArrayList<>();
        for (APIProvider provider : providerRegistryService.getActiveProviders()) {
            providerKeys.add(provider.getId());
        }
        if (providerKeys.isEmpty()) {
            return getDefaultProviders();
        }
        return providerKeys;
    }

    public Map<String, Object> getProviderKnowledge(String provider) {
        return providerKnowledge(provider);
    }

    public Map<String, Object> getAgentKnowledge(String agent) {
        return agentKnowledge(agent);
    }

    private List<String> getDefaultProviders() {
        return Arrays.asList(
            "openai-gpt4",
            "anthropic-claude",
            "google-gemini",
            "meta-llama",
            "mistral",
            "cohere",
            "huggingface",
            "xai-grok",
            "deepseek",
            "perplexity"
        );
    }
}
