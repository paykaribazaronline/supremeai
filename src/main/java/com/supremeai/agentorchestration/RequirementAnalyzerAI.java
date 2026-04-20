package com.supremeai.agentorchestration;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RequirementAnalyzerAI {

    public List<String> generateClarifyingQuestions(String userRequirement) {
        List<String> questions = new ArrayList<>();
        
        questions.add("What is the primary purpose or goal of this application?");
        questions.add("Who are the intended users?");
        questions.add("What platforms should this application support? (Web, Mobile, Desktop, API)");
        questions.add("Do you need user authentication and authorization?");
        questions.add("What data will be stored?");
        questions.add("Are there any specific performance or scalability requirements?");
        questions.add("Do you need integration with any external services or APIs?");
        questions.add("What is your expected timeline for delivery?");
        questions.add("Are there any security or compliance requirements?");
        questions.add("Do you have any design preferences or branding requirements?");
        
        return questions;
    }
}
