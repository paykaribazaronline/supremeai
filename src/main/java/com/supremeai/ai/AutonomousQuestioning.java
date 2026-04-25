package com.supremeai.ai;

import com.supremeai.ai.client.GeminiClient;
import com.supremeai.ai.client.OpenAIClient;
import com.supremeai.dto.AmbiguityScore;
import com.supremeai.dto.ClarificationResponse;
import com.supremeai.dto.UserRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AutonomousQuestioning {

    private static final Logger log = LoggerFactory.getLogger(AutonomousQuestioning.class);

    private final GeminiClient geminiClient;
    private final OpenAIClient openAIClient;

    @Autowired
    public AutonomousQuestioning(GeminiClient geminiClient, OpenAIClient openAIClient) {
        this.geminiClient = geminiClient;
        this.openAIClient = openAIClient;
    }

    public ClarificationResponse analyzeAndClarify(UserRequest request) {
        try {
            AmbiguityScore score = analyzeAmbiguity(request);
            log.info("Ambiguity score for request {}: {}", request.getUserId(), score.getConfidence());

            if (score.getConfidence() < 0.7) {
                List<String> questions = generateQuestions(request, score);
                return ClarificationResponse.builder()
                        .needsClarification(true)
                        .questions(questions)
                        .suggestedApproach(inferApproach(request))
                        .build();
            }

            return ClarificationResponse.builder()
                    .needsClarification(false)
                    .directResponse(processRequest(request))
                    .build();
        } catch (Exception e) {
            log.error("Error analyzing request: {}", request.getUserId(), e);
            return ClarificationResponse.builder()
                    .needsClarification(false)
                    .directResponse("Error processing request, please try again.")
                    .build();
        }
    }

    private AmbiguityScore analyzeAmbiguity(UserRequest request) {
        try {
            String prompt = String.format(
                "Analyze ambiguity of this request: %s. Return confidence (0-1) and unclear areas as JSON: {\"confidence\": 0.8, \"unclearAreas\": [\"area1\"]}",
                request.getDescription()
            );
            String response = geminiClient.analyze(prompt);
            double confidence = 0.5;
            List<String> unclearAreas = new ArrayList<>();
            return AmbiguityScore.builder()
                    .confidence(confidence)
                    .unclearAreas(unclearAreas)
                    .build();
        } catch (Exception e) {
            log.warn("Ambiguity analysis failed, defaulting to low confidence", e);
            return AmbiguityScore.builder()
                    .confidence(0.3)
                    .unclearAreas(List.of("General request clarity"))
                    .build();
        }
    }

    private List<String> generateQuestions(UserRequest request, AmbiguityScore score) {
        String languageInstruction = request.getLanguagePreference() == com.supremeai.dto.LanguagePreference.BENGALI
                ? "Generate questions in Bengali (Bengali script)"
                : "Generate questions in English";
        String prompt = String.format("""
            User wants to: %s
            Ambiguity areas: %s
            %s
            Generate 2-3 specific, actionable clarifying questions. Return as JSON array.
            """, request.getDescription(), score.getUnclearAreas(), languageInstruction);

        try {
            String response = geminiClient.generateQuestions(prompt);
            // Parse JSON array response to List<String>
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(response, 
                mapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            log.error("Failed to generate questions", e);
            return List.of("Could you provide more details about your request?");
        }
    }

    private String inferApproach(UserRequest request) {
        try {
            String prompt = String.format("Infer the best approach to fulfill this request: %s. Return 1-2 sentence summary.", request.getDescription());
            return openAIClient.generate(prompt);
        } catch (Exception e) {
            log.warn("Approach inference failed", e);
            return "General approach will be determined after clarification.";
        }
    }

    private String processRequest(UserRequest request) {
        try {
            String prompt = String.format("Process this clear request: %s. Return direct response.", request.getDescription());
            return openAIClient.generate(prompt);
        } catch (Exception e) {
            log.error("Failed to process request", e);
            return "Request processed, but response generation failed.";
        }
    }
}
