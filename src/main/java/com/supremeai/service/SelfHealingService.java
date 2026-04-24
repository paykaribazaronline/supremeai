package com.supremeai.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class SelfHealingService {

    @Autowired
    private AIReasoningService reasoningService;

    public void handleWorkflowFailure(String repo, String workflowId, String errorLog) {
        reasoningService.logReasoning(
                workflowId, 
                "Self-Healing Triggered", 
                "Workflow failure detected in " + repo + ". Error: " + truncate(errorLog),
                "SupremeAI-SelfHealer"
        );

        // Logic to analyze error and generate fix
        String suggestedFix = analyzeError(errorLog);
        
        if (suggestedFix != null) {
            applyFix(repo, suggestedFix);
        }
    }

    private String analyzeError(String log) {
        if (log.contains("Dependency resolution failed")) {
            return "CHECK_DEPENDENCIES";
        } else if (log.contains("Tests failed")) {
            return "FIX_TESTS";
        } else if (log.contains("Unauthorized")) {
            return "CHECK_SECRETS";
        }
        return "GENERAL_ANALYSIS";
    }

    private void applyFix(String repo, String fixType) {
        // In a real scenario, this would trigger a GitHub PR or Commit
        System.out.println("Applying fix [" + fixType + "] to repository: " + repo);
    }

    private String truncate(String text) {
        if (text == null) return "";
        return text.length() > 100 ? text.substring(0, 100) + "..." : text;
    }
}
