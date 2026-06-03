
package com.supremeai.dto;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for User.
 * Used to avoid exposing sensitive user data and optimize query performance.
 */
public class UserDTO {
    private String id;
    private String email;
    private String displayName;
    private String tier;
    private Long monthlyQuota;
    private Long currentUsage;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private Boolean isActive;

    // Constructors
    public UserDTO() {}

    public UserDTO(String id, String email, String displayName, String tier, Long monthlyQuota, 
                   Long currentUsage, LocalDateTime createdAt, LocalDateTime lastLoginAt, Boolean isActive) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.tier = tier;
        this.monthlyQuota = monthlyQuota;
        this.currentUsage = currentUsage;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
        this.isActive = isActive;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }

    public Long getMonthlyQuota() { return monthlyQuota; }
    public void setMonthlyQuota(Long monthlyQuota) { this.monthlyQuota = monthlyQuota; }

    public Long getCurrentUsage() { return currentUsage; }
    public void setCurrentUsage(Long currentUsage) { this.currentUsage = currentUsage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
