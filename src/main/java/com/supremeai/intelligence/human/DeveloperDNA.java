package com.supremeai.intelligence.human;

public class DeveloperDNA {
    
    // Communication Traits
    private int explanationScore = 50; // 0 = Just give me code, 100 = Explain everything like I'm 5
    
    // Code Formatting Traits
    private int indentStyle = 4; // Default 4 spaces
    private boolean usesCamelCase = true;
    
    public void increaseExplanationPreference() {
        if (explanationScore < 100) explanationScore += 10;
    }

    public void increaseDirectCodePreference() {
        if (explanationScore > 0) explanationScore -= 10;
    }

    public void setIndentStyle(int spaces) {
        this.indentStyle = spaces;
    }

    public void setUsesCamelCase(boolean camelCase) {
        this.usesCamelCase = camelCase;
    }

    /**
     * System uses this DNA to inject "System Instructions" into the LLM prompt.
     * So the AI feels like it "knows" the developer.
     */
    public String generatePromptInjection() {
        StringBuilder injection = new StringBuilder();
        injection.append("SYSTEM INSTRUCTION FOR THIS SPECIFIC USER:\n");
        
        if (explanationScore > 70) {
            injection.append("- The user prefers detailed explanations. Break down the logic step-by-step.\n");
        } else if (explanationScore < 30) {
            injection.append("- The user is an expert who hates chatting. Output ONLY the raw code. NO explanations.\n");
        } else {
            injection.append("- Provide the code with brief, concise comments.\n");
        }
        
        injection.append("- Format code using ").append(indentStyle).append(" spaces for indentation.\n");
        injection.append("- Use ").append(usesCamelCase ? "camelCase" : "snake_case").append(" for variables.\n");
        
        return injection.toString();
    }

    public String getSummary() {
        return String.format("ExpScore:%d, Indent:%d, CamelCase:%b", explanationScore, indentStyle, usesCamelCase);
    }
}