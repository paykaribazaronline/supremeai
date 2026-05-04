package com.supremeai.agentorchestration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import com.supremeai.service.TranslationService;
import com.supremeai.service.UserLanguagePreferenceService;
import com.supremeai.service.EnhancedLearningService;
import com.supremeai.model.UserLanguagePreference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.*;

@Profile("!local")
@Service
public class AdaptiveAgentOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(AdaptiveAgentOrchestrator.class);

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

    @Autowired(required = false)
    private EnhancedLearningService enhancedLearningService;

    public AdaptiveAgentOrchestrator() {}

    /**
     * Main orchestration entry point - single argument version for API compatibility.
     * Takes a requirement and uses default userId and project.
     */
    public OrchesResultContext orchestrate(String requirement) {
        return orchestrate(requirement, "default-user", "default", null);
    }

    /**
     * Main orchestration entry point with userId.
     * Takes a requirement and userId, returns an OrchesResultContext.
     * Language support: translates questions to user's preferred language.
     */
    public OrchesResultContext orchestrate(String requirement, String userId) {
        return orchestrate(requirement, userId, "default", null);
    }

    /**
     * Main orchestration entry point (3-param version - delegates to multimodal version).
     * Takes a requirement, userId, and projectId, returns an OrchesResultContext.
     * Language support: translates questions to user's preferred language.
     * Behavior profiles: applies project-specific AI coding standards.
     */
    public OrchesResultContext orchestrate(String requirement, String userId, String projectId) {
        return orchestrate(requirement, userId, projectId, null);
    }

    /**
     * Multimodal orchestration entry point - with image support.
     * Takes a requirement, userId, projectId, and optional image URL.
     * Enables multimodal learning capture.
     */
    public OrchesResultContext orchestrate(String requirement, String userId, String projectId, String imageUrl) {
        Map<String, Object> context = new LinkedHashMap<>();
        Date started = new Date();
        context.put("startedAt", started);
        context.put("originalRequirement", requirement);
        context.put("projectId", projectId);
        context.put("imageUrl", imageUrl);

        // Detect platform for learning capture
        String detectedPlatform = detectPlatform(requirement);
        context.put("detectedPlatform", detectedPlatform);

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

        String detectedMode = detectMode(requirement);
        context.put("mode", detectedMode);

        OrchesResultContext result = new OrchesResultContext(context);
        result.setStartedAt(started);
        result.setCompletedAt(completed);
        result.setStatus("COMPLETED");
        result.setMode(detectedMode);

        return result;
    }

    /**
     * Record the final build result - call this after actual app build completes
     */
    public void recordBuildResult(String requirement, String appType, boolean buildSuccess,
                                  String apkPath, Map<String, Object> additionalMetrics) {
        if (enhancedLearningService != null) {
            Map<String, Object> buildMetrics = new HashMap<>();
            buildMetrics.put("appType", appType);
            buildMetrics.put("buildSuccess", buildSuccess);
            buildMetrics.put("apkPath", apkPath);
            if (additionalMetrics != null) {
                buildMetrics.putAll(additionalMetrics);
            }

            enhancedLearningService.learnFromAppGeneration(
                    requirement,
                    (String) additionalMetrics.get("platform"),
                    buildSuccess,
                    apkPath,
                    buildMetrics,
                    "AdaptiveAgentOrchestrator"
            ).subscribe();
        }
    }

    private List<Question> generateQuestionsAI(String requirement) {
        List<Question> questions = new ArrayList<>();
        try {
            AIProvider provider = providerFactory.getProvider("groq");
            String prompt = String.format(
                    "Analyze this app requirement and generate 5 key questions to clarify the app's features, target platform, and user needs:\n\nRequirement: %s\n\nFormat as JSON array of objects with 'key', 'text', and 'priority' fields.",
                    requirement
            );
            String response = provider.generate(prompt).block();
            List<Map<String, Object>> parsed = new ObjectMapper().readValue(
                    response, new TypeReference<List<Map<String, Object>>>() {}
            );
            for (Map<String, Object> q : parsed) {
                questions.add(new Question(
                        (String) q.get("key"),
                        (String) q.get("text"),
                        ((Number) q.getOrDefault("priority", 1)).intValue()
                ));
            }
        } catch (Exception e) {
            logger.error("Failed to generate questions with AI", e);
            questions = getDefaultQuestions();
        }
        return questions;
    }

    private List<Question> getDefaultQuestions() {
        return Arrays.asList(
                new Question("platform", "What platform should the app target? (web, mobile, desktop)", 1),
                new Question("features", "What are the core features needed?", 1),
                new Question("users", "Who are the target users?", 2),
                new Question("design", "Any specific design preferences or style?", 3),
                new Question("timeline", "What is the expected timeline?", 2)
        );
    }

    private Map<String, String> autoAnswerQuestions(List<Question> questions) {
        Map<String, String> answers = new HashMap<>();
        for (Question q : questions) {
            answers.put(q.getKey(), "auto-answered");
        }
        return answers;
    }

    private List<VotingDecision> buildDirectDecisions(Map<String, String> answers) {
        List<VotingDecision> decisions = new ArrayList<>();
        decisions.add(new VotingDecision("platform", "web", 0.9));
        decisions.add(new VotingDecision("framework", "React", 0.8));
        return decisions;
    }

    private Map<String, Object> buildGenerationContext(List<VotingDecision> decisions) {
        Map<String, Object> context = new HashMap<>();
        for (VotingDecision d : decisions) {
            context.put(d.getDecisionKey(), d.getDecisionValue());
        }
        return context;
    }

    private String detectPlatform(String requirement) {
        String req = requirement.toLowerCase();
        if (req.contains("mobile") || req.contains("android") || req.contains("ios")) {
            return "mobile";
        } else if (req.contains("desktop") || req.contains("windows") || req.contains("mac")) {
            return "desktop";
        }
        return "web";
    }

    private String detectMode(String requirement) {
        String req = requirement.toLowerCase();
        if (req.contains("fullstack") || req.contains("complete") || req.contains("end-to-end")) {
            return "fullstack";
        }
        return "standard";
    }


    public static class Question {
        private String key;
        private String text;
        private int priority;

        public Question(String key, String text, int priority) {
            this.key = key;
            this.text = text;
            this.priority = priority;
        }

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public int getPriority() { return priority; }
        public void setPriority(int priority) { this.priority = priority; }
    }

    public static class VotingDecision {
        private String decisionKey;
        private String decisionValue;
        private double confidence;

        public VotingDecision(String decisionKey, String decisionValue, double confidence) {
            this.decisionKey = decisionKey;
            this.decisionValue = decisionValue;
            this.confidence = confidence;
        }

        public String getDecisionKey() { return decisionKey; }
        public void setDecisionKey(String decisionKey) { this.decisionKey = decisionKey; }
        public String getDecisionValue() { return decisionValue; }
        public void setDecisionValue(String decisionValue) { this.decisionValue = decisionValue; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
    }
}
