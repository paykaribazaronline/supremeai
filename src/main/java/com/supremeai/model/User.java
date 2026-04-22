package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import java.time.LocalDateTime;

@Document(collectionName = "users")
public class User {

    @DocumentId
    private String firebaseUid;

    private String email;

    private String displayName;

    private UserTier tier = UserTier.FREE;

    // Legacy Firestore fields (used by some documents that store isAdmin/role instead of tier)
    private Boolean isAdmin;
    private String role;

    private Long currentUsage = 0L;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime lastUsedAt;

    private LocalDateTime lastLoginAt;

    private Boolean isActive = true;

    // Constructors
    public User() {}

    public User(String firebaseUid, String email, String displayName) {
        this.firebaseUid = firebaseUid;
        this.email = email;
        this.displayName = displayName;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getFirebaseUid() { return firebaseUid; }
    public void setFirebaseUid(String firebaseUid) { this.firebaseUid = firebaseUid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public UserTier getTier() {
        // If tier is explicitly set, use it; otherwise derive from legacy Firestore fields
        if (tier != null && tier != UserTier.FREE) return tier;
        if (Boolean.TRUE.equals(isAdmin) || "admin".equalsIgnoreCase(role)) return UserTier.ADMIN;
        return tier != null ? tier : UserTier.FREE;
    }
    public void setTier(UserTier tier) { this.tier = tier; }

    public Boolean getIsAdmin() { return isAdmin; }
    public void setIsAdmin(Boolean isAdmin) { this.isAdmin = isAdmin; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Long getCurrentUsage() { return currentUsage; }
    public void setCurrentUsage(Long currentUsage) { this.currentUsage = currentUsage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    // Helper methods
    public boolean isAdmin() {
        return getTier() == UserTier.ADMIN;
    }

    public Long getMonthlyQuota() {
        return getTier().getDefaultMonthlyQuota();
    }

    public void resetMonthlyUsage() {
        this.currentUsage = 0L;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean hasQuotaRemaining() {
        if (getTier() == UserTier.ADMIN) return true;
        return this.currentUsage < getMonthlyQuota();
    }
}