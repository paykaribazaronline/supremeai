package com.supremeai.model;

/**
 * Defines the access levels for SupremeAI users.
 * Quotas for these tiers are managed dynamically via SystemConfig.
 */
public enum UserTier {
    GUEST("Anonymous guest access with minimal quota"),
    FREE("Registered free tier"),
    BASIC("Basic paid tier"),
    PRO("Professional tier with high limits"),
    ENTERPRISE("Enterprise tier with maximum limits"),
    ADMIN("Administrator with unlimited access");

    private final String description;

    UserTier(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isPremium() {
        return this == BASIC || this == PRO || this == ENTERPRISE;
    }

    public boolean isUnlimited() {
        return this == ADMIN;
    }

    /**
     * Default monthly quota for each tier (in API calls or tokens)
     * Note: Actual quotas may be overridden by SystemConfig.
     */
    public long getDefaultMonthlyQuota() {
        return switch (this) {
            case GUEST -> 10L;
            case FREE -> 100L;
            case BASIC -> 1_000L;
            case PRO -> 5_000L;
            case ENTERPRISE -> 50_000L;
            case ADMIN -> Long.MAX_VALUE; // effectively unlimited
        };
    }

    public boolean hasUnlimitedQuota() {
        return isUnlimited();
    }
}
