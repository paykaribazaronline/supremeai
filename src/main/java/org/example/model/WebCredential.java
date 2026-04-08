package org.example.model;

import java.util.UUID;

/**
 * Stores a saved login credential for a web site.
 * Used by the Browser Automation section to auto-fill credentials when browsing.
 *
 * Passwords are stored in the local JSON store (admin-only, never sent to any AI provider).
 */
public class WebCredential {

    private String id = UUID.randomUUID().toString();

    /** Human-readable label, e.g. "GitHub", "Firebase Console" */
    private String siteName;

    /** Base URL of the site, e.g. "https://github.com" */
    private String siteUrl;

    /** Login email or username */
    private String username;

    /** Login password (stored admin-side only, never logged or sent to AI) */
    private String password;

    private long createdAt = System.currentTimeMillis();

    // ── Getters & Setters ───────────────────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSiteName() { return siteName; }
    public void setSiteName(String siteName) { this.siteName = siteName; }

    public String getSiteUrl() { return siteUrl; }
    public void setSiteUrl(String siteUrl) { this.siteUrl = siteUrl; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    /** Returns a safe view of this credential with the password masked. */
    public java.util.Map<String, Object> toMaskedMap() {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", id);
        m.put("siteName", siteName);
        m.put("siteUrl", siteUrl);
        m.put("username", username);
        m.put("hasPassword", password != null && !password.isBlank());
        m.put("createdAt", createdAt);
        return m;
    }
}
