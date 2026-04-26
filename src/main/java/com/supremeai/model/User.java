package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.Exclude;
import com.google.cloud.spring.data.firestore.Document;
import com.google.cloud.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Instant;

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

    // Use Object to seamlessly handle legacy Map (from LocalDateTime), String, or Timestamp
    private Object createdAt;

    private Object updatedAt;

    private Object lastUsedAt;

    private Object lastLoginAt;

    private Boolean isActive = true;

    // Constructors
    public User() {}

    public User(String firebaseUid, String email, String displayName) {
        this.firebaseUid = firebaseUid;
        this.email = email;
        this.displayName = displayName;
        this.createdAt = java.time.LocalDateTime.now().toString();
        this.updatedAt = java.time.LocalDateTime.now().toString();
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

    public String getCreatedAt() { return convertDateObjToString(createdAt); }
    public void setCreatedAt(Object createdAt) { this.createdAt = convertToIsoString(createdAt); }

    public String getUpdatedAt() { return convertDateObjToString(updatedAt); }
    public void setUpdatedAt(Object updatedAt) { this.updatedAt = convertToIsoString(updatedAt); }

    public String getLastUsedAt() { return convertDateObjToString(lastUsedAt); }
    public void setLastUsedAt(Object lastUsedAt) { this.lastUsedAt = convertToIsoString(lastUsedAt); }

    public String getLastLoginAt() { return convertDateObjToString(lastLoginAt); }
    public void setLastLoginAt(Object lastLoginAt) { this.lastLoginAt = convertToIsoString(lastLoginAt); }

    /**
     * Converts any date representation to ISO-8601 string for consistent storage.
     * Supports: String (already ISO), LocalDateTime, Firestore Timestamp, Map (legacy Firestore format).
     */
    private String convertToIsoString(Object obj) {
        if (obj == null) return null;
        if (obj instanceof String) return (String) obj;
        if (obj instanceof LocalDateTime) return ((LocalDateTime) obj).toString();
        if (obj instanceof Timestamp) return ((Timestamp) obj).toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().toString();
        if (obj instanceof java.util.Map) {
            // Handle Firestore Map representation of Timestamp: {seconds: X, nanoseconds: Y}
            try {
                java.util.Map<?, ?> map = (java.util.Map<?, ?>) obj;
                Object seconds = map.get("seconds");
                Object nanos = map.get("nanoseconds");
                if (seconds instanceof Long && nanos instanceof Integer) {
                    return LocalDateTime.ofInstant(
                        Instant.ofEpochSecond((Long) seconds, (Integer) nanos),
                        ZoneId.systemDefault()
                    ).toString();
                }
            } catch (Exception e) {
                // Fall through to null return
            }
            return null; // Cannot convert this Map format
        }
        return null; // Unsupported type
    }

    /**
     * Getter conversion - ensures we always return ISO string or null.
     * Since setters normalize to ISO strings, this mostly handles legacy loaded data.
     */
    private String convertDateObjToString(Object obj) {
        if (obj == null) return null;
        if (obj instanceof String) return (String) obj;
        // Legacy Map or Timestamp - convert on the fly
        return convertToIsoString(obj);
    }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    // Helper methods (Using non-bean names to avoid Firestore conflicts)
    @Exclude
    public boolean isSystemAdmin() {
        return getTier() == UserTier.ADMIN;
    }

    @Exclude
    public Long fetchMonthlyQuota() {
        return getTier().getDefaultMonthlyQuota();
    }

    public void resetMonthlyUsage() {
        this.currentUsage = 0L;
        this.updatedAt = java.time.LocalDateTime.now().toString();
    }

    @Exclude
    public boolean checkQuotaRemaining() {
        if (getTier() == UserTier.ADMIN) return true;
        return this.currentUsage < fetchMonthlyQuota();
    }
}