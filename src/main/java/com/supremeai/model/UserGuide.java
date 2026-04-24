package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Video tutorial/guide model for user onboarding.
 * Supports multiple languages (Bangla, English) for titles and descriptions.
 */
@Document(collectionName = "user_guides")
public class UserGuide {

    @DocumentId
    private String id;

    // Title in different languages (e.g., {"en": "Getting Started", "bn": "শুরু করা"})
    private Map<String, String> title;

    // Description in different languages
    private Map<String, String> description;

    // Video URLs by language (e.g., YouTube embedded URLs or hosted video URLs)
    private Map<String, String> videoUrl;

    // Thumbnail image URL
    private String thumbnailUrl;

    // Order for display sorting (lower numbers appear first)
    private Integer order = 0;

    // Category for grouping (e.g., "basics", "advanced", "security")
    private String category = "general";

    // Duration in seconds
    private Integer durationSeconds;

    // Whether this guide is published/visible to users
    private Boolean isPublished = false;

    // Tags for search/filtering
    private java.util.List<String> tags;

    // Creation timestamp
    private LocalDateTime createdAt;

    // Last update timestamp
    private LocalDateTime updatedAt;

    // Constructors
    public UserGuide() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public UserGuide(String id, Map<String, String> title, Map<String, String> description) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, String> getTitle() {
        return title;
    }

    public void setTitle(Map<String, String> title) {
        this.title = title;
    }

    public Map<String, String> getDescription() {
        return description;
    }

    public void setDescription(Map<String, String> description) {
        this.description = description;
    }

    public Map<String, String> getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(Map<String, String> videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Boolean getIsPublished() {
        return isPublished;
    }

    public void setIsPublished(Boolean isPublished) {
        this.isPublished = isPublished;
    }

    public java.util.List<String> getTags() {
        return tags;
    }

    public void setTags(java.util.List<String> tags) {
        this.tags = tags;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Helper methods
    public String getTitle(String lang) {
        if (title != null && title.containsKey(lang)) {
            return title.get(lang);
        }
        // Fallback to English or first available
        if (title != null && title.containsKey("en")) {
            return title.get("en");
        }
        if (title != null && !title.isEmpty()) {
            return title.values().iterator().next();
        }
        return "";
    }

    public String getDescription(String lang) {
        if (description != null && description.containsKey(lang)) {
            return description.get(lang);
        }
        if (description != null && description.containsKey("en")) {
            return description.get("en");
        }
        if (description != null && !description.isEmpty()) {
            return description.values().iterator().next();
        }
        return "";
    }

    public String getVideoUrl(String lang) {
        if (videoUrl != null && videoUrl.containsKey(lang)) {
            return videoUrl.get(lang);
        }
        if (videoUrl != null && videoUrl.containsKey("en")) {
            return videoUrl.get("en");
        }
        if (videoUrl != null && !videoUrl.isEmpty()) {
            return videoUrl.values().iterator().next();
        }
        return "";
    }
}
