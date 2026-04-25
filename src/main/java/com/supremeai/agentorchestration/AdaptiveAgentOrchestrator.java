package com.supremeai.agentorchestration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import com.supremeai.service.TranslationService;
import com.supremeai.service.UserLanguagePreferenceService;
import com.supremeai.model.UserLanguagePreference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.*;

@Service
public class AdaptiveAgentOrchestrator {

    @Autowired
    private RequirementAnalyzerAI requirementAnalyzer;

    @Autowired
    private AIProviderFactory providerFactory;

    @Autowired
    private com.supremeai.agent.DiOSAgent diOSAgent;

    @Autowired
    private com.supremeai.agent.EWebAgent eWebAgent;

    @Autowired
    private com.supremeai.agent.FDesktopAgent fDesktopAgent;

    @Autowired
    private com.supremeai.agent.GPublishAgent gPublishAgent;

    @Autowired
    private TranslationService translationService;

    @Autowired
    private UserLanguagePreferenceService languagePreferenceService;

    @Autowired
    private com.supremeai.service.AIBehaviorProfileService behaviorProfileService;

    public AdaptiveAgentOrchestrator() {}

    /**
     * Main orchestration entry point - single argument version for API compatibility.
     * Takes a requirement and uses default userId and project.
     */
    public OrchesResultContext orchestrate(String requirement) {
        return orchestrate(requirement, "default-user", "default");
    }

    /**
     * Main orchestration entry point with userId.
     * Takes a requirement and userId, returns an OrchesResultContext.
     * Language support: translates questions to user's preferred language.
     */
    public OrchesResultContext orchestrate(String requirement, String userId) {
        return orchestrate(requirement, userId, "default");
    }

    /**
     * Main orchestration entry point.
     * Takes a requirement, userId, and projectId, returns an OrchesResultContext.
     * Language support: translates questions to user's preferred language.
     * Behavior profiles: applies project-specific AI coding standards.
     */
    public OrchesResultContext orchestrate(String requirement, String userId, String projectId) {
        Map<String, Object> context = new LinkedHashMap<>();
        Date started = new Date();
        context.put("startedAt", started);
        context.put("originalRequirement", requirement);
        context.put("projectId", projectId);

        // Get user's language preference
        UserLanguagePreference languagePreference = languagePreferenceService
                .getUserLanguagePreference(userId)
                .block(); // Blocking for simplicity in this context
        String userLanguage = languagePreference != null ?
                languagePreference.getLanguageName() : "English";

        // Get and apply behavior profile for the project
        com.supremeai.model.AIBehaviorProfile behaviorProfile = behaviorProfileService
                .getProfileForProject(projectId)
                .block();
        context.put("behaviorProfile", behaviorProfile);

        // Step 1: Generate questions (in English)
        List<Question> questions = generateQuestionsAI(requirement);

        // Translate questions to user's language
        List<Question> translatedQuestions = new ArrayList<>();
        for (Question question : questions) {
            String translatedText = translationService.translateFromEnglish(
                    question.getText(), userLanguage).block();
            Question translatedQuestion = new Question(
                    question.getKey(),
                    translatedText,
                    question.getPriority()
            );
            translatedQuestions.add(translatedQuestion);
        }
        context.put("translatedQuestions", translatedQuestions);

        // Step 2: For taste phase, auto-answer questions (simulate admin)
        Map<String, String> answers = autoAnswerQuestions(questions);

        // MVP mode: direct decisions for fastest response and lowest complexity.
        List<VotingDecision> decisions = buildDirectDecisions(answers);
        context.put("decisions", decisions);

        // Step 4: Build final context for code generator
        Map<String, Object> generationContext = buildGenerationContext(decisions);

        // Apply behavior profile to generation context
        if (behaviorProfile != null) {
            behaviorProfileService.applyProfileToContext(behaviorProfile, generationContext);
        }

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
     * Generate questions using AI based on the requirement.
     * প্ল্যাটফর্ম অনুযায়ী উপযুক্ত এজেন্ট নির্বাচন করে প্রশ্ন তৈরি করে
     */
    private List<Question> generateQuestionsAI(String requirement) {
        // প্ল্যাটফর্ম সনাক্তকরণ
        String platform = detectPlatform(requirement);

        // প্ল্যাটফর্ম অনুযায়ী এজেন্ট নির্বাচন
        switch (platform.toLowerCase()) {
            case "ios":
                return diOSAgent.analyzeIOSRequirements(requirement);
            case "desktop":
                return fDesktopAgent.analyzeDesktopRequirements(requirement);
            case "web":
                return eWebAgent.analyzeWebRequirements(requirement);
            default:
                // ডিফল্ট হিসেবে মূল প্রয়োজনীয়তা বিশ্লেষক ব্যবহার
                return requirementAnalyzer.analyze(requirement);
        }
    }

    /**
     * প্রয়োজনীয়তা থেকে প্ল্যাটফর্ম সনাক্ত করে
     */
    private String detectPlatform(String requirement) {
        String lowerReq = requirement.toLowerCase();

        if (lowerReq.contains("ios") || lowerReq.contains("iphone") || lowerReq.contains("ipad")) {
            return "ios";
        } else if (lowerReq.contains("desktop") || lowerReq.contains("windows") || 
                   lowerReq.contains("mac") || lowerReq.contains("linux")) {
            return "desktop";
        } else if (lowerReq.contains("web") || lowerReq.contains("website") || 
                   lowerReq.contains("browser")) {
            return "web";
        } else if (lowerReq.contains("android")) {
            return "android";
        }

        // ডিফল্ট প্ল্যাটফর্ম
        return "web";
    }

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
