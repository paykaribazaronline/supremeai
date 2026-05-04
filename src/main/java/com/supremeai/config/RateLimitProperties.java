package com.supremeai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for rate limiting.
 * Configurable via application.yml under 'rate-limit' prefix.
 */
@Configuration
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

    /**
     * Default requests per minute for authenticated users.
     */
    private int authenticatedRequestsPerMinute = 100;

    /**
     * Default requests per minute for anonymous/unauthenticated users.
     */
    private int anonymousRequestsPerMinute = 10;

    /**
     * Default requests per minute for admin users.
     */
    private int adminRequestsPerMinute = 1000;

    /**
     * Default requests per minute for AI provider endpoints.
     */
    private int aiProviderRequestsPerMinute = 50;

    /**
     * Time window in seconds for rate limiting.
     */
    private int windowSeconds = 60;

    /**
     * Enable or disable rate limiting.
     */
    private boolean enabled = true;

    /**
     * Use distributed Redis-based rate limiting (true) or in-memory (false).
     */
    private boolean distributed = true;

    public int getAuthenticatedRequestsPerMinute() {
        return authenticatedRequestsPerMinute;
    }

    public void setAuthenticatedRequestsPerMinute(int authenticatedRequestsPerMinute) {
        this.authenticatedRequestsPerMinute = authenticatedRequestsPerMinute;
    }

    public int getAnonymousRequestsPerMinute() {
        return anonymousRequestsPerMinute;
    }

    public void setAnonymousRequestsPerMinute(int anonymousRequestsPerMinute) {
        this.anonymousRequestsPerMinute = anonymousRequestsPerMinute;
    }

    public int getAdminRequestsPerMinute() {
        return adminRequestsPerMinute;
    }

    public void setAdminRequestsPerMinute(int adminRequestsPerMinute) {
        this.adminRequestsPerMinute = adminRequestsPerMinute;
    }

    public int getAiProviderRequestsPerMinute() {
        return aiProviderRequestsPerMinute;
    }

    public void setAiProviderRequestsPerMinute(int aiProviderRequestsPerMinute) {
        this.aiProviderRequestsPerMinute = aiProviderRequestsPerMinute;
    }

    public int getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(int windowSeconds) {
        this.windowSeconds = windowSeconds;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isDistributed() {
        return distributed;
    }

    public void setDistributed(boolean distributed) {
        this.distributed = distributed;
    }
}
