package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.ServerTimestamp;

import java.time.LocalDateTime;

import com.google.cloud.spring.data.firestore.Document;

/**
 * Simulator deployment record stored in Firestore.
 * Collection: "simulator_deployments"
 */
@Document(collectionName = "simulator_deployments")
public class SimulatorDeploymentRecord {

    @DocumentId
    private String appId;
    
    private String deviceType;
    private String previewUrl;
    private String status; // NOT_DEPLOYED, DEPLOYING, RUNNING, STOPPED, ERROR
    
    @ServerTimestamp
    private LocalDateTime deployedAt;

    public SimulatorDeploymentRecord() {}

    public SimulatorDeploymentRecord(String appId, String deviceType, String previewUrl, String status) {
        this.appId = appId;
        this.deviceType = deviceType;
        this.previewUrl = previewUrl;
        this.status = status;
        this.deployedAt = LocalDateTime.now();
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getDeployedAt() {
        return deployedAt;
    }

    public void setDeployedAt(LocalDateTime deployedAt) {
        this.deployedAt = deployedAt;
    }
}
