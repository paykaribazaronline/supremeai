package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.ServerTimestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Generated application stored in Firestore.
 * Collection: "generated_apps"
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneratedApp {

    @DocumentId
    private String appId;

    private String userId;

    private String name;

    private String description;

    private String platform;  // WEB, IOS, ANDROID, DESKTOP

    private String language;  // SwiftUI, Kotlin, React, Tauri

    private String techStack; // Detailed stack info

    private String htmlContent;  // For web apps: complete HTML

    private Map<String, String> sourceFiles;  // filename → content for multi-file projects

    private String version;

    private String status;  // GENERATED, DEPLOYED, ERROR

    private String errorMessage;

    private byte[] screenshot;

    private String requestId;

    @ServerTimestamp
    private LocalDateTime createdAt;

    @ServerTimestamp
    private LocalDateTime updatedAt;

    // Backward compatibility constructor
    public GeneratedApp(String appId, String userId, String platform, String language) {
        this.appId = appId;
        this.userId = userId;
        this.platform = platform;
        this.language = language;
        this.version = "1.0.0";
        this.status = "GENERATED";
        this.createdAt = LocalDateTime.now();
    }

    public String getId() {
        return appId;
    }
}


