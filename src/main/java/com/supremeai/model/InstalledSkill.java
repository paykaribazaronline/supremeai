package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import java.time.LocalDateTime;

@Document(collectionName = "installed_skills")
public class InstalledSkill {

    @DocumentId
    private String id;
    private String name;
    private String category;
    private String status;
    private String priority;
    private boolean installStatus;
    private String secretRef;
    private LocalDateTime createdAt;

    public InstalledSkill() {}

    public InstalledSkill(String id, String name, String category, String status, String priority, boolean installStatus, String secretRef, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.status = status;
        this.priority = priority;
        this.installStatus = installStatus;
        this.secretRef = secretRef;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public boolean isInstallStatus() { return installStatus; }
    public void setInstallStatus(boolean installStatus) { this.installStatus = installStatus; }

    public String getSecretRef() { return secretRef; }
    public void setSecretRef(String secretRef) { this.secretRef = secretRef; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
