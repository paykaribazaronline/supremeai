package org.example.model;

import java.util.*;

/**
 * Represents a VPN connection configuration.
 *
 * Note: "status" tracks the configured/intended state in SupremeAI's records.
 * Actual OS-level tunnel management depends on the host network configuration.
 */
public class VPNConnection {
    private String id;
    private String name;
    private String protocol; // "OpenVPN", "WireGuard", "L2TP/IPSec", "PPTP"
    private String server;
    private int port;
    private String status; // "connected", "disconnected"
    private String encryption; // "AES-256", "AES-128", "ChaCha20"
    private boolean autoConnect;
    /** Epoch milliseconds of the last time Connect was clicked; 0 = never. */
    private long lastConnectedAt = 0;
    private String config; // Base64 encoded config file

    public VPNConnection() {
        this.id = UUID.randomUUID().toString();
        this.status = "disconnected";
        this.autoConnect = false;
    }

    public VPNConnection(String name, String protocol, String server, int port) {
        this();
        this.name = name;
        this.protocol = protocol;
        this.server = server;
        this.port = port;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    public String getServer() { return server; }
    public void setServer(String server) { this.server = server; }

    /** Alias used by the admin dashboard frontend. */
    public String getServerHost() { return server; }
    public void setServerHost(String serverHost) { this.server = serverHost; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    /** Alias used by the admin dashboard frontend. */
    public int getServerPort() { return port; }
    public void setServerPort(int serverPort) { this.port = serverPort; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getEncryption() { return encryption; }
    public void setEncryption(String encryption) { this.encryption = encryption; }

    public boolean isAutoConnect() { return autoConnect; }
    public void setAutoConnect(boolean autoConnect) { this.autoConnect = autoConnect; }

    public long getLastConnectedAt() { return lastConnectedAt; }
    public void setLastConnectedAt(long lastConnectedAt) { this.lastConnectedAt = lastConnectedAt; }

    public String getConfig() { return config; }
    public void setConfig(String config) { this.config = config; }
}
