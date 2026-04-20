package com.supremeai.automation.farm;

public class SyntheticAccount {
    private String email;
    private String password;
    private String associatedApiKey;
    private String vpnRegionRequired; // e.g., "US", "UK", "SG"
    private boolean isBanned;

    public SyntheticAccount(String email, String password, String vpnRegionRequired) {
        this.email = email;
        this.password = password;
        this.vpnRegionRequired = vpnRegionRequired;
        this.isBanned = false;
    }

    public void setAssociatedApiKey(String key) { this.associatedApiKey = key; }
    public void markAsBanned() { this.isBanned = true; }

    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getAssociatedApiKey() { return associatedApiKey; }
    public String getVpnRegionRequired() { return vpnRegionRequired; }
    public boolean isBanned() { return isBanned; }
}