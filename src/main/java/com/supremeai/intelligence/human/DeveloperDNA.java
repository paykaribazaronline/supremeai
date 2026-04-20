package com.supremeai.intelligence.human;

public class DeveloperDNA {
    
    // Communication Traits
    private int explanationScore = 50; // 0 = Just give me code, 100 = Explain everything like I'm 5
    
    // Code Formatting Traits
    private int indentStyle = 4; // Default 4 spaces
    private boolean usesCamelCase = true;

    // Local Language and Tone
    private String preferredLanguage = "English"; // Default
    private boolean prefersSimpleLanguage = false; // If true, avoid heavy jargon
    
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

    public void setPreferredLanguage(String language) {
        this.preferredLanguage = language;
    }

    public void setPrefersSimpleLanguage(boolean prefersSimple) {
        this.prefersSimpleLanguage = prefersSimple;
    }
    
    public String getPreferredLanguage() { return preferredLanguage; }
    public boolean isPrefersSimpleLanguage() { return prefersSimpleLanguage; }

    /**
     * System uses this DNA to inject "System Instructions" into the LLM prompt.
     * So the AI feels like it "knows" the developer.
     */
    public String generatePromptInjection() {
        StringBuilder injection = new StringBuilder();
        injection.append("SYSTEM INSTRUCTION FOR THIS SPECIFIC USER:\n");
        
        // 1. Language & Tone Injection (The Most Important Part!)
        if (!"English".equalsIgnoreCase(preferredLanguage)) {
            injection.append("CRITICAL: ALWAYS communicate with this user in ").append(preferredLanguage).append(".\n");
        }
        
        if (prefersSimpleLanguage) {
            injection.append("- Use extremely simple, everyday words. Avoid heavy technical jargon. ");
            injection.append("Explain programming concepts as if you are talking to a non-technical person.\n");
        }

        // 2. Explanation Style
        if (explanationScore > 70) {
            injection.append("- The user prefers detailed explanations. Break down the logic step-by-step.\n");
        } else if (explanationScore < 30) {
            injection.append("- The user is an expert who hates chatting. Output ONLY the raw code. NO explanations.\n");
        }
        
        // 3. Formatting
        injection.append("- Format code using ").append(indentStyle).append(" spaces for indentation.\n");
        injection.append("- Use ").append(usesCamelCase ? "camelCase" : "snake_case").append(" for variables.\n");
        
        return injection.toString();
    }

    public String getSummary() {
        return String.format("Language:%s, SimpleTone:%b, ExpScore:%d", preferredLanguage, prefersSimpleLanguage, explanationScore);
    }
}