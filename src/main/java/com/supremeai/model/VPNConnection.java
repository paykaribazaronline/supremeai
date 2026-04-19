package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import java.time.LocalDateTime;

@Document(collectionName = "vpn_connections")
public class VPNConnection {
    @DocumentId
    private String id;
    private String name;
    private String region;
    private String status;
    private String ipAddress;
    private LocalDateTime connectedAt;
    private String latency;

    public VPNConnection() {}

    public VPNConnection(String id, String name, String region, String status) {
        this.id = id;
        this.name = name;
        this.region = region;
        this.status = status;
        this.connectedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public LocalDateTime getConnectedAt() { return connectedAt; }
    public void setConnectedAt(LocalDateTime connectedAt) { this.connectedAt = connectedAt; }
    public String getLatency() { return latency; }
    public void setLatency(String latency) { this.latency = latency; }
}
