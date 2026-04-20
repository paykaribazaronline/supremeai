package com.supremeai.intelligence.human;

import org.springframework.stereotype.Service;

/**
 * Solves the "User doesn't know how to explain what they want" problem.
 * Analyzes the prompt for vagueness and asks targeted multiple-choice questions with hints.
 */
@Service
public class RequirementAnalyzer {

    /**
     * Analyzes the user's prompt. 
     * If the prompt is too vague (e.g., "make a login page" or "fix the bug"), 
     * it generates a Clarification object instead of guessing and writing wrong code.
     */
    public RequirementClarification analyzeRequirement(String userPrompt) {
        String lowerPrompt = userPrompt.toLowerCase();

        // SCENARIO 1: Extremely vague requirement about UI/Auth
        if (lowerPrompt.contains("login") && lowerPrompt.length() < 30) {
            RequirementClarification clarification = new RequirementClarification(
                "I'd love to build the login feature for you, but I need to know a bit more about your tech stack and security needs. Which of these sounds closest to what you want?"
            );
            
            clarification.addOption(
                "JWT (JSON Web Token) API Login", 
                "Best for mobile apps or single-page apps (React/Angular). Stateless and scalable."
            );
            clarification.addOption(
                "Session-based Login (Cookies)", 
                "Best for traditional web apps (Thymeleaf/JSP). Easier to revoke access."
            );
            clarification.addOption(
                "OAuth2 / Social Login", 
                "Users click 'Login with Google/Facebook'. Much higher conversion rate."
            );
            
            return clarification;
        }

        // SCENARIO 2: Extremely vague bug report
        if ((lowerPrompt.contains("it doesn't work") || lowerPrompt.contains("fix bug") || lowerPrompt.contains("error")) 
             && lowerPrompt.length() < 50 && !lowerPrompt.contains("exception")) {
            
            RequirementClarification clarification = new RequirementClarification(
                "I can fix the bug, but 'it doesn't work' is a bit too broad! Could you give me a tiny hint about what exactly is failing?"
            );
            
            clarification.addOption(
                "The app crashes / throws an Exception", 
                "Look at your terminal/console and paste the red error text here."
            );
            clarification.addOption(
                "The data is saving wrong or not saving", 
                "I'll check the Database Repository and Service logic."
            );
            clarification.addOption(
                "The UI looks broken or button doesn't click", 
                "I'll check the Frontend framework and API connections."
            );
            
            return clarification;
        }

        // SCENARIO 3: Generic "build a website"
        if (lowerPrompt.contains("build") && lowerPrompt.contains("app") && lowerPrompt.length() < 40) {
             RequirementClarification clarification = new RequirementClarification(
                "Building an app is exciting! To set up the perfect architecture, what is the main purpose of the app?"
            );
            
            clarification.addOption(
                "E-commerce / Shopping", 
                "I'll set up Product catalogs, Carts, and Payment gateway integrations."
            );
            clarification.addOption(
                "Dashboard / Admin Panel", 
                "I'll focus on Data Tables, Charts, and Role-based Access Control (RBAC)."
            );
            clarification.addOption(
                "Social / Chat", 
                "I'll configure WebSockets for real-time messaging and User profiles."
            );
            
            return clarification;
        }

        // If the prompt is long and detailed enough, return null (meaning: clear to proceed to AI)
        return null;
    }
}