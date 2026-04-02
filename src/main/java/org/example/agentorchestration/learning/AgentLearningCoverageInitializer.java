package org.example.agentorchestration.learning;

import org.example.agentorchestration.ExpertAgentRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ensures Levels 2 and 3 have baseline coverage for every known internal agent
 * and external AI provider, even before live traffic accumulates history.
 */
@Service
public class AgentLearningCoverageInitializer {

    private static final Logger logger = LoggerFactory.getLogger(AgentLearningCoverageInitializer.class);

    @Autowired
    private AgentPatternProfiler profiler;

    @Autowired
    private ReasoningChainCopier chainCopier;

    @Autowired
    private KnowledgeSeedService knowledgeSeedService;

    @PostConstruct
    public void initializeCoverage() {
        profiler.ensureKnownProfilesPresent();

        Map<String, List<String>> tasksByAgent = new LinkedHashMap<>();
        for (String agent : ExpertAgentRouter.ALL_AGENTS) {
            tasksByAgent.put(agent, extractTaskTypes(knowledgeSeedService.getAgentKnowledge(agent)));
        }
        for (String provider : knowledgeSeedService.getAllProviders()) {
            tasksByAgent.put(provider, extractTaskTypes(knowledgeSeedService.getProviderKnowledge(provider)));
        }

        int bootstrapChains = chainCopier.ensureBootstrapCoverage(tasksByAgent);
        logger.info("🧠 Deep learning coverage ready: profiles={} bootstrapChains={}",
            profiler.getAllProfileSummaries().size(), bootstrapChains);
    }

    @SuppressWarnings("unchecked")
    private List<String> extractTaskTypes(Map<String, Object> knowledge) {
        Object bestTasks = knowledge == null ? null : knowledge.get("best_tasks");
        if (bestTasks instanceof List<?>) {
            List<String> tasks = new ArrayList<>();
            for (Object task : (List<Object>) bestTasks) {
                if (task != null) {
                    tasks.add(String.valueOf(task));
                }
            }
            if (!tasks.isEmpty()) {
                return tasks;
            }
        }
        return List.of("LEARNING");
    }
}