package com.supremeai.agentorchestration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AdaptiveAgentOrchestrator {

    @Autowired
    public AdaptiveAgentOrchestrator() {}

    /**
     * Main orchestration entry point.
     * Takes a requirement and returns an OrchesResultContext.
     */
    public OrchesResultContext orchestrate(String requirement) {
        Map<String, Object> context = new LinkedHashMap<>();
        Date started = new Date();
        context.put("startedAt", started);
        context.put("originalRequirement", requirement);

        // Step 1: Generate questions (taste phase: hardcoded)
        List<Question> questions = generateQuestions(requirement);
        context.put("questions", questions);

        // Step 2: For taste phase, auto-answer questions (simulate admin)
        Map<String, String> answers = autoAnswerQuestions(questions);
        context.put("answers", answers);

        // MVP mode: direct decisions for fastest response and lowest complexity.
        List<VotingDecision> decisions = buildDirectDecisions(answers);
        context.put("decisions", decisions);

        // Step 4: Build final context for code generator
        Map<String, Object> generationContext = buildGenerationContext(decisions);
        context.put("generationContext", generationContext);

        Date completed = new Date();
        context.put("completedAt", completed);
        context.put("status", "COMPLETED");

        OrchesResultContext result = new OrchesResultContext(context);
        result.setStartedAt(started);
        result.setCompletedAt(completed);
        result.setStatus("COMPLETED");
        return result;
    }

    /**
     * Generate a set of standard architecture/design questions.
     * In full version, this would use an AI to generate specific questions.
     */
    private List<Question> generateQuestions(String requirement) {
        List<Question> questions = new ArrayList<>();
        questions.add(new Question("architecture", "What architecture style? (monolith, microservices, serverless)", "HIGH"));
        questions.add(new Question("database", "Which database? (PostgreSQL, MySQL, MongoDB, DynamoDB)", "CRITICAL"));
        questions.add(new Question("apiStyle", "API style? (REST, GraphQL, gRPC)", "MEDIUM"));
        questions.add(new Question("authType", "Authentication type? (JWT, OAuth2, Session)", "HIGH"));
        questions.add(new Question("frontend", "Frontend framework? (React, Vue, Angular, None)", "MEDIUM"));
        questions.add(new Question("deployment", "Deployment target? (AWS, GCP, Azure, On-prem)", "MEDIUM"));
        return questions;
    }

    /**
     * Auto-answer questions for taste phase (simulates admin).
     * In production, admin would answer via UI.
     */
    private Map<String, String> autoAnswerQuestions(List<Question> questions) {
        Map<String, String> answers = new LinkedHashMap<>();
        // Default sensible answers for demo
        for (Question q : questions) {
            String key = q.getKey();
            switch (key) {
                case "architecture": answers.put(key, "monolith"); break;
                case "database": answers.put(key, "PostgreSQL"); break;
                case "apiStyle": answers.put(key, "REST"); break;
                case "authType": answers.put(key, "JWT"); break;
                case "frontend": answers.put(key, "React"); break;
                case "deployment": answers.put(key, "GCP"); break;
                default: answers.put(key, "default");
            }
        }
        return answers;
    }

    private List<VotingDecision> buildDirectDecisions(Map<String, String> answers) {
        List<VotingDecision> decisions = new ArrayList<>();
        for (Map.Entry<String, String> entry : answers.entrySet()) {
            VotingDecision decision = new VotingDecision();
            decision.setDecisionKey(entry.getKey());
            decision.setProposedAnswer(entry.getValue());
            decision.setAiConsensus(entry.getValue());
            decision.setConfidence(1.0);
            decision.setStrength("DIRECT");
            decision.setProviderVotes(List.of());
            decisions.add(decision);
        }
        return decisions;
    }

    private Map<String, Object> buildGenerationContext(List<VotingDecision> decisions) {
        Map<String, Object> ctx = new LinkedHashMap<>();
        for (VotingDecision d : decisions) {
            ctx.put(d.getDecisionKey(), d.getAiConsensus());
        }
        // Add defaults
        ctx.putIfAbsent("javaVersion", "17");
        ctx.putIfAbsent("springBootVersion", "3.2.3");
        ctx.putIfAbsent("includeTests", true);
        ctx.putIfAbsent("includeDocker", true);
        return ctx;
    }
}
