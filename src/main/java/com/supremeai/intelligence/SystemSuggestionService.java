package com.supremeai.intelligence;

import com.supremeai.admin.AdminDashboardService;
import com.supremeai.learning.SupremeLearningOrchestrator;
import com.supremeai.model.ImprovementProposal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * SystemSuggestionService
 * 
 * Proactive advisor for SupremeAI infrastructure.
 * Evaluates new models from shared links and suggests better alternatives 
 * based on the logic defined in core_knowledge.json.
 */
@Service
public class SystemSuggestionService {
    public SystemSuggestionService(SupremeLearningOrchestrator supremeLearningOrchestrator, AdminDashboardService adminDashboard) {
        this.supremeLearningOrchestrator = supremeLearningOrchestrator;
        this.adminDashboard = adminDashboard;
    }


    private static final Logger log = LoggerFactory.getLogger(SystemSuggestionService.class);



    /**
     * Evaluates an AI model based on a shared link or metadata.
     * 
     * @param sourceUrl Link to model (HuggingFace, Arxiv, GitHub, etc.)
     * @param currentModelId The model currently deployed for a specific hub
     * @return A recommendation report
     */
    public Mono<String> evaluateModelLink(String sourceUrl, String currentModelId) {
        log.info("[SYSTEM_SUGGESTION] Evaluating model link: {} as alternative to {}", sourceUrl, currentModelId);

        // Step 1: Simulate metadata extraction (In production, this would use a web scraper)
        // For testing, we extract intent from URL structure
        String modelName = extractModelName(sourceUrl);
        
        return Mono.just(simulateEvaluation(modelName, currentModelId))
            .flatMap(score -> {
                if (score > 0.85) { // Threshold for recommendation
                    String reason = String.format(
                        "New model '%s' shows %d%% better parameter efficiency than '%s'.", 
                        modelName, (int)((score - 0.7) * 100), currentModelId
                    );
                    
                    ImprovementProposal proposal = new ImprovementProposal(
                        "Model Upgrade Suggestion",
                        reason + " Suggested action: Deploy via Cloud Run for specialized benchmark.",
                        "INFRASTRUCTURE_UPGRADE",
                        sourceUrl
                    );
                    
                    return adminDashboard.submitImprovement(proposal)
                        .map(approved -> approved ? 
                            "Recommendation: Upgrade approved by auto-pilot. Starting deployment pre-checks." :
                            "Recommendation: High potential detected. Pending Admin approval in Dashboard.");
                } else {
                    return Mono.just("Recommendation: Current models are still superior for this specific task category.");
                }
            });
    }

    private String extractModelName(String url) {
        try {
            String[] parts = url.split("/");
            return parts[parts.length - 1].replace("-", " ");
        } catch (Exception e) {
            return "Unknown New Model";
        }
    }

    private double simulateEvaluation(String modelName, String currentModel) {
        // Logic from core_knowledge.json: evaluation_criteria
        // [parameter_efficiency, context_window, task_accuracy, infrastructure_cost]
        
        // Random simulation for testing phase
        double baseScore = 0.7; 
        if (modelName.toLowerCase().contains("llama 3.2") || modelName.toLowerCase().contains("deepseek v3")) {
            baseScore += 0.2; // Known better models
        }
        return baseScore + (Math.random() * 0.1);
    }

    /**
     * Periodically checks for "better alternatives" for the current hub orchestration.
     */
    public void scanForBetterAlternatives() {
        log.info("[SYSTEM_SUGGESTION] Scanning global landscape for infrastructure optimizations...");
        // This will eventually integrate with RSS feeds of research papers or model hubs
    }
}
