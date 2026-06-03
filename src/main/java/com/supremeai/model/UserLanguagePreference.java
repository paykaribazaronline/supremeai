package com.supremeai.model;

import com.google.cloud.spring.data.firestore.Document;
import java.time.LocalDateTime;

@Document(collectionName = "user_preferences")
public class UserLanguagePreference {
    private String id;
    private String userId;
    private String languageCode; // ISO 639-1 কোড, যেমন 'en', 'bn'
    private String languageName; // ভাষার নাম, যেমন 'English', 'Bengali'
    
    // New Preference Fields
    private Boolean darkMode = true;
    private Boolean notificationsEnabled = true;
    private Boolean focusMode = false;
    private String chatFont = "font-mono";
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // কনস্ট্রাক্টর, গেটার এবং সেটার
    public UserLanguagePreference() {}

    public UserLanguagePreference(String userId, String languageCode, String languageName) {
        this.userId = userId;
        this.languageCode = languageCode;
        this.languageName = languageName;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // গেটার এবং সেটার মেথড
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getLanguageCode() { return languageCode; }
    public void setLanguageCode(String languageCode) { this.languageCode = languageCode; }
    public String getLanguageName() { return languageName; }
    public void setLanguageName(String languageName) { this.languageName = languageName; }
    
    public Boolean getDarkMode() { return darkMode; }
    public void setDarkMode(Boolean darkMode) { this.darkMode = darkMode; }
    
    public Boolean getNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(Boolean notificationsEnabled) { this.notificationsEnabled = notificationsEnabled; }
    
    public Boolean getFocusMode() { return focusMode; }
    public void setFocusMode(Boolean focusMode) { this.focusMode = focusMode; }
    
    public String getChatFont() { return chatFont; }
    public void setChatFont(String chatFont) { this.chatFont = chatFont; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // আপডেট টাইমস্ট্যাম্প মেথড
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}