package com.supremeai.model.browser;

import com.google.cloud.spring.data.firestore.Document;
import java.time.LocalDateTime;

@Document(collectionName = "browser_credentials")
public class StoredCredential {
    private String id;
    private String userId;
    private String website;
    private String username;
    private String password; // Should be encrypted in a real app
    private String selectorUsername;
    private String selectorPassword;
    private String selectorSubmit;
    private String token;
    private LocalDateTime createdAt;

    public StoredCredential() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getSelectorUsername() { return selectorUsername; }
    public void setSelectorUsername(String selectorUsername) { this.selectorUsername = selectorUsername; }
    public String getSelectorPassword() { return selectorPassword; }
    public void setSelectorPassword(String selectorPassword) { this.selectorPassword = selectorPassword; }
    public String getSelectorSubmit() { return selectorSubmit; }
    public void setSelectorSubmit(String selectorSubmit) { this.selectorSubmit = selectorSubmit; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
