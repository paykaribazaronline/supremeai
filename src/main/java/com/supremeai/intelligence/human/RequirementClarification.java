package com.supremeai.intelligence.human;

import java.util.List;
import java.util.ArrayList;

public class RequirementClarification {
    private String clarifyingQuestion;
    private List<ClarificationOption> options;

    public RequirementClarification(String clarifyingQuestion) {
        this.clarifyingQuestion = clarifyingQuestion;
        this.options = new ArrayList<>();
    }

    public void addOption(String optionText, String hint) {
        this.options.add(new ClarificationOption(optionText, hint));
    }

    public String getClarifyingQuestion() { return clarifyingQuestion; }
    public List<ClarificationOption> getOptions() { return options; }

    public String buildChatResponse() {
        StringBuilder sb = new StringBuilder();
        sb.append(clarifyingQuestion).append("\n\n");
        for (int i = 0; i < options.size(); i++) {
            sb.append((i + 1)).append(". ").append(options.get(i).getOptionText()).append("\n");
            sb.append("   [Hint: ").append(options.get(i).getHint()).append("]\n\n");
        }
        sb.append("Please reply with the number (e.g., '1') or tell me in your own words.");
        return sb.toString();
    }
}