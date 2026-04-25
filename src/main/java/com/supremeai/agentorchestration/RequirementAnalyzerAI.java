package com.supremeai.agentorchestration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RequirementAnalyzerAI {

    private static final Logger log = LoggerFactory.getLogger(RequirementAnalyzerAI.class);

    @Autowired
    private AIProviderFactory providerFactory;

    /**
     * Analyzes user requirements and generates a list of structured clarifying questions.
     */
    public List<Question> analyze(String requirement) {
        try {
            AIProvider provider = providerFactory.getProvider("groq");
            String prompt = "Given the following user requirement for a software project: \"" + requirement + "\"\n" +
                    "Generate a list of 5-7 clarifying questions to better understand the technical needs.\n" +
                    "Format each question as a JSON object with 'key', 'text', and 'priority' (CRITICAL, HIGH, MEDIUM, LOW).\n" +
                    "Return only a JSON array of these objects.";
            
            String response = provider.generate(prompt).block();
            return parseQuestions(response, requirement);
        } catch (Exception e) {
            log.warn("AI analysis failed, falling back to default questions: {}", e.getMessage());
            return getFallbackQuestions();
        }
    }

    private List<Question> parseQuestions(String response, String requirement) {
        try {
            // Find the JSON array part of the response
            int startIndex = response.indexOf("[");
            int endIndex = response.lastIndexOf("]");
            if (startIndex == -1 || endIndex == -1 || endIndex <= startIndex) {
                return getFallbackQuestions();
            }
            String jsonArray = response.substring(startIndex, endIndex + 1);

            ObjectMapper mapper = new ObjectMapper();
            List<Question> questions = mapper.readValue(jsonArray, new TypeReference<List<Question>>() {});
            
            if (questions == null || questions.isEmpty()) {
                return getFallbackQuestions();
            }
            return questions;
        } catch (Exception e) {
            log.error("Failed to parse AI questions: {}", e.getMessage());
            return getFallbackQuestions();
        }
    }

    private List<Question> getFallbackQuestions() {
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
     * Legacy support for simple string list questions.
     */
    public List<String> generateClarifyingQuestions(String userRequirement) {
        List<Question> questions = analyze(userRequirement);
        List<String> result = new ArrayList<>();
        for (Question q : questions) {
            result.add(q.getText());
        }
        return result;
    }
}
