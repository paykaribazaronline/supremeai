package com.supremeai.intelligence.human;

public class ClarificationOption {
    private String optionText;
    private String hint;

    public ClarificationOption(String optionText, String hint) {
        this.optionText = optionText;
        this.hint = hint;
    }

    public String getOptionText() { return optionText; }
    public String getHint() { return hint; }

    @Override
    public String toString() {
        return "- " + optionText + "\n  (Hint: " + hint + ")";
    }
}