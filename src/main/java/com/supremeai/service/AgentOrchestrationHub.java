package com.supremeai.service;

import com.supremeai.model.ReverseEngineeringJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Simplified Hub to replace 15+ micro-agents.
 * All core logic for Security, Cost, and Compliance is being consolidated here.
 */
@Service
public class AgentOrchestrationHub {

    private static final Logger logger = LoggerFactory.getLogger(AgentOrchestrationHub.class);

    @Autowired
    private ReverseEngineeringIntegrationService reverseEngineeringIntegrationService;

    @org.springframework.beans.factory.annotation.Autowired
    private CodeGenerationService codeGenerationService;

    @org.springframework.beans.factory.annotation.Autowired
    private SimulatorService simulatorService;

    @Autowired
    private com.supremeai.agentorchestration.CrossAgentVectorMemory crossAgentMemory;

    /**
     * Executes a specific agent with given inputs.
     * Routes to the appropriate service based on agent name.
     */
    public Mono<Map<String, Object>> executeAgent(String agentName, Map<String, Object> input) {
        logger.info("[AgentHub] Executing agent: {} with input: {}", agentName, input);
        
        String sessionId = (String) input.getOrDefault("sessionId", "default-session");
        String taskType = (String) input.getOrDefault("taskType", "general");
        
        // Retrieve shared context
        String sharedContext = crossAgentMemory.retrieveRelevantContext(sessionId, agentName, taskType);
        if (!sharedContext.isEmpty()) {
            logger.info("Retrieved shared context from CrossAgentVectorMemory for {}", agentName);
            input.put("sharedAgentContext", sharedContext);
        }
        
        return switch (agentName) {
            case "ReverseEngineeringAgent" -> executeReverseEngineering(input)
                .doOnSuccess(res -> crossAgentMemory.storeContext(sessionId, agentName, res.toString(), taskType));
            case "CodeGenerationAgent" -> executeCodeGeneration(input)
                .doOnSuccess(res -> crossAgentMemory.storeContext(sessionId, agentName, res.toString(), taskType));
            case "SimulatorAgent" -> executeSimulator(input)
                .doOnSuccess(res -> crossAgentMemory.storeContext(sessionId, agentName, res.toString(), taskType));
            default -> Mono.error(new RuntimeException("Unknown agent: " + agentName));
        };
    }

    private Mono<Map<String, Object>> executeReverseEngineering(Map<String, Object> input) {
        String url = (String) input.get("url");
        String userId = (String) input.getOrDefault("userId", "system");
        
        return reverseEngineeringIntegrationService.startJob(userId, url, "FULL_ANALYSIS", null, null)
            .map(job -> Map.of("jobId", job.getJobId(), "status", job.getStatus()));
    }

    private Mono<Map<String, Object>> executeCodeGeneration(Map<String, Object> input) {
        String requirements = (String) input.getOrDefault("requirements", input.getOrDefault("task", "Generic app"));
        String userId = (String) input.getOrDefault("userId", "system");

        // Delegate to CodeGenerationService; run on boundedElastic to avoid blocking the Netty event loop
        return Mono.fromCallable(() -> {
                Map<String, String> context = new HashMap<>();
                context.put("architecture", (String) input.getOrDefault("architecture", "monolith"));
                context.put("database", (String) input.getOrDefault("database", "PostgreSQL"));
                context.put("apiStyle", (String) input.getOrDefault("apiStyle", "REST"));
                context.put("authType", (String) input.getOrDefault("authType", "JWT"));
                context.put("frontend", (String) input.getOrDefault("frontend", "React"));
                context.put("deployment", (String) input.getOrDefault("deployment", "GCP"));
                Map<String, Object> result = codeGenerationService.generateFromContext(context);
                result.put("appId", result.getOrDefault("appId", "gen_" + UUID.randomUUID().toString().substring(0, 8)));
                result.put("status", "GENERATED");
                result.put("requirements", requirements);
                logger.info("[CodeGeneration] appId={} userId={}", result.get("appId"), userId);
                return result;
            })
            .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }

    private Mono<Map<String, Object>> executeSimulator(Map<String, Object> input) {
        String appId = (String) input.get("app_id");
        List<String> devices = (List<String>) input.getOrDefault("device_types", List.of("PIXEL_6"));
        
        // Logic to deploy to simulator
        return Mono.just(Map.of("sessionId", "session_" + UUID.randomUUID().toString().substring(0, 8), "status", "DEPLOYED"));
    }
}

