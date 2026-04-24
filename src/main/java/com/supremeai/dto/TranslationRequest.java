package com.supremeai.dto;

public class TranslationRequest {
    private String text;
    private String fromLanguage;
    private String toLanguage;

    public TranslationRequest() {}

    public TranslationRequest(String text, String fromLanguage, String toLanguage) {
        this.text = text;
        this.fromLanguage = fromLanguage;
        this.toLanguage = toLanguage;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getFromLanguage() {
        return fromLanguage;
    }

    public void setFromLanguage(String fromLanguage) {
        this.fromLanguage = fromLanguage;
    }

    public String getToLanguage() {
        return toLanguage;
    }

    public void setToLanguage(String toLanguage) {
        this.toLanguage = toLanguage;
    }
}