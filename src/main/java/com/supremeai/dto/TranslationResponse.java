package com.supremeai.dto;

public class TranslationResponse {
    private String translatedText;
    private boolean success;

    public TranslationResponse() {}

    public TranslationResponse(String translatedText, boolean success) {
        this.translatedText = translatedText;
        this.success = success;
    }

    public String getTranslatedText() {
        return translatedText;
    }

    public void setTranslatedText(String translatedText) {
        this.translatedText = translatedText;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}