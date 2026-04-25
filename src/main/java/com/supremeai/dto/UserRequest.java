package com.supremeai.dto;

public class UserRequest {
    private String description;
    private LanguagePreference languagePreference;
    private String userId;

    // Getters
    public String getDescription() { return description; }
    public LanguagePreference getLanguagePreference() { return languagePreference; }
    public String getUserId() { return userId; }

    // Setters
    public void setDescription(String description) { this.description = description; }
    public void setLanguagePreference(LanguagePreference languagePreference) { this.languagePreference = languagePreference; }
    public void setUserId(String userId) { this.userId = userId; }

    // Builder pattern
    public static UserRequest builder() { return new UserRequest(); }

    public UserRequest description(String description) { this.description = description; return this; }
    public UserRequest languagePreference(LanguagePreference languagePreference) { this.languagePreference = languagePreference; return this; }
    public UserRequest userId(String userId) { this.userId = userId; return this; }
    public UserRequest build() { return this; }
}
